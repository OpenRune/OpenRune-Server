package org.rsmod.content.areas.city.edgeville

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
import org.rsmod.api.shops.Shops
import org.rsmod.content.areas.city.edgeville.npcs.EdgevilleTownsfolk
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
class OziachInteractionTest {
    private val previousPolicy = QuestRequirements.activePolicy()

    @AfterEach
    fun restorePolicy() {
        QuestRequirements.install(previousPolicy)
    }

    @Test fun `Oziach forges a dragonfire shield for 1,250,000 coins`() {
        val f = Fixture()
        f.player.inv[0] = InvObj(COINS, 1_300_000)
        f.player.inv[1] = InvObj(VISAGE, 1)
        f.player.inv[2] = InvObj(ANTI_DRAGON_SHIELD, 1)
        f.talkTo(OZIACH)
        f.finish(listOf(5, 1))
        assertEquals(50_000, f.player.inv.count(COINS))
        assertEquals(0, f.player.inv.count(VISAGE))
        assertEquals(0, f.player.inv.count(ANTI_DRAGON_SHIELD))
        assertEquals(1, f.player.inv.count(DRAGONFIRE_SHIELD))
    }

    @Test fun `Oziach keeps everything when the player is short of coins`() {
        val f = Fixture()
        f.player.inv[0] = InvObj(COINS, 1_249_999)
        f.player.inv[1] = InvObj(VISAGE, 1)
        f.player.inv[2] = InvObj(ANTI_DRAGON_SHIELD, 1)
        f.talkTo(OZIACH)
        f.finish(listOf(5, 1))
        assertEquals(1_249_999, f.player.inv.count(COINS))
        assertEquals(1, f.player.inv.count(VISAGE))
        assertEquals(1, f.player.inv.count(ANTI_DRAGON_SHIELD))
        assertEquals(0, f.player.inv.count(DRAGONFIRE_SHIELD))
    }

    @Test fun `declining Oziach's offer costs nothing`() {
        val f = Fixture()
        f.player.inv[0] = InvObj(COINS, 1_250_000)
        f.player.inv[1] = InvObj(VISAGE, 1)
        f.player.inv[2] = InvObj(ANTI_DRAGON_SHIELD, 1)
        f.talkTo(OZIACH)
        f.finish(listOf(5, 2))
        assertEquals(1_250_000, f.player.inv.count(COINS))
        assertEquals(1, f.player.inv.count(VISAGE))
        assertEquals(0, f.player.inv.count(DRAGONFIRE_SHIELD))
    }

    @Test fun `Oziach offers no forging before Dragon Slayer I`() {
        QuestRequirements.install(QuestRequirementPolicy(QuestRequirementMode.RespectProgress))
        val f = Fixture()
        f.player.inv[0] = InvObj(COINS, 1_250_000)
        f.player.inv[1] = InvObj(VISAGE, 1)
        f.player.inv[2] = InvObj(ANTI_DRAGON_SHIELD, 1)
        f.talkTo(OZIACH)
        f.finish(listOf(2))
        assertEquals(1_250_000, f.player.inv.count(COINS))
        assertEquals(0, f.player.inv.count(DRAGONFIRE_SHIELD))
    }

    private class Fixture {
        private val events = EventBus()
        private val client = RecordingClient()
        private val coroutine = GameCoroutine("oziach-test")
        private var result: Result<Unit>? = null
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
                coords = CoordGrid(3069, 3517, 0)
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
            if (QuestRequirements.activePolicy().mode != QuestRequirementMode.RespectProgress) {
                QuestRequirements.install(
                    QuestRequirementPolicy(QuestRequirementMode.AssumeCompleted)
                )
            }
            val scripts = ScriptContext(events, CheatCommandMap(), EngineQueueCache())
            with(EdgevilleTownsfolk(Shops(events))) { scripts.startup() }
        }

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
        private const val OZIACH = "npc.oziach"
        private const val COINS = "obj.coins"
        private const val VISAGE = "obj.dragonfire_visage"
        private const val ANTI_DRAGON_SHIELD = "obj.antidragonbreathshield"
        private const val DRAGONFIRE_SHIELD = "obj.dragonfire_shield_uncharged"
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
