package org.rsmod.content.areas.city.taverley

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
import org.rsmod.content.areas.city.taverley.npcs.TaverleyDruids
import org.rsmod.content.areas.city.taverley.npcs.TaverleyTownsfolk
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
class TaverleyQuestPurchasesInteractionTest {
    @Test fun `Kaqemeex sells a Herblore cape and hood for 99000 coins`() {
        val f = Fixture()
        f.player.statMap.setBaseLevel("stat.herblore", 99)
        f.player.inv[0] = InvObj(COINS, 100_000)
        f.talkTo(KAQEMEEX)
        f.finish(listOf(3, 2))
        assertEquals(1_000, f.player.inv.count(COINS))
        assertEquals(1, f.player.inv.count(HERBLORE_CAPE))
        assertEquals(1, f.player.inv.count(HERBLORE_HOOD))
    }

    @Test fun `Kaqemeex sells nothing to a player short of coins`() {
        val f = Fixture()
        f.player.statMap.setBaseLevel("stat.herblore", 99)
        f.player.inv[0] = InvObj(COINS, CAPE_PRICE - 1)
        f.talkTo(KAQEMEEX)
        f.finish(listOf(3))
        assertEquals(CAPE_PRICE - 1, f.player.inv.count(COINS))
        assertEquals(0, f.player.inv.count(HERBLORE_CAPE))
    }

    @Test fun `Kaqemeex keeps the cape from a player without two free slots`() {
        val f = Fixture()
        f.player.statMap.setBaseLevel("stat.herblore", 99)
        f.fillInventory()
        f.player.inv[0] = InvObj(COINS, CAPE_PRICE)
        f.player.inv[1] = null
        f.talkTo(KAQEMEEX)
        f.finish(listOf(3))
        assertEquals(CAPE_PRICE, f.player.inv.count(COINS))
        assertEquals(0, f.player.inv.count(HERBLORE_CAPE))
        assertEquals(0, f.groundCount(HERBLORE_CAPE))
    }

    @Test fun `declining Kaqemeex's price costs nothing`() {
        val f = Fixture()
        f.player.statMap.setBaseLevel("stat.herblore", 99)
        f.player.inv[0] = InvObj(COINS, CAPE_PRICE)
        f.talkTo(KAQEMEEX)
        f.finish(listOf(3, 1))
        assertEquals(CAPE_PRICE, f.player.inv.count(COINS))
        assertEquals(0, f.player.inv.count(HERBLORE_CAPE))
    }

    @Test fun `Kaqemeex gives a cape owner one free hood`() {
        val f = Fixture()
        f.player.statMap.setBaseLevel("stat.herblore", 99)
        f.player.inv[0] = InvObj(HERBLORE_CAPE, 1)
        f.talkTo(KAQEMEEX)
        f.finish(listOf(4))
        assertEquals(1, f.player.inv.count(HERBLORE_CAPE))
        assertEquals(1, f.player.inv.count(HERBLORE_HOOD))
    }

    @Test fun `the Lady of the Lake returns Excalibur for 500 coins`() {
        val f = Fixture()
        f.player.inv[0] = InvObj(COINS, 600)
        f.talkTo(LADY_OF_THE_LAKE)
        f.finish(listOf(3))
        assertEquals(100, f.player.inv.count(COINS))
        assertEquals(1, f.player.inv.count(EXCALIBUR))
    }

    @Test fun `the Lady of the Lake keeps Excalibur from a player short of coins`() {
        val f = Fixture()
        f.player.inv[0] = InvObj(COINS, 499)
        f.talkTo(LADY_OF_THE_LAKE)
        f.finish(listOf(3))
        assertEquals(499, f.player.inv.count(COINS))
        assertEquals(0, f.player.inv.count(EXCALIBUR))
    }

    @Test fun `the Lady of the Lake offers no second Excalibur`() {
        val f = Fixture()
        f.player.inv[0] = InvObj(EXCALIBUR, 1)
        f.player.inv[1] = InvObj(COINS, 600)
        f.talkTo(LADY_OF_THE_LAKE)
        f.finish(listOf(2))
        assertEquals(600, f.player.inv.count(COINS))
        assertEquals(1, f.player.inv.count(EXCALIBUR))
    }

    private class Fixture {
        private val events = EventBus()
        private val client = RecordingClient()
        private val coroutine = GameCoroutine("taverley-quest-purchases-test")
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
                coords = CoordGrid(2925, 3484, 0)
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
            with(TaverleyDruids(objRepo)) { scripts.startup() }
            with(TaverleyTownsfolk(objRepo)) { scripts.startup() }
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
        private const val KAQEMEEX = "npc.kaqemeex"
        private const val LADY_OF_THE_LAKE = "npc.ladyofthelake"
        private const val HERBLORE_CAPE = "obj.skillcape_herblore"
        private const val HERBLORE_HOOD = "obj.skillcape_herblore_hood"
        private const val EXCALIBUR = "obj.excalibur"
        private const val CAPE_PRICE = 99_000
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
