package org.rsmod.content.areas.city.catherby

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
import org.rsmod.api.player.input.ResumePCountDialogInput
import org.rsmod.api.player.input.ResumePauseButtonInput
import org.rsmod.api.player.interact.NpcInteractions
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.protect.ProtectedAccessContextFactory
import org.rsmod.api.player.protect.clearPendingAction
import org.rsmod.api.registry.obj.ObjRegistry
import org.rsmod.api.registry.zone.ZoneUpdateMap
import org.rsmod.content.areas.city.catherby.npcs.CatherbyTownsfolk
import org.rsmod.content.quest.manager.QuestRequirementMode
import org.rsmod.content.quest.manager.QuestRequirementPolicy
import org.rsmod.content.quest.manager.QuestRequirements
import org.rsmod.coroutine.GameCoroutine
import org.rsmod.events.EventBus
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
class CalebGauntletsInteractionTest {
    private val previous = QuestRequirements.activePolicy()

    @AfterEach
    fun restorePolicy() {
        QuestRequirements.install(previous)
    }

    private fun afterFamilyCrestOnly() {
        QuestRequirements.install(
            QuestRequirementPolicy(
                QuestRequirementMode.VirtualCompletions,
                virtualCompletions = setOf(FAMILY_CREST),
            )
        )
    }

    private fun afterFamilyPest() {
        QuestRequirements.install(QuestRequirementPolicy(QuestRequirementMode.AssumeCompleted))
    }

    @Test fun `Caleb changes goldsmith gauntlets for 25,000 coins`() {
        afterFamilyCrestOnly()
        val f = Fixture()
        f.player.inv[0] = InvObj(COINS, 30_000)
        f.player.inv[1] = InvObj(GOLDSMITH, 1)
        f.talkTo(CALEB)
        f.finish(listOf(2, 1, 1))
        assertEquals(5_000, f.player.inv.count(COINS))
        assertEquals(0, f.player.inv.count(GOLDSMITH))
        assertEquals(1, f.player.inv.count(COOKING))
    }

    @Test fun `Caleb keeps the gauntlets of a player short of coins`() {
        afterFamilyCrestOnly()
        val f = Fixture()
        f.player.inv[0] = InvObj(COINS, 24_999)
        f.player.inv[1] = InvObj(GOLDSMITH, 1)
        f.talkTo(CALEB)
        f.finish(listOf(2, 1))
        assertEquals(24_999, f.player.inv.count(COINS))
        assertEquals(1, f.player.inv.count(GOLDSMITH))
        assertEquals(0, f.player.inv.count(COOKING))
    }

    @Test fun `declining the change costs nothing`() {
        afterFamilyCrestOnly()
        val f = Fixture()
        f.player.inv[0] = InvObj(COINS, 30_000)
        f.player.inv[1] = InvObj(GOLDSMITH, 1)
        f.talkTo(CALEB)
        f.finish(listOf(2, 1, 2))
        assertEquals(30_000, f.player.inv.count(COINS))
        assertEquals(1, f.player.inv.count(GOLDSMITH))
        assertEquals(0, f.player.inv.count(COOKING))
    }

    @Test fun `Caleb changes steel gauntlets for free`() {
        afterFamilyCrestOnly()
        val f = Fixture()
        f.player.inv[0] = InvObj(STEEL, 1)
        f.talkTo(CALEB)
        f.finish(listOf(2, 1))
        assertEquals(0, f.player.inv.count(STEEL))
        assertEquals(1, f.player.inv.count(COOKING))
    }

    @Test fun `Caleb leaves other gauntlets alone when the player has cooking gauntlets`() {
        afterFamilyCrestOnly()
        val f = Fixture()
        f.player.inv[0] = InvObj(COINS, 30_000)
        f.player.inv[1] = InvObj(GOLDSMITH, 1)
        f.player.worn[9] = InvObj(COOKING, 1)
        f.talkTo(CALEB)
        f.finish(listOf(2, 1))
        assertEquals(30_000, f.player.inv.count(COINS))
        assertEquals(1, f.player.inv.count(GOLDSMITH))
        assertEquals(0, f.player.inv.count(COOKING))
    }

    @Test fun `Caleb hands out the cooking gauntlets asked for after Family Pest`() {
        afterFamilyPest()
        val f = Fixture()
        f.talkTo(CALEB)
        f.finish(listOf(1), count = 3)
        assertEquals(3, f.player.inv.count(COOKING))
    }

    @Test fun `Caleb's gauntlets are capped at the free inventory space`() {
        afterFamilyPest()
        val f = Fixture()
        f.fillInventory()
        f.player.inv[0] = null
        f.player.inv[1] = null
        f.talkTo(CALEB)
        f.finish(listOf(1), count = Int.MAX_VALUE)
        assertEquals(2, f.player.inv.count(COOKING))
        assertEquals(0, f.groundCount(COOKING))
    }

    @Test fun `a full pack gets no cooking gauntlets`() {
        afterFamilyPest()
        val f = Fixture()
        f.fillInventory()
        f.talkTo(CALEB)
        f.finish(listOf(1), count = 1)
        assertEquals(0, f.player.inv.count(COOKING))
        assertEquals(0, f.groundCount(COOKING))
    }

    private class Fixture {
        private val events = EventBus()
        private val client = RecordingClient()
        private val coroutine = GameCoroutine("caleb-gauntlets-test")
        private var result: Result<Unit>? = null
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
                coords = CoordGrid(3213, 3428, 0)
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
            }

        init {
            val scripts = ScriptContext(events, CheatCommandMap(), EngineQueueCache())
            with(CatherbyTownsfolk()) { scripts.startup() }
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

        fun finish(options: List<Int>, count: Int = 0) {
            val selections = options.iterator()
            repeat(100) {
                if (coroutine.isIdle) return
                advance(selections, count)
            }
            fail<Unit>("Interaction did not finish: ${client.messages.joinToString("\n")}")
        }

        private fun advance(options: Iterator<Int>, count: Int) {
            if (coroutine.isAwaiting(ResumePCountDialogInput::class)) {
                coroutine.resumeWith(ResumePCountDialogInput(count))
            } else if (coroutine.isAwaiting(ResumePauseButtonInput::class)) {
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
        private val CONTINUE_COMPONENTS =
            mapOf(
                "interface.chat_left" to "component.chat_left:continue",
                "interface.chat_right" to "component.chat_right:continue",
                "interface.messagebox" to "component.messagebox:continue",
                "interface.objectbox" to "component.objectbox:universe",
            )
        private const val COINS = "obj.coins"
        private const val CALEB = "npc.caleb_fitzharmon_1op"
        private const val COOKING = "obj.gauntlets_of_cooking"
        private const val GOLDSMITH = "obj.gauntlets_of_goldsmithing"
        private const val STEEL = "obj.steel_gauntlets"
        private const val FAMILY_CREST = "quest_familycrest"
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
