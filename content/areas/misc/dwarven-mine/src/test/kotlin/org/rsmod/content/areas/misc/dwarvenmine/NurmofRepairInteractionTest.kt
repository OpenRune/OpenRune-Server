package org.rsmod.content.areas.misc.dwarvenmine

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
import org.rsmod.api.shops.Shops
import org.rsmod.content.areas.misc.dwarvenmine.npcs.DwarvenMineShopkeepersScript
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
class NurmofRepairInteractionTest {
    @Test fun `repairing in a full pack swaps the broken pickaxe for a fixed one`() {
        val f = Fixture()
        f.player.inv[0] = InvObj(BROKEN, 1)
        f.player.inv[1] = InvObj(COINS, COST + 7)
        for (slot in 2 until 28) f.player.inv[slot] = InvObj("obj.bronze_dagger", 1)
        f.talkToNurmof()
        f.finish(listOf(1))
        assertEquals(0, f.player.inv.count(BROKEN))
        assertEquals(1, f.player.inv.count(FIXED))
        assertEquals(7, f.player.inv.count(COINS))
        assertEquals(26, f.player.inv.count("obj.bronze_dagger"))
    }

    @Test fun `accepting after the price complaint also repairs`() {
        val f = Fixture()
        f.player.inv[0] = InvObj(BROKEN, 1)
        f.player.inv[1] = InvObj(COINS, COST)
        f.talkToNurmof()
        f.finish(listOf(2, 1))
        assertEquals(0, f.player.inv.count(BROKEN))
        assertEquals(1, f.player.inv.count(FIXED))
        assertEquals(0, f.player.inv.count(COINS))
    }

    @Test fun `too few coins keeps the pickaxe and the coins`() {
        val f = Fixture()
        f.player.inv[0] = InvObj(BROKEN, 1)
        f.player.inv[1] = InvObj(COINS, COST - 1)
        f.talkToNurmof()
        f.finish(listOf(1))
        assertEquals(1, f.player.inv.count(BROKEN))
        assertEquals(0, f.player.inv.count(FIXED))
        assertEquals(COST - 1, f.player.inv.count(COINS))
    }

    @Test fun `declining keeps the pickaxe and the coins`() {
        val f = Fixture()
        f.player.inv[0] = InvObj(BROKEN, 1)
        f.player.inv[1] = InvObj(COINS, COST)
        f.talkToNurmof()
        f.finish(listOf(2, 2))
        assertEquals(1, f.player.inv.count(BROKEN))
        assertEquals(0, f.player.inv.count(FIXED))
        assertEquals(COST, f.player.inv.count(COINS))
    }

    private class Fixture {
        private val events = EventBus()
        private val client = RecordingClient()
        private val coroutine = GameCoroutine("nurmof-test")
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
                uuid = 796L
                observerUUID = 796L
                slotId = 1
                assignUid()
                coords = CoordGrid(3037, 9787, 0)
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
            with(DwarvenMineShopkeepersScript(Shops(events))) { scripts.startup() }
        }

        fun talkToNurmof() {
            player.clearPendingAction(events)
            result = null
            player.activeCoroutine = coroutine
            val nurmof = Npc(NURMOF, player.coords.translateZ(1))
            val body: suspend () -> Unit = {
                val access = ProtectedAccess(player, coroutine, context)
                assertTrue(events.publish(access, NpcEvents.Op1(nurmof)))
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
            fail<Unit>("Interaction did not finish: ${output()}")
        }

        private fun advance(options: Iterator<Int>) {
            if (coroutine.isAwaiting(ResumePauseButtonInput::class)) {
                val input =
                    when {
                        player.ui.containsModal("interface.chatmenu") ->
                            ResumePauseButtonInput(
                                "component.chatmenu:options",
                                if (options.hasNext()) options.next() else 1,
                            )
                        player.ui.containsModal("interface.objectbox") ->
                            ResumePauseButtonInput("component.objectbox:universe", -1)
                        else -> {
                            val parent =
                                listOf("chat_left", "chat_right", "messagebox").firstOrNull {
                                    player.ui.containsModal("interface.$it")
                                } ?: error("Unknown dialogue: ${output()}")
                            ResumePauseButtonInput("component.$parent:continue", -1)
                        }
                    }
                coroutine.resumeWith(input)
            } else {
                player.currentMapClock++
                player.processedMapClock = player.currentMapClock
                coroutine.advance()
            }
            result?.getOrThrow()
        }

        fun output() = client.messages.joinToString("\n")
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
        private const val NURMOF = "npc.nurmof"
        private const val COINS = "obj.coins"
        private const val BROKEN = "obj.wbr_dragon_pickaxe_broken"
        private const val FIXED = "obj.dragon_pickaxe"
        private const val COST = 2_500_000
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
