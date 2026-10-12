package org.rsmod.content.areas.misc.villages

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
import org.rsmod.content.areas.misc.villages.barbarianoutpost.npcs.BarbarianAssaultLobby
import org.rsmod.content.areas.misc.villages.barbarianoutpost.npcs.BarbarianOutpostTownsfolk
import org.rsmod.content.areas.misc.villages.barbarianoutpost.npcs.Otto
import org.rsmod.content.areas.misc.villages.baxtorianfalls.npcs.BaxtorianFalls
import org.rsmod.content.areas.misc.villages.castlewars.npcs.Lanthus
import org.rsmod.content.areas.misc.villages.clocktower.npcs.BrotherKojo
import org.rsmod.content.areas.misc.villages.feldip.npcs.GuTanoth
import org.rsmod.content.areas.misc.villages.feldip.npcs.HuntingExperts
import org.rsmod.content.areas.misc.villages.fishingplatform.npcs.FishingPlatformNpcs
import org.rsmod.content.areas.misc.villages.hemenster.npcs.FishingContestants
import org.rsmod.content.areas.misc.villages.hemenster.npcs.GrandpaJack
import org.rsmod.content.areas.misc.villages.observatory.npcs.Observatory
import org.rsmod.content.areas.misc.villages.observatory.npcs.TowerOfLife
import org.rsmod.content.areas.misc.villages.piscatoris.npcs.ArnoldLydspor
import org.rsmod.content.areas.misc.villages.piscatoris.npcs.PiscatorisColonists
import org.rsmod.content.areas.misc.villages.portkhazard.npcs.FightArena
import org.rsmod.content.areas.misc.villages.portkhazard.npcs.Murphy
import org.rsmod.content.areas.misc.villages.portkhazard.npcs.TindelMarchant
import org.rsmod.content.areas.misc.villages.portkhazard.npcs.shops.KhazardGeneralStore
import org.rsmod.content.areas.misc.villages.witchaven.npcs.EzekialLovecraft
import org.rsmod.content.areas.misc.villages.witchaven.npcs.WitchavenTownsfolk
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
import org.rsmod.game.inv.InvObj
import org.rsmod.game.inv.InvVirtualStorageHolder
import org.rsmod.game.inv.Inventory
import org.rsmod.game.queue.EngineQueueCache
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.ScriptContext

@Execution(ExecutionMode.SAME_THREAD)
@ResourceLock("ServerCacheManager")
@ResourceLock("QuestRequirements")
class VillagesPurchasesInteractionTest {
    private val previousPolicy = QuestRequirements.activePolicy()

    @AfterEach
    fun restorePolicy() {
        QuestRequirements.install(previousPolicy)
    }

    @Test fun `the Khazard barman sells a beer for 2 coins`() {
        val f = Fixture()
        f.player.inv[0] = InvObj(COINS, 5)
        f.talkTo(BARMAN)
        f.finish(listOf(1))
        assertEquals(3, f.player.inv.count(COINS))
        assertEquals(1, f.player.inv.count(BEER))
    }

    @Test fun `the Khazard barman keeps the beer from a player short of coins`() {
        val f = Fixture()
        f.player.inv[0] = InvObj(COINS, 1)
        f.talkTo(BARMAN)
        f.finish(listOf(1))
        assertEquals(1, f.player.inv.count(COINS))
        assertEquals(0, f.player.inv.count(BEER))
    }

    @Test fun `the Khazard barman takes nothing from a full pack`() {
        val f = Fixture()
        f.fillInventory()
        f.player.inv[0] = InvObj(COINS, 5)
        f.talkTo(BARMAN)
        f.finish(listOf(1))
        assertEquals(5, f.player.inv.count(COINS))
        assertEquals(0, f.player.inv.count(BEER))
        assertEquals(0, f.groundCount(BEER))
    }

    @Test fun `declining the Khazard barman costs nothing`() {
        val f = Fixture()
        f.player.inv[0] = InvObj(COINS, 5)
        f.talkTo(BARMAN)
        f.finish(listOf(2, 3))
        assertEquals(5, f.player.inv.count(COINS))
        assertEquals(0, f.player.inv.count(BEER))
    }

    @Test fun `Grandpa Jack sells a fishing rod for 5 coins`() {
        val f = Fixture()
        f.player.inv[0] = InvObj(COINS, 12)
        f.talkTo(GRANDPA_JACK)
        f.finish(listOf(3, 1))
        assertEquals(7, f.player.inv.count(COINS))
        assertEquals(1, f.player.inv.count(FISHING_ROD))
    }

    @Test fun `Grandpa Jack sells nothing to a player short of coins`() {
        val f = Fixture()
        f.player.inv[0] = InvObj(COINS, 4)
        f.talkTo(GRANDPA_JACK)
        f.finish(listOf(3))
        assertEquals(4, f.player.inv.count(COINS))
        assertEquals(0, f.player.inv.count(FISHING_ROD))
    }

    @Test fun `declining Grandpa Jack's rod costs nothing`() {
        val f = Fixture()
        f.player.inv[0] = InvObj(COINS, 12)
        f.talkTo(GRANDPA_JACK)
        f.finish(listOf(3, 2))
        assertEquals(12, f.player.inv.count(COINS))
        assertEquals(0, f.player.inv.count(FISHING_ROD))
    }

    @Test fun `Grandpa Jack won't sell a second rod`() {
        val f = Fixture()
        f.player.inv[0] = InvObj(COINS, 12)
        f.player.inv[1] = InvObj(FISHING_ROD, 1)
        f.talkTo(GRANDPA_JACK)
        f.finish(listOf(3))
        assertEquals(12, f.player.inv.count(COINS))
        assertEquals(1, f.player.inv.count(FISHING_ROD))
    }

    @Test fun `Grandpa Jack takes nothing from a full pack`() {
        val f = Fixture()
        f.fillInventory()
        f.player.inv[0] = InvObj(COINS, 12)
        f.talkTo(GRANDPA_JACK)
        f.finish(listOf(3, 1))
        assertEquals(12, f.player.inv.count(COINS))
        assertEquals(0, f.player.inv.count(FISHING_ROD))
        assertEquals(0, f.groundCount(FISHING_ROD))
    }

    @Test fun `Arnold gives a cabbage to a player who came to chat`() {
        val f = Fixture()
        f.talkTo(ARNOLD)
        f.finish(listOf(5))
        assertEquals(1, f.player.inv.count(CABBAGE))
    }

    @Test fun `a full pack drops Arnold's cabbage instead of losing it`() {
        val f = Fixture()
        f.fillInventory()
        f.talkTo(ARNOLD)
        f.finish(listOf(5))
        assertEquals(0, f.player.inv.count(CABBAGE))
        assertEquals(1, f.groundCount(CABBAGE))
    }

    @Test fun `Franklin buys every iron sheet for 20 coins each`() {
        val f = Fixture()
        for (slot in 0 until 3) f.player.inv[slot] = InvObj(IRON_SHEET, 1)
        f.talkTo(FRANKLIN)
        f.finish(listOf(1))
        assertEquals(0, f.player.inv.count(IRON_SHEET))
        assertEquals(60, f.player.inv.count(COINS))
    }

    @Test fun `Franklin buys a full pack of iron sheets`() {
        val f = Fixture()
        for (slot in 0 until 28) f.player.inv[slot] = InvObj(IRON_SHEET, 1)
        f.talkTo(FRANKLIN)
        f.finish(listOf(1))
        assertEquals(0, f.player.inv.count(IRON_SHEET))
        assertEquals(28 * 20, f.player.inv.count(COINS))
        assertEquals(0, f.groundCount(COINS))
    }

    @Test fun `refusing Franklin keeps the iron sheets`() {
        val f = Fixture()
        for (slot in 0 until 3) f.player.inv[slot] = InvObj(IRON_SHEET, 1)
        f.talkTo(FRANKLIN)
        f.finish(listOf(2))
        assertEquals(3, f.player.inv.count(IRON_SHEET))
        assertEquals(0, f.player.inv.count(COINS))
    }

    @Test fun `Franklin pays nothing to a player without iron sheets`() {
        val f = Fixture()
        f.talkTo(FRANKLIN)
        f.finish(emptyList())
        assertEquals(0, f.player.inv.count(COINS))
    }

    @Test fun `Otto gives one copy of My notes`() {
        val f = Fixture()
        f.talkTo(OTTO)
        f.finish(listOf(2, 3))
        assertEquals(1, f.player.inv.count(MY_NOTES))
    }

    @Test fun `Otto offers no notes to a player who has them`() {
        val f = Fixture()
        f.player.inv[0] = InvObj(MY_NOTES, 1)
        f.talkTo(OTTO)
        f.finish(listOf(3))
        assertEquals(1, f.player.inv.count(MY_NOTES))
    }

    @Test fun `a full pack drops Otto's notes instead of losing them`() {
        val f = Fixture()
        f.fillInventory()
        f.talkTo(OTTO)
        f.finish(listOf(2, 3))
        assertEquals(0, f.player.inv.count(MY_NOTES))
        assertEquals(1, f.groundCount(MY_NOTES))
    }

    @Test fun `Lanthus hands out a Castle Wars manual`() {
        val f = Fixture()
        f.talkTo(LANTHUS)
        f.finish(listOf(3))
        assertEquals(1, f.player.inv.count(MANUAL))
    }

    @Test fun `a full pack drops the Castle Wars manual instead of losing it`() {
        val f = Fixture()
        f.fillInventory()
        f.talkTo(LANTHUS)
        f.finish(listOf(3))
        assertEquals(0, f.player.inv.count(MANUAL))
        assertEquals(1, f.groundCount(MANUAL))
    }

    @Test fun `the Observatory professor gives a chart`() {
        val f = Fixture()
        f.talkTo(PROFESSOR)
        f.finish(listOf(2))
        assertEquals(1, f.player.inv.count(CHART))
    }

    @Test fun `the Observatory professor keeps the chart from a full pack`() {
        val f = Fixture()
        f.fillInventory()
        f.talkTo(PROFESSOR)
        f.finish(listOf(2))
        assertEquals(0, f.player.inv.count(CHART))
        assertEquals(0, f.groundCount(CHART))
    }

    @Test fun `Black-eye gives a hard hat for three right answers after Tower of Life`() {
        QuestRequirements.install(QuestRequirementPolicy(QuestRequirementMode.AssumeCompleted))
        val f = Fixture()
        f.talkTo(BLACK_EYE)
        f.finish(listOf(3, 1, 2))
        assertEquals(1, f.player.inv.count(HARD_HAT))
    }

    @Test fun `a full pack drops Black-eye's hard hat instead of losing it`() {
        QuestRequirements.install(QuestRequirementPolicy(QuestRequirementMode.AssumeCompleted))
        val f = Fixture()
        f.fillInventory()
        f.talkTo(BLACK_EYE)
        f.finish(listOf(3, 1, 2))
        assertEquals(0, f.player.inv.count(HARD_HAT))
        assertEquals(1, f.groundCount(HARD_HAT))
    }

    @Test fun `Black-eye gives nothing for a wrong answer`() {
        QuestRequirements.install(QuestRequirementPolicy(QuestRequirementMode.AssumeCompleted))
        val f = Fixture()
        f.talkTo(BLACK_EYE)
        f.finish(listOf(3, 1, 1))
        assertEquals(0, f.player.inv.count(HARD_HAT))
    }

    @Test fun `Black-eye gives no second hard hat`() {
        QuestRequirements.install(QuestRequirementPolicy(QuestRequirementMode.AssumeCompleted))
        val f = Fixture()
        f.player.inv[0] = InvObj(HARD_HAT, 1)
        f.talkTo(BLACK_EYE)
        f.finish(listOf(3, 1, 2))
        assertEquals(1, f.player.inv.count(HARD_HAT))
    }

    @Test fun `Black-eye gives no hard hat before Tower of Life`() {
        QuestRequirements.install(QuestRequirementPolicy(QuestRequirementMode.RespectProgress))
        val f = Fixture()
        f.talkTo(BLACK_EYE)
        f.finish(listOf(3, 1, 2))
        assertEquals(0, f.player.inv.count(HARD_HAT))
    }

    @Test fun `Rasolo replaces a lost ring of visibility after Desert Treasure I`() {
        QuestRequirements.install(AFTER_DESERT_TREASURE_I)
        val f = Fixture()
        f.talkTo(RASOLO)
        f.finish(listOf(3))
        assertEquals(1, f.player.inv.count(RING_OF_VISIBILITY))
    }

    @Test fun `Rasolo keeps the ring from a full pack`() {
        QuestRequirements.install(AFTER_DESERT_TREASURE_I)
        val f = Fixture()
        f.fillInventory()
        f.talkTo(RASOLO)
        f.finish(listOf(3))
        assertEquals(0, f.player.inv.count(RING_OF_VISIBILITY))
        assertEquals(0, f.groundCount(RING_OF_VISIBILITY))
    }

    @Test fun `Rasolo gives no ring to a player with one in the bank`() {
        QuestRequirements.install(AFTER_DESERT_TREASURE_I)
        val f = Fixture()
        val bank = f.bank()
        bank[0] = InvObj(RING_OF_VISIBILITY, 1)
        f.talkTo(RASOLO)
        f.finish(listOf(3))
        assertEquals(0, f.player.inv.count(RING_OF_VISIBILITY))
        assertEquals(1, bank.count(RING_OF_VISIBILITY))
    }

    @Test fun `Rasolo has no ring left after Desert Treasure II`() {
        QuestRequirements.install(QuestRequirementPolicy(QuestRequirementMode.AssumeCompleted))
        val f = Fixture()
        f.talkTo(RASOLO)
        f.finish(listOf(3))
        assertEquals(0, f.player.inv.count(RING_OF_VISIBILITY))
    }

    private class Fixture {
        private val events = EventBus()
        private val client = RecordingClient()
        private val coroutine = GameCoroutine("villages-purchases-test")
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
                uuid = 795L
                observerUUID = 795L
                slotId = 1
                assignUid()
                coords = CoordGrid(2655, 3160, 0)
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
            with(Murphy()) { scripts.startup() }
            with(TindelMarchant()) { scripts.startup() }
            with(KhazardGeneralStore(shops)) { scripts.startup() }
            with(FightArena(objRepo)) { scripts.startup() }
            with(WitchavenTownsfolk()) { scripts.startup() }
            with(EzekialLovecraft()) { scripts.startup() }
            with(FishingPlatformNpcs()) { scripts.startup() }
            with(FishingContestants()) { scripts.startup() }
            with(GrandpaJack(objRepo)) { scripts.startup() }
            with(BarbarianOutpostTownsfolk()) { scripts.startup() }
            with(BarbarianAssaultLobby()) { scripts.startup() }
            with(Otto(objRepo)) { scripts.startup() }
            with(ArnoldLydspor(shops, objRepo)) { scripts.startup() }
            with(PiscatorisColonists(objRepo)) { scripts.startup() }
            with(GuTanoth(shops)) { scripts.startup() }
            with(HuntingExperts()) { scripts.startup() }
            with(Lanthus(objRepo)) { scripts.startup() }
            with(Observatory(objRepo)) { scripts.startup() }
            with(TowerOfLife(objRepo)) { scripts.startup() }
            with(BaxtorianFalls(shops)) { scripts.startup() }
            with(BrotherKojo()) { scripts.startup() }
        }

        fun bank(): Inventory =
            Inventory(
                    checkNotNull(ServerCacheManager.getInventory("inv.bank".asRSCM())),
                    arrayOfNulls(
                        checkNotNull(ServerCacheManager.getInventory("inv.bank".asRSCM())).size
                    ),
                )
                .also { player.invMap["inv.bank"] = it }

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
            repeat(100) {
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
        private const val BEER = "obj.beer"
        private const val FISHING_ROD = "obj.fishing_rod"
        private const val CABBAGE = "obj.cabbage"
        private const val IRON_SHEET = "obj.iron_sheet"
        private const val MY_NOTES = "obj.brut_logbook"
        private const val MANUAL = "obj.castlewars_manual"
        private const val CHART = "obj.trail_chart"
        private const val BARMAN = "npc.khazard_barman"
        private const val GRANDPA_JACK = "npc.grandpa_jack"
        private const val ARNOLD = "npc.swan_arnold"
        private const val FRANKLIN = "npc.swan_franklin"
        private const val OTTO = "npc.brut_otto"
        private const val LANTHUS = "npc.castlewars_judge_model"
        private const val PROFESSOR = "npc.observatory_professor_multi"
        private const val BLACK_EYE = "npc.tol_npc_builder01"
        private const val HARD_HAT = "obj.tol_player_construction_hardhat"
        private const val RASOLO = "npc.shadow_warrior_rasool"
        private const val RING_OF_VISIBILITY = "obj.fd_ring_visibility"
        private val AFTER_DESERT_TREASURE_I =
            QuestRequirementPolicy(
                QuestRequirementMode.VirtualCompletions,
                virtualCompletions = setOf("quest_deserttreasure"),
            )
        private val CONTINUE_COMPONENTS =
            mapOf(
                "interface.chat_left" to "component.chat_left:continue",
                "interface.chat_right" to "component.chat_right:continue",
                "interface.messagebox" to "component.messagebox:continue",
                "interface.objectbox" to "component.objectbox:universe",
                "interface.objectbox_double" to "component.objectbox_double:pausebutton",
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
