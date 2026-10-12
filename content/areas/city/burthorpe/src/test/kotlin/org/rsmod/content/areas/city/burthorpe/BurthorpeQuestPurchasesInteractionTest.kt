package org.rsmod.content.areas.city.burthorpe

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import kotlin.coroutines.Continuation
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.coroutines.startCoroutine
import org.junit.jupiter.api.AfterAll
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
import org.rsmod.content.areas.city.burthorpe.npcs.BurthorpeTownsfolk
import org.rsmod.content.areas.city.burthorpe.npcs.TrollCountryGapNpcs
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
class BurthorpeQuestPurchasesInteractionTest {
    @Test fun `Tenzing sells Climbing boots for 12 coins`() {
        val f = Fixture()
        f.player.inv[0] = InvObj(COINS, 20)
        f.talkTo(TENZING)
        f.finish(listOf(1, 1, 5))
        assertEquals(8, f.player.inv.count(COINS))
        assertEquals(1, f.player.inv.count(CLIMBING_BOOTS))
    }

    @Test fun `Tenzing sells nothing to a player short of coins`() {
        val f = Fixture()
        f.player.inv[0] = InvObj(COINS, 11)
        f.talkTo(TENZING)
        f.finish(listOf(1, 1, 5))
        assertEquals(11, f.player.inv.count(COINS))
        assertEquals(0, f.player.inv.count(CLIMBING_BOOTS))
    }

    @Test fun `Dunstan sells a law talisman for 1000 coins`() {
        val f = Fixture()
        f.player.inv[0] = InvObj(COINS, 1_500)
        f.talkTo(DUNSTAN)
        f.finish(listOf(1, 1, 1))
        assertEquals(500, f.player.inv.count(COINS))
        assertEquals(1, f.player.inv.count(LAW_TALISMAN))
    }

    @Test fun `Dunstan sells no law talisman to a player short of coins`() {
        val f = Fixture()
        f.player.inv[0] = InvObj(COINS, 999)
        f.talkTo(DUNSTAN)
        f.finish(listOf(1, 1, 1))
        assertEquals(999, f.player.inv.count(COINS))
        assertEquals(0, f.player.inv.count(LAW_TALISMAN))
    }

    @Test fun `Dunstan sells no law talisman to a full pack`() {
        val f = Fixture()
        f.fillInventory()
        f.player.inv[0] = InvObj(COINS, 1_000)
        f.talkTo(DUNSTAN)
        f.finish(listOf(1, 1, 1))
        assertEquals(1_000, f.player.inv.count(COINS))
        assertEquals(0, f.player.inv.count(LAW_TALISMAN))
    }

    @Test fun `Dunstan offers no law talisman to a player with five`() {
        val f = Fixture()
        f.player.inv[0] = InvObj(COINS, 1_000)
        for (slot in 1..5) f.player.inv[slot] = InvObj(LAW_TALISMAN, 1)
        f.talkTo(DUNSTAN)
        f.finish(listOf(1, 4))
        assertEquals(1_000, f.player.inv.count(COINS))
        assertEquals(5, f.player.inv.count(LAW_TALISMAN))
    }

    @Test fun `Dunstan spikes Climbing boots for an iron bar`() {
        val f = Fixture()
        f.player.inv[0] = InvObj(CLIMBING_BOOTS, 1)
        f.player.inv[1] = InvObj(IRON_BAR, 1)
        f.talkTo(DUNSTAN)
        f.finish(listOf(1, 2, 1, 5))
        assertEquals(0, f.player.inv.count(CLIMBING_BOOTS))
        assertEquals(0, f.player.inv.count(IRON_BAR))
        assertEquals(1, f.player.inv.count(SPIKED_BOOTS))
    }

    @Test fun `Dunstan keeps the boots without an iron bar`() {
        val f = Fixture()
        f.player.inv[0] = InvObj(CLIMBING_BOOTS, 1)
        f.talkTo(DUNSTAN)
        f.finish(listOf(1, 2, 1, 5))
        assertEquals(1, f.player.inv.count(CLIMBING_BOOTS))
        assertEquals(0, f.player.inv.count(SPIKED_BOOTS))
    }

    @Test fun `Hild enchants a Catspeak amulet for five death runes`() {
        val f = Fixture()
        f.player.inv[0] = InvObj(CATSPEAK_AMULET, 1)
        f.player.inv[1] = InvObj(DEATH_RUNE, 7)
        f.talkTo(HILD)
        f.finish(emptyList())
        assertEquals(0, f.player.inv.count(CATSPEAK_AMULET))
        assertEquals(2, f.player.inv.count(DEATH_RUNE))
        assertEquals(1, f.player.inv.count(CATSPEAK_AMULET_E))
    }

    @Test fun `Hild keeps the runes without an amulet`() {
        val f = Fixture()
        f.player.inv[0] = InvObj(DEATH_RUNE, 5)
        f.talkTo(HILD)
        f.finish(emptyList())
        assertEquals(5, f.player.inv.count(DEATH_RUNE))
        assertEquals(0, f.player.inv.count(CATSPEAK_AMULET_E))
    }

    @Test fun `Hild keeps the amulet from a player short of death runes`() {
        val f = Fixture()
        f.player.inv[0] = InvObj(CATSPEAK_AMULET, 1)
        f.player.inv[1] = InvObj(DEATH_RUNE, 4)
        f.talkTo(HILD)
        f.finish(emptyList())
        assertEquals(1, f.player.inv.count(CATSPEAK_AMULET))
        assertEquals(4, f.player.inv.count(DEATH_RUNE))
        assertEquals(0, f.player.inv.count(CATSPEAK_AMULET_E))
    }

    private class Fixture {
        private val events = EventBus()
        private val client = RecordingClient()
        private val coroutine = GameCoroutine("burthorpe-quest-purchases-test")
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
                coords = CoordGrid(2898, 3546, 0)
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
            with(TrollCountryGapNpcs(objRepo)) { scripts.startup() }
            with(BurthorpeTownsfolk()) { scripts.startup() }
        }

        fun fillInventory() {
            for (slot in 0 until 28) player.inv[slot] = InvObj("obj.bronze_dagger", 1)
        }

        fun groundCount(obj: String): Int =
            objRegistry.findAll(player.coords).filter { it.type == obj.asRSCM() }.sumOf { it.count }

        fun talkTo(npc: String, op: (Npc) -> NpcEvents.Op = { NpcEvents.Op1(it) }) {
            player.clearPendingAction(events)
            result = null
            player.activeCoroutine = coroutine
            val target = Npc(npc, player.coords.translateZ(1))
            val body: suspend () -> Unit = {
                val access = ProtectedAccess(player, coroutine, context)
                assertTrue(events.publish(access, op(target)))
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
        private const val TENZING = "npc.death_sherpa"
        private const val DUNSTAN = "npc.death_smithy"
        private const val HILD = "npc.death_woman_indoors1"
        private const val CLIMBING_BOOTS = "obj.death_climbingboots"
        private const val SPIKED_BOOTS = "obj.death_spikedboots"
        private const val IRON_BAR = "obj.iron_bar"
        private const val LAW_TALISMAN = "obj.law_talisman"
        private const val DEATH_RUNE = "obj.deathrune"
        private const val CATSPEAK_AMULET = "obj.ics_little_amulet_of_catspeak"
        private const val CATSPEAK_AMULET_E = "obj.twocats_amuletofcatspeak"
        private val CONTINUE_COMPONENTS =
            mapOf(
                "interface.chat_left" to "component.chat_left:continue",
                "interface.chat_right" to "component.chat_right:continue",
                "interface.messagebox" to "component.messagebox:continue",
                "interface.objectbox" to "component.objectbox:universe",
                "interface.objectbox_double" to "component.objectbox_double:pausebutton",
            )
        private val restored = mutableListOf<() -> Unit>()
        private lateinit var previousPolicy: QuestRequirementPolicy

        @OptIn(InternalApi::class)
        @JvmStatic
        @BeforeAll
        fun cache() {
            ServerCacheManager.init(240).close()
            previousPolicy = QuestRequirements.activePolicy()
            QuestRequirements.install(QuestRequirementPolicy(QuestRequirementMode.AssumeCompleted))
            restored += { QuestRequirements.install(previousPolicy) }
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
