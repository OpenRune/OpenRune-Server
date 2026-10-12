package org.rsmod.content.areas.misc.outskirts

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
import org.rsmod.content.areas.misc.outskirts.digsite.npcs.DigsiteNpcs
import org.rsmod.content.areas.misc.outskirts.monastery.npcs.EdgevilleMonastery
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
class OutskirtsInteractionTest {
    private lateinit var questPolicy: QuestRequirementPolicy

    @BeforeEach
    fun respectQuestProgress() {
        questPolicy = QuestRequirements.activePolicy()
        QuestRequirements.install(QuestRequirementPolicy(QuestRequirementMode.RespectProgress))
    }

    @AfterEach
    fun restoreQuestPolicy() {
        QuestRequirements.install(questPolicy)
    }

    @Test fun `Examiner replaces a lost trowel after The Dig Site`() {
        digSiteCompleted()
        val f = Fixture()
        f.talkTo(EXAMINER)
        f.finish(listOf(2))
        assertEquals(1, f.player.inv.count(TROWEL))
    }

    @Test fun `Examiner drops the replacement trowel when the pack is full`() {
        digSiteCompleted()
        val f = Fixture()
        f.fillInventory()
        f.talkTo(EXAMINER)
        f.finish(listOf(2))
        assertEquals(0, f.player.inv.count(TROWEL))
        assertEquals(1, f.groundCount(TROWEL))
    }

    @Test fun `Examiner gives no trowel when one is in the pack`() {
        digSiteCompleted()
        val f = Fixture()
        f.player.inv[0] = InvObj(TROWEL, 1)
        f.talkTo(EXAMINER)
        f.finish(listOf(2))
        assertEquals(1, f.player.inv.count(TROWEL))
        assertEquals(0, f.groundCount(TROWEL))
    }

    @Test fun `Examiner gives no trowel before The Dig Site`() {
        val f = Fixture()
        f.talkTo(EXAMINER)
        f.finish(listOf(2))
        assertEquals(0, f.player.inv.count(TROWEL))
        assertEquals(0, f.groundCount(TROWEL))
    }

    private fun digSiteCompleted() {
        QuestRequirements.install(
            QuestRequirementPolicy(
                QuestRequirementMode.VirtualCompletions,
                virtualCompletions = setOf("quest_digsite"),
            )
        )
    }

    @Test fun `Brother Jered blesses one unblessed symbol`() {
        val f = Fixture()
        f.player.inv[0] = InvObj(UNBLESSED_SYMBOL, 1)
        f.player.inv[1] = InvObj(UNBLESSED_SYMBOL, 1)
        f.talkTo(JERED)
        f.finish(listOf(1, 1))
        assertEquals(1, f.player.inv.count(UNBLESSED_SYMBOL))
        assertEquals(1, f.player.inv.count(HOLY_SYMBOL))
    }

    @Test fun `Brother Jered blesses a symbol in a full pack`() {
        val f = Fixture()
        f.fillInventory()
        f.player.inv[27] = InvObj(UNBLESSED_SYMBOL, 1)
        f.talkTo(JERED)
        f.finish(listOf(1, 1))
        assertEquals(0, f.player.inv.count(UNBLESSED_SYMBOL))
        assertEquals(1, f.player.inv.count(HOLY_SYMBOL))
        assertEquals(27, f.player.inv.count("obj.bronze_dagger"))
    }

    @Test fun `declining Brother Jered keeps the unblessed symbol`() {
        val f = Fixture()
        f.player.inv[0] = InvObj(UNBLESSED_SYMBOL, 1)
        f.talkTo(JERED)
        f.finish(listOf(1, 3))
        assertEquals(1, f.player.inv.count(UNBLESSED_SYMBOL))
        assertEquals(0, f.player.inv.count(HOLY_SYMBOL))
    }

    @Test fun `Brother Jered gives nothing without an unblessed symbol`() {
        val f = Fixture()
        f.talkTo(JERED)
        f.finish(listOf(1, 1))
        assertEquals(0, f.player.inv.count(HOLY_SYMBOL))
    }

    private class Fixture {
        private val events = EventBus()
        private val client = RecordingClient()
        private val coroutine = GameCoroutine("outskirts-interaction-test")
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
                uuid = 796L
                observerUUID = 796L
                slotId = 1
                assignUid()
                coords = CoordGrid(3046, 3488, 1)
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
            with(EdgevilleMonastery()) { scripts.startup() }
            with(DigsiteNpcs(objRepo)) { scripts.startup() }
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
        private const val JERED = "npc.brother_jered"
        private const val EXAMINER = "npc.examiner"
        private const val TROWEL = "obj.trowel"
        private const val UNBLESSED_SYMBOL = "obj.stringstar"
        private const val HOLY_SYMBOL = "obj.blessedstar"
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
