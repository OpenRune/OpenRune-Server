package org.rsmod.content.areas.city.desert

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import kotlin.coroutines.Continuation
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.coroutines.startCoroutine
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Assertions.fail
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.junit.jupiter.api.parallel.ResourceLock
import org.rsmod.annotations.InternalApi
import org.rsmod.api.inv.storage.PlayerItemStorage
import org.rsmod.api.invtx.InvTransactionsScript
import org.rsmod.api.npc.interact.AiPlayerInteractions
import org.rsmod.api.player.dialogue.align.TextAlignment
import org.rsmod.api.player.events.interact.NpcEvents
import org.rsmod.api.player.input.ResumePauseButtonInput
import org.rsmod.api.player.interact.NpcInteractions
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.protect.ProtectedAccessContextFactory
import org.rsmod.api.player.protect.clearPendingAction
import org.rsmod.api.registry.obj.ObjRegistry
import org.rsmod.api.registry.zone.ZoneUpdateMap
import org.rsmod.api.repo.obj.ObjRepository
import org.rsmod.api.shops.Shops
import org.rsmod.content.areas.city.desert.bedabin.npcs.BedabinTownsfolk
import org.rsmod.content.areas.city.desert.bedabin.npcs.shops.BedabinVillageBartering
import org.rsmod.content.areas.city.desert.menaphos.npcs.Faaid
import org.rsmod.content.areas.city.desert.nardah.npcs.AliTheCarter
import org.rsmod.content.areas.city.desert.nardah.npcs.NardahTownsfolk
import org.rsmod.content.areas.city.desert.nardah.npcs.shops.NardahGeneralStore
import org.rsmod.content.areas.city.desert.nardah.npcs.shops.NardahHunterShop
import org.rsmod.content.areas.city.desert.nardah.npcs.shops.RoksChocsBox
import org.rsmod.content.areas.city.desert.nardah.npcs.shops.SedduAdventurersStore
import org.rsmod.content.areas.city.desert.pollnivneach.npcs.AliTheCamelMan
import org.rsmod.content.areas.city.desert.pollnivneach.npcs.AliTheDyer
import org.rsmod.content.areas.city.desert.pollnivneach.npcs.KebabSeller
import org.rsmod.content.areas.city.desert.pollnivneach.npcs.PollnivneachGangs
import org.rsmod.content.areas.city.desert.pollnivneach.npcs.PollnivneachTownsfolk
import org.rsmod.content.areas.city.desert.pollnivneach.npcs.StreetUrchin
import org.rsmod.content.areas.city.desert.pollnivneach.npcs.shops.AspAndSnakeBar
import org.rsmod.content.areas.city.desert.pollnivneach.npcs.shops.PollnivneachGeneralStore
import org.rsmod.content.areas.city.desert.sophanem.npcs.SophanemTownsfolk
import org.rsmod.content.areas.city.desert.sophanem.npcs.shops.BladesByUrbi
import org.rsmod.content.areas.city.desert.sophanem.npcs.shops.JamilasCraftStall
import org.rsmod.content.areas.city.desert.sophanem.npcs.shops.NathifasBakeStall
import org.rsmod.content.areas.city.desert.sophanem.npcs.shops.RaetulClothStore
import org.rsmod.content.quest.manager.QuestRequirementMode
import org.rsmod.content.quest.manager.QuestRequirementPolicy
import org.rsmod.content.quest.manager.QuestRequirements
import org.rsmod.coroutine.GameCoroutine
import org.rsmod.events.EventBus
import org.rsmod.game.MapClock
import org.rsmod.game.cheat.CheatCommandMap
import org.rsmod.game.client.Client
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.game.entity.PlayerList
import org.rsmod.game.inv.InvObj
import org.rsmod.game.inv.InvVirtualStorageHolder
import org.rsmod.game.inv.Inventory
import org.rsmod.game.queue.EngineQueueCache
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.ScriptContext

@Execution(ExecutionMode.SAME_THREAD)
@ResourceLock("ServerCacheManager")
class DesertPurchasesInteractionTest {
    private val previousPolicy = QuestRequirements.activePolicy()

    @AfterEach
    fun restorePolicy() {
        QuestRequirements.install(previousPolicy)
    }

    private fun questsNotStarted() {
        QuestRequirements.install(QuestRequirementPolicy(QuestRequirementMode.RespectProgress))
    }

    private fun questsCompleted() {
        QuestRequirements.install(QuestRequirementPolicy(QuestRequirementMode.AssumeCompleted))
    }

    @Test fun `the kebab seller sells a kebab for 3 coins`() {
        val f = Fixture()
        f.player.inv[0] = InvObj(COINS, 10)
        f.talkTo(KEBAB_SELLER)
        f.finish(listOf(1, 2))
        assertEquals(7, f.player.inv.count(COINS))
        assertEquals(1, f.player.inv.count(KEBAB))
    }

    @Test fun `the kebab seller sells a super kebab for 5 coins`() {
        val f = Fixture()
        f.player.inv[0] = InvObj(COINS, 10)
        f.talkTo(KEBAB_SELLER)
        f.finish(listOf(2, 2))
        assertEquals(5, f.player.inv.count(COINS))
        assertEquals(1, f.player.inv.count(SUPER_KEBAB))
    }

    @Test fun `the kebab seller keeps the kebabs from a player short of coins`() {
        val f = Fixture()
        f.player.inv[0] = InvObj(COINS, 4)
        f.talkTo(KEBAB_SELLER)
        f.finish(listOf(2, 1, 1, 2))
        assertEquals(1, f.player.inv.count(COINS))
        assertEquals(1, f.player.inv.count(KEBAB))
        assertEquals(0, f.player.inv.count(SUPER_KEBAB))
    }

    @Test fun `a full pack drops the kebab instead of losing it`() {
        val f = Fixture()
        f.fillInventory()
        f.player.inv[0] = InvObj(COINS, 10)
        f.talkTo(KEBAB_SELLER)
        f.finish(listOf(1, 2))
        assertEquals(7, f.player.inv.count(COINS))
        assertEquals(0, f.player.inv.count(KEBAB))
        assertEquals(1, f.groundCount(KEBAB))
    }

    @Test fun `the kebab seller gives one free bottle of sauce`() {
        val f = Fixture()
        f.talkTo(KEBAB_SELLER)
        f.finish(listOf(3, 1, 3, 2))
        assertEquals(1, f.player.inv.count(SAUCE))
    }

    @Test fun `the kebab seller gives no sauce to a player who has some banked`() {
        val f = Fixture()
        val bank = f.player.invMap.getOrPut("inv.bank")
        bank[0] = InvObj(SAUCE, 1)
        f.talkTo(KEBAB_SELLER)
        f.finish(listOf(3, 2))
        assertEquals(0, f.player.inv.count(SAUCE))
        assertEquals(1, bank.count(SAUCE))
        assertEquals(0, f.groundCount(SAUCE))
    }

    @Test fun `the kebab seller gives no sauce to a full pack`() {
        val f = Fixture()
        f.fillInventory()
        f.talkTo(KEBAB_SELLER)
        f.finish(listOf(3, 2))
        assertEquals(0, f.player.inv.count(SAUCE))
        assertEquals(0, f.groundCount(SAUCE))
    }

    @Test fun `the dyer makes red dye from three redberries and 25 coins`() {
        val f = Fixture()
        f.player.inv[0] = InvObj(COINS, 30)
        f.player.inv[1] = InvObj(REDBERRIES, 1)
        f.player.inv[2] = InvObj(REDBERRIES, 1)
        f.player.inv[3] = InvObj(REDBERRIES, 1)
        f.player.inv[4] = InvObj(REDBERRIES, 1)
        f.talkTo(DYER)
        f.finish(listOf(1, 1, 1))
        assertEquals(5, f.player.inv.count(COINS))
        assertEquals(1, f.player.inv.count(REDBERRIES))
        assertEquals(1, f.player.inv.count(RED_DYE))
    }

    @Test fun `the dyer makes yellow dye from two onions and 25 coins`() {
        val f = Fixture()
        f.player.inv[0] = InvObj(COINS, 25)
        f.player.inv[1] = InvObj(ONION, 1)
        f.player.inv[2] = InvObj(ONION, 1)
        f.talkTo(DYER)
        f.finish(listOf(1, 2, 1))
        assertEquals(0, f.player.inv.count(COINS))
        assertEquals(0, f.player.inv.count(ONION))
        assertEquals(1, f.player.inv.count(YELLOW_DYE))
    }

    @Test fun `the dyer makes blue dye from two woad leaves and 25 coins`() {
        val f = Fixture()
        f.player.inv[0] = InvObj(COINS, 25)
        f.player.inv[1] = InvObj(WOAD_LEAF, 2)
        f.talkTo(DYER)
        f.finish(listOf(1, 3, 1))
        assertEquals(0, f.player.inv.count(COINS))
        assertEquals(0, f.player.inv.count(WOAD_LEAF))
        assertEquals(1, f.player.inv.count(BLUE_DYE))
    }

    @Test fun `the dyer takes nothing from a player short of coins`() {
        val f = Fixture()
        f.player.inv[0] = InvObj(COINS, 24)
        f.player.inv[1] = InvObj(ONION, 1)
        f.player.inv[2] = InvObj(ONION, 1)
        f.talkTo(DYER)
        f.finish(listOf(1, 2, 1))
        assertEquals(24, f.player.inv.count(COINS))
        assertEquals(2, f.player.inv.count(ONION))
        assertEquals(0, f.player.inv.count(YELLOW_DYE))
    }

    @Test fun `the dyer takes nothing from a player short of ingredients`() {
        val f = Fixture()
        f.player.inv[0] = InvObj(COINS, 100)
        f.player.inv[1] = InvObj(WOAD_LEAF, 1)
        f.talkTo(DYER)
        f.finish(listOf(1, 3, 1))
        assertEquals(100, f.player.inv.count(COINS))
        assertEquals(1, f.player.inv.count(WOAD_LEAF))
        assertEquals(0, f.player.inv.count(BLUE_DYE))
    }

    @Test fun `declining the dyer's price costs nothing`() {
        val f = Fixture()
        f.player.inv[0] = InvObj(COINS, 100)
        f.player.inv[1] = InvObj(WOAD_LEAF, 2)
        f.talkTo(DYER)
        f.finish(listOf(1, 3, 3))
        assertEquals(100, f.player.inv.count(COINS))
        assertEquals(2, f.player.inv.count(WOAD_LEAF))
    }

    @Test fun `Ali the Carter sells a waterskin for 1000 coins`() {
        questsNotStarted()
        val f = Fixture()
        f.player.inv[0] = InvObj(COINS, 1500)
        f.talkTo(CARTER)
        f.finish(listOf(2))
        assertEquals(500, f.player.inv.count(COINS))
        assertEquals(1, f.player.inv.count(WATERSKIN))
    }

    @Test fun `Ali the Carter sells nothing to a player short of coins`() {
        questsNotStarted()
        val f = Fixture()
        f.player.inv[0] = InvObj(COINS, 999)
        f.talkTo(CARTER)
        f.finish(listOf(2))
        assertEquals(999, f.player.inv.count(COINS))
        assertEquals(0, f.player.inv.count(WATERSKIN))
    }

    @Test fun `declining Ali the Carter costs nothing`() {
        questsNotStarted()
        val f = Fixture()
        f.player.inv[0] = InvObj(COINS, 1000)
        f.talkTo(CARTER)
        f.finish(listOf(3))
        assertEquals(1000, f.player.inv.count(COINS))
        assertEquals(0, f.player.inv.count(WATERSKIN))
    }

    @Test fun `a full pack drops the waterskin instead of losing it`() {
        questsNotStarted()
        val f = Fixture()
        f.fillInventory()
        f.player.inv[0] = InvObj(COINS, 1500)
        f.talkTo(CARTER)
        f.finish(listOf(2))
        assertEquals(500, f.player.inv.count(COINS))
        assertEquals(0, f.player.inv.count(WATERSKIN))
        assertEquals(1, f.groundCount(WATERSKIN))
    }

    @Test fun `Ali the Carter still sells a waterskin after Spirits of the Elid`() {
        questsCompleted()
        val f = Fixture()
        f.player.inv[0] = InvObj(COINS, 1500)
        f.talkTo(CARTER)
        f.finish(listOf(1))
        assertEquals(500, f.player.inv.count(COINS))
        assertEquals(1, f.player.inv.count(WATERSKIN))
    }

    @Test fun `Ali the Carter sells nothing after the quest to a player short of coins`() {
        questsCompleted()
        val f = Fixture()
        f.player.inv[0] = InvObj(COINS, 999)
        f.talkTo(CARTER)
        f.finish(listOf(1))
        assertEquals(999, f.player.inv.count(COINS))
        assertEquals(0, f.player.inv.count(WATERSKIN))
    }

    @Test fun `the High Priest sells another circlet for 10000 coins`() {
        questsCompleted()
        val f = Fixture()
        f.player.inv[0] = InvObj(COINS, 12_000)
        f.talkTo(HIGH_PRIEST)
        f.finish(listOf(2, 1, 3))
        assertEquals(2_000, f.player.inv.count(COINS))
        assertEquals(1, f.player.inv.count(CIRCLET))
    }

    @Test fun `the High Priest sells no circlet to a player short of coins`() {
        questsCompleted()
        val f = Fixture()
        f.player.inv[0] = InvObj(COINS, 9_999)
        f.talkTo(HIGH_PRIEST)
        f.finish(listOf(2, 1, 3))
        assertEquals(9_999, f.player.inv.count(COINS))
        assertEquals(0, f.player.inv.count(CIRCLET))
    }

    @Test fun `declining the High Priest's circlet costs nothing`() {
        questsCompleted()
        val f = Fixture()
        f.player.inv[0] = InvObj(COINS, 12_000)
        f.talkTo(HIGH_PRIEST)
        f.finish(listOf(2, 2, 3))
        assertEquals(12_000, f.player.inv.count(COINS))
        assertEquals(0, f.player.inv.count(CIRCLET))
    }

    @Test fun `a full pack drops the circlet instead of losing it`() {
        questsCompleted()
        val f = Fixture()
        f.fillInventory()
        f.player.inv[0] = InvObj(COINS, 12_000)
        f.talkTo(HIGH_PRIEST)
        f.finish(listOf(2, 1, 3))
        assertEquals(2_000, f.player.inv.count(COINS))
        assertEquals(0, f.player.inv.count(CIRCLET))
        assertEquals(1, f.groundCount(CIRCLET))
    }

    @Test fun `the High Priest has no circlet before Beneath Cursed Sands`() {
        questsNotStarted()
        val f = Fixture()
        f.player.inv[0] = InvObj(COINS, 12_000)
        f.talkTo(HIGH_PRIEST)
        f.finish(emptyList())
        assertEquals(12_000, f.player.inv.count(COINS))
        assertEquals(0, f.player.inv.count(CIRCLET))
    }

    private class Fixture {
        private val events = EventBus()
        private val client = RecordingClient()
        private val coroutine = GameCoroutine("desert-purchases-test")
        private var result: Result<Unit>? = null
        private val clock = MapClock(100)
        private val objRegistry = ObjRegistry(ZoneUpdateMap())
        private val context =
            ProtectedAccessContextFactory.empty()
                .copy(
                    getEventBus = { events },
                    getAlignment = { TextAlignment() },
                    getNpcInteractions = { NpcInteractions(events) },
                )

        @OptIn(InternalApi::class)
        val player =
            Player().apply {
                this.client = this@Fixture.client
                uuid = 794L
                observerUUID = 794L
                slotId = 1
                assignUid()
                coords = CoordGrid(3360, 2970, 0)
                currentMapClock = 100
                processedMapClock = 100
                inv =
                    Inventory(
                        checkNotNull(ServerCacheManager.getInventory("inv.inv".asRSCM())),
                        arrayOfNulls(28),
                    )
                worn =
                    Inventory(
                        checkNotNull(ServerCacheManager.getInventory("inv.worn".asRSCM())),
                        arrayOfNulls(14),
                    )
                invMap["inv.inv"] = inv
                invMap["inv.worn"] = worn
            }

        init {
            val scripts = ScriptContext(events, CheatCommandMap(), EngineQueueCache())
            val objRepo = ObjRepository(clock, objRegistry)
            val shops = Shops(events)
            with(KebabSeller(objRepo)) { scripts.startup() }
            with(AliTheDyer(objRepo)) { scripts.startup() }
            with(AliTheCarter(objRepo)) { scripts.startup() }
            with(AliTheCamelMan()) { scripts.startup() }
            with(StreetUrchin()) { scripts.startup() }
            with(PollnivneachTownsfolk()) { scripts.startup() }
            with(PollnivneachGangs(AiPlayerInteractions(events, PlayerList()))) {
                scripts.startup()
            }
            with(AspAndSnakeBar(shops)) { scripts.startup() }
            with(PollnivneachGeneralStore(shops)) { scripts.startup() }
            with(NardahTownsfolk()) { scripts.startup() }
            with(NardahGeneralStore(shops)) { scripts.startup() }
            with(NardahHunterShop(shops)) { scripts.startup() }
            with(RoksChocsBox(shops)) { scripts.startup() }
            with(SedduAdventurersStore(shops)) { scripts.startup() }
            with(BedabinTownsfolk()) { scripts.startup() }
            with(BedabinVillageBartering(shops)) { scripts.startup() }
            with(Faaid()) { scripts.startup() }
            with(SophanemTownsfolk(shops, objRepo)) { scripts.startup() }
            with(RaetulClothStore(shops)) { scripts.startup() }
            with(JamilasCraftStall(shops)) { scripts.startup() }
            with(NathifasBakeStall(shops)) { scripts.startup() }
            with(BladesByUrbi(shops)) { scripts.startup() }
        }

        fun fillInventory() {
            for (slot in 0 until 28) player.inv[slot] = InvObj("obj.bronze_dagger", 1)
        }

        fun groundCount(obj: String): Int =
            objRegistry.findAll(player.coords).filter { it.type == obj.asRSCM() }.sumOf { it.count }

        fun talkTo(npc: String) {
            player.clearPendingAction(events)
            result = null
            player.activeCoroutine = coroutine
            val target = Npc(npc, player.coords.translateZ(1))
            val body: suspend () -> Unit = {
                val access = ProtectedAccess(player, coroutine, context)
                assertTrue(events.publish(access, NpcEvents.Op1(target)))
            }
            body.startCoroutine(
                object : Continuation<Unit> {
                    override val context = EmptyCoroutineContext

                    override fun resumeWith(result: Result<Unit>) {
                        this@Fixture.result = result
                    }
                }
            )
            result?.getOrThrow()
        }

        fun finish(options: List<Int>) {
            val selections = options.iterator()
            repeat(200) {
                if (coroutine.isIdle) return
                advance(selections)
            }
            fail<Unit>("Interaction did not finish: ${client.messages.joinToString("\n")}")
        }

        private fun advance(options: Iterator<Int>) {
            if (coroutine.isAwaiting(ResumePauseButtonInput::class)) {
                val input =
                    if (player.ui.containsModal("interface.chatmenu")) {
                        ResumePauseButtonInput(
                            "component.chatmenu:options",
                            if (options.hasNext()) options.next() else 1,
                        )
                    } else {
                        val component =
                            CONTINUE_COMPONENTS.entries
                                .firstOrNull { player.ui.containsModal(it.key) }
                                ?.value
                                ?: error("Unknown dialogue: ${client.messages.joinToString("\n")}")
                        ResumePauseButtonInput(component, -1)
                    }
                coroutine.resumeWith(input)
            } else {
                player.currentMapClock++
                player.processedMapClock = player.currentMapClock
                coroutine.advance()
            }
            result?.getOrThrow()
        }
    }

    private class RecordingClient : Client<Any, Any> {
        val messages = mutableListOf<Any>()

        override fun write(message: Any) {
            messages += message
        }

        override fun close() {}

        override fun read(player: Player) {}

        override fun flush() {}

        override fun flushHighPriority() {}

        override fun unregister(service: Any, player: Player) {}
    }

    companion object {
        private const val COINS = "obj.coins"
        private const val KEBAB = "obj.kebab"
        private const val SUPER_KEBAB = "obj.super_kebab"
        private const val SAUCE = "obj.superhot_kebab_sauce"
        private const val REDBERRIES = "obj.redberries"
        private const val ONION = "obj.onion"
        private const val WOAD_LEAF = "obj.woadleaf"
        private const val RED_DYE = "obj.reddye"
        private const val YELLOW_DYE = "obj.yellowdye"
        private const val BLUE_DYE = "obj.bluedye"
        private const val WATERSKIN = "obj.water_skin4"
        private const val KEBAB_SELLER = "npc.feud_kebabman_multi"
        private const val DYER = "npc.feud_dye_lady_multi"
        private const val CARTER = "npc.ali_the_carter"
        private const val HIGH_PRIEST = "npc.ics_little_hipriest_town"
        private const val CIRCLET = "obj.water_circlet"
        private val CONTINUE_COMPONENTS =
            mapOf(
                "interface.chat_left" to "component.chat_left:continue",
                "interface.chat_right" to "component.chat_right:continue",
                "interface.messagebox" to "component.messagebox:continue",
                "interface.objectbox" to "component.objectbox:universe",
            )
        private val restored = mutableListOf<() -> Unit>()

        @OptIn(InternalApi::class)
        @JvmStatic
        @BeforeAll
        fun cache() {
            ServerCacheManager.init(240).close()
            for ((owner, name) in
                listOf(
                    "org.rsmod.api.invtx.InvTransactionsScriptKt" to "cachedInventoryTransactions",
                    "org.rsmod.api.invtx.VirtualInvTransactionsKt" to "cachedPlayerItemStorage",
                )) {
                val field =
                    Class.forName(owner).getDeclaredField(name).apply { isAccessible = true }
                val old = field.get(null)
                restored += { field.set(null, old) }
            }
            val oldStorage = InvVirtualStorageHolder.instance
            restored += { InvVirtualStorageHolder.instance = oldStorage }
            with(InvTransactionsScript(PlayerItemStorage(emptySet()))) {
                ScriptContext(EventBus(), CheatCommandMap(), EngineQueueCache()).startup()
            }
        }

        @JvmStatic
        @AfterAll
        fun restore() {
            restored.asReversed().forEach { it() }
            restored.clear()
        }
    }
}
