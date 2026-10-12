package org.rsmod.content.areas.city.falador

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
import org.junit.jupiter.api.BeforeEach
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
import org.rsmod.content.areas.city.falador.npcs.FaladorBarsScript
import org.rsmod.content.areas.city.falador.npcs.FaladorTownsfolkScript
import org.rsmod.content.areas.city.falador.npcs.MalignusMortiferGapNpc
import org.rsmod.content.areas.city.falador.npcs.SirTiffyCashien
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
class FaladorPurchasesInteractionTest {
    private lateinit var questPolicy: QuestRequirementPolicy

    @BeforeEach
    fun assumeQuestsCompleted() {
        questPolicy = QuestRequirements.activePolicy()
        QuestRequirements.install(QuestRequirementPolicy(QuestRequirementMode.AssumeCompleted))
    }

    @AfterEach
    fun restoreQuestPolicy() {
        QuestRequirements.install(questPolicy)
    }

    @Test fun `Malignius sells magic secateurs for 40000 coins`() {
        val f = Fixture()
        f.player.inv[0] = InvObj(COINS, 45_000)
        f.talkTo(MALIGNIUS)
        f.finish(listOf(1, 1, 3))
        assertEquals(5_000, f.player.inv.count(COINS))
        assertEquals(1, f.player.inv.count(MAGIC_SECATEURS))
    }

    @Test fun `Malignius keeps the secateurs from a player short of coins`() {
        val f = Fixture()
        f.player.inv[0] = InvObj(COINS, 39_999)
        f.talkTo(MALIGNIUS)
        f.finish(listOf(1, 1, 3))
        assertEquals(39_999, f.player.inv.count(COINS))
        assertEquals(0, f.player.inv.count(MAGIC_SECATEURS))
    }

    @Test fun `declining the secateurs costs nothing`() {
        for (options in listOf(listOf(1, 1, 1), listOf(1, 1, 2), listOf(1, 2))) {
            val f = Fixture()
            f.player.inv[0] = InvObj(COINS, 40_000)
            f.talkTo(MALIGNIUS)
            f.finish(options)
            assertEquals(40_000, f.player.inv.count(COINS), "$options")
            assertEquals(0, f.player.inv.count(MAGIC_SECATEURS), "$options")
        }
    }

    @Test fun `a full pack keeps the coins when the secateurs have no room`() {
        val f = Fixture()
        f.fillInventory()
        f.player.inv[0] = InvObj(COINS, 50_000)
        f.talkTo(MALIGNIUS)
        f.finish(listOf(1, 1, 3))
        assertEquals(50_000, f.player.inv.count(COINS))
        assertEquals(0, f.player.inv.count(MAGIC_SECATEURS))
        assertEquals(0, f.groundCount(MAGIC_SECATEURS))
    }

    @Test fun `paying the exact price frees the slot for the secateurs`() {
        val f = Fixture()
        f.fillInventory()
        f.player.inv[0] = InvObj(COINS, 40_000)
        f.talkTo(MALIGNIUS)
        f.finish(listOf(1, 1, 3))
        assertEquals(0, f.player.inv.count(COINS))
        assertEquals(1, f.player.inv.count(MAGIC_SECATEURS))
    }

    @Test fun `Malignius hands out a brown apron`() {
        val f = Fixture()
        f.talkTo(MALIGNIUS)
        f.finish(listOf(2))
        assertEquals(1, f.player.inv.count(BROWN_APRON))
    }

    @Test fun `a full pack drops the brown apron instead of losing it`() {
        val f = Fixture()
        f.fillInventory()
        f.talkTo(MALIGNIUS)
        f.finish(listOf(2))
        assertEquals(0, f.player.inv.count(BROWN_APRON))
        assertEquals(1, f.groundCount(BROWN_APRON))
    }

    @Test fun `Malignius offers no apron to a player carrying one`() {
        val f = Fixture()
        f.player.inv[0] = InvObj(BROWN_APRON, 1)
        f.player.inv[1] = InvObj(AIRTIGHT_POT, 1)
        f.talkTo(MALIGNIUS)
        f.finish(listOf(2))
        assertEquals(1, f.player.inv.count(BROWN_APRON))
        assertEquals(0, f.player.inv.count(AIRTIGHT_POT))
        assertEquals(1, f.player.inv.count(BONE_SEEDS))
    }

    @Test fun `Malignius fills an airtight pot with bone seeds`() {
        val f = Fixture()
        f.player.inv[0] = InvObj(AIRTIGHT_POT, 1)
        f.talkTo(MALIGNIUS)
        f.finish(listOf(3))
        assertEquals(0, f.player.inv.count(AIRTIGHT_POT))
        assertEquals(1, f.player.inv.count(BONE_SEEDS))
    }

    @Test fun `Malignius fills a pot and lid with bone seeds`() {
        val f = Fixture()
        f.player.inv[0] = InvObj(EMPTY_POT, 1)
        f.player.inv[1] = InvObj(POT_LID, 1)
        f.talkTo(MALIGNIUS)
        f.finish(listOf(3))
        assertEquals(0, f.player.inv.count(EMPTY_POT))
        assertEquals(0, f.player.inv.count(POT_LID))
        assertEquals(1, f.player.inv.count(BONE_SEEDS))
    }

    @Test fun `Malignius gives no bone seeds without a pot and lid`() {
        for (held in listOf(EMPTY_POT, POT_LID)) {
            val f = Fixture()
            f.player.inv[0] = InvObj(held, 1)
            f.talkTo(MALIGNIUS)
            f.finish(listOf(3))
            assertEquals(1, f.player.inv.count(held), held)
            assertEquals(0, f.player.inv.count(BONE_SEEDS), held)
        }
    }

    @Test fun `Sir Tiffy replaces a lost CommOrb`() {
        val f = Fixture()
        f.talkTo(SIR_TIFFY)
        f.finish(emptyList())
        assertEquals(1, f.player.inv.count(COMMORB))
    }

    @Test fun `a full pack drops the replacement CommOrb instead of losing it`() {
        val f = Fixture()
        f.fillInventory()
        f.talkTo(SIR_TIFFY)
        f.finish(emptyList())
        assertEquals(0, f.player.inv.count(COMMORB))
        assertEquals(1, f.groundCount(COMMORB))
    }

    @Test fun `Sir Tiffy gives no CommOrb to a player who owns one`() {
        for (banked in listOf(false, true)) {
            val f = Fixture()
            val inv = if (banked) f.bank else f.player.inv
            inv[0] = InvObj(COMMORB, 1)
            f.talkTo(SIR_TIFFY)
            f.finish(listOf(5))
            assertEquals(1, f.player.inv.count(COMMORB) + f.bank.count(COMMORB), "$banked")
            assertEquals(0, f.groundCount(COMMORB), "$banked")
        }
    }

    @Test fun `Wyson sells two woad leaves for 20 coins`() {
        val f = Fixture()
        f.player.inv[0] = InvObj(COINS, 25)
        f.talkTo(WYSON)
        f.finish(listOf(1, 4))
        assertEquals(5, f.player.inv.count(COINS))
        assertEquals(2, f.player.inv.count(WOAD_LEAF))
    }

    @Test fun `Wyson sells one woad leaf for 15 coins`() {
        val f = Fixture()
        f.player.inv[0] = InvObj(COINS, 15)
        f.talkTo(WYSON)
        f.finish(listOf(1, 3))
        assertEquals(0, f.player.inv.count(COINS))
        assertEquals(1, f.player.inv.count(WOAD_LEAF))
    }

    @Test fun `Wyson keeps his leaves from a player short of coins`() {
        val f = Fixture()
        f.player.inv[0] = InvObj(COINS, 19)
        f.talkTo(WYSON)
        f.finish(listOf(1, 4))
        assertEquals(19, f.player.inv.count(COINS))
        assertEquals(0, f.player.inv.count(WOAD_LEAF))
    }

    @Test fun `a lowball offer or declining Wyson costs nothing`() {
        for (options in listOf(listOf(1, 1), listOf(1, 2), listOf(3))) {
            val f = Fixture()
            f.player.inv[0] = InvObj(COINS, 20)
            f.talkTo(WYSON)
            f.finish(options)
            assertEquals(20, f.player.inv.count(COINS), "$options")
            assertEquals(0, f.player.inv.count(WOAD_LEAF), "$options")
        }
    }

    @Test fun `a full pack drops the woad leaves instead of losing them`() {
        val f = Fixture()
        f.fillInventory()
        f.player.inv[0] = InvObj(COINS, 25)
        f.talkTo(WYSON)
        f.finish(listOf(1, 4))
        assertEquals(5, f.player.inv.count(COINS))
        assertEquals(0, f.player.inv.count(WOAD_LEAF))
        assertEquals(2, f.groundCount(WOAD_LEAF))
    }

    @Test fun `every Rising Sun barmaid sells each ale for 3 coins`() {
        for (barmaid in BARMAIDS) {
            for ((option, ale) in ALES) {
                val f = Fixture()
                f.player.inv[0] = InvObj(COINS, 5)
                f.talkTo(barmaid)
                f.finish(listOf(option))
                assertEquals(2, f.player.inv.count(COINS), "$barmaid $ale")
                assertEquals(1, f.player.inv.count(ale), "$barmaid $ale")
            }
        }
    }

    @Test fun `barmaids keep the ale from a player short of coins`() {
        for (barmaid in BARMAIDS) {
            val f = Fixture()
            f.player.inv[0] = InvObj(COINS, 2)
            f.talkTo(barmaid)
            f.finish(listOf(1))
            assertEquals(2, f.player.inv.count(COINS), barmaid)
            assertEquals(0, f.player.inv.count(ASGARNIAN_ALE), barmaid)
        }
    }

    @Test fun `declining the ales costs nothing`() {
        val f = Fixture()
        f.player.inv[0] = InvObj(COINS, 5)
        f.talkTo(BARMAIDS.first())
        f.finish(listOf(4))
        assertEquals(5, f.player.inv.count(COINS))
        assertEquals(0, f.player.inv.count(ASGARNIAN_ALE))
    }

    @Test fun `a full pack drops the ale instead of losing it`() {
        val f = Fixture()
        f.fillInventory()
        f.player.inv[0] = InvObj(COINS, 5)
        f.talkTo(BARMAIDS.first())
        f.finish(listOf(1))
        assertEquals(2, f.player.inv.count(COINS))
        assertEquals(0, f.player.inv.count(ASGARNIAN_ALE))
        assertEquals(1, f.groundCount(ASGARNIAN_ALE))
    }

    @Test fun `barmaids buy every beer glass for 2 coins each`() {
        val f = Fixture()
        f.fillInventory()
        for (slot in 0 until 3) f.player.inv[slot] = InvObj(BEER_GLASS, 1)
        f.talkTo(BARMAIDS.first())
        f.finish(listOf(2, 1))
        assertEquals(0, f.player.inv.count(BEER_GLASS))
        assertEquals(6, f.player.inv.count(COINS))
        assertEquals(0, f.groundCount(COINS))
    }

    @Test fun `keeping the beer glasses costs nothing`() {
        val f = Fixture()
        f.player.inv[0] = InvObj(BEER_GLASS, 1)
        f.talkTo(BARMAIDS.first())
        f.finish(listOf(2, 2))
        assertEquals(1, f.player.inv.count(BEER_GLASS))
        assertEquals(0, f.player.inv.count(COINS))
    }

    @Test fun `Party Room barmaids sell a beer for 2 coins`() {
        for ((barmaid, options) in PARTY_BEER) {
            val f = Fixture()
            f.player.inv[0] = InvObj(COINS, 3)
            f.talkTo(barmaid)
            f.finish(options)
            assertEquals(1, f.player.inv.count(COINS), barmaid)
            assertEquals(1, f.player.inv.count(BEER), barmaid)
        }
    }

    @Test fun `Party Room barmaids keep the beer from a player short of coins`() {
        for ((barmaid, options) in PARTY_BEER) {
            val f = Fixture()
            f.player.inv[0] = InvObj(COINS, 1)
            f.talkTo(barmaid)
            f.finish(options)
            assertEquals(1, f.player.inv.count(COINS), barmaid)
            assertEquals(0, f.player.inv.count(BEER), barmaid)
        }
    }

    @Test fun `a full pack drops the Party Room beer instead of losing it`() {
        for ((barmaid, options) in PARTY_BEER) {
            val f = Fixture()
            f.fillInventory()
            f.player.inv[0] = InvObj(COINS, 3)
            f.talkTo(barmaid)
            f.finish(options)
            assertEquals(1, f.player.inv.count(COINS), barmaid)
            assertEquals(0, f.player.inv.count(BEER), barmaid)
            assertEquals(1, f.groundCount(BEER), barmaid)
        }
    }

    private class Fixture {
        private val events = EventBus()
        private val client = RecordingClient()
        private val coroutine = GameCoroutine("falador-purchases-test")
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
                coords = CoordGrid(2958, 3372, 0)
                currentMapClock = 100
                processedMapClock = 100
                inv =
                    Inventory(
                        checkNotNull(ServerCacheManager.getInventory("inv.inv".asRSCM())),
                        arrayOfNulls(28),
                    )
                invMap["inv.inv"] = inv
            }

        val bank: Inventory =
            checkNotNull(ServerCacheManager.getInventory("inv.bank".asRSCM())).let { type ->
                Inventory(type, arrayOfNulls(type.size)).also { player.invMap["inv.bank"] = it }
            }

        init {
            val scripts = ScriptContext(events, CheatCommandMap(), EngineQueueCache())
            val objRepo = ObjRepository(clock, objRegistry)
            with(FaladorTownsfolkScript(objRepo)) { scripts.startup() }
            with(FaladorBarsScript(objRepo)) { scripts.startup() }
            with(MalignusMortiferGapNpc(objRepo)) { scripts.startup() }
            with(SirTiffyCashien(Shops(events), objRepo)) { scripts.startup() }
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
                    } else if (player.ui.containsModal("interface.objectbox")) {
                        ResumePauseButtonInput("component.objectbox:universe", -1)
                    } else {
                        val parent =
                            listOf("chat_left", "chat_right", "messagebox").firstOrNull {
                                player.ui.containsModal("interface.$it")
                            } ?: error("Unknown dialogue: ${client.messages.joinToString("\n")}")
                        ResumePauseButtonInput("component.$parent:continue", -1)
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
        private const val BEER_GLASS = "obj.beer_glass"
        private const val WOAD_LEAF = "obj.woadleaf"
        private const val ASGARNIAN_ALE = "obj.asgarnian_ale"
        private const val WYSON = "npc.wyson"
        private const val MALIGNIUS = "npc.elemental_wizard_boss"
        private const val SIR_TIFFY = "npc.rd_teleporter_guy"
        private const val MAGIC_SECATEURS = "obj.fairy_enchanted_secateurs"
        private const val BROWN_APRON = "obj.brown_apron"
        private const val AIRTIGHT_POT = "obj.favour_airtight_pot"
        private const val EMPTY_POT = "obj.pot_empty"
        private const val POT_LID = "obj.potlid"
        private const val BONE_SEEDS = "obj.swan_army"
        private const val COMMORB = "obj.slug2_crystal_ball"
        private val BARMAIDS =
            listOf("npc.risingsun_barmaid", "npc.risingsun_barmaid2", "npc.risingsun_barmaid3")
        private val ALES =
            listOf(1 to ASGARNIAN_ALE, 2 to "obj.wizards_mind_bomb", 3 to "obj.dwarven_stout")
        private val PARTY_BEER =
            mapOf("npc.partyroom_barmaid" to listOf(1), "npc.partyroom_barmaid2" to listOf())
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
