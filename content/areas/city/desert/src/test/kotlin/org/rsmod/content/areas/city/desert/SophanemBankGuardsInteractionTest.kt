package org.rsmod.content.areas.city.desert

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
import org.rsmod.content.areas.city.desert.sophanem.npcs.SophanemBankGuards
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
class SophanemBankGuardsInteractionTest {
    @Test fun `the guard sells a tinderbox for 50 coins`() {
        val f = Fixture()
        f.player.inv[0] = InvObj(COINS, 120)
        f.talkTo(GUARD)
        f.finish(listOf(2, 1))
        assertEquals(70, f.player.inv.count(COINS))
        assertEquals(1, f.player.inv.count(TINDERBOX))
    }

    @Test fun `the guard keeps the tinderbox from a player short of coins`() {
        val f = Fixture()
        f.player.inv[0] = InvObj(COINS, 49)
        f.talkTo(GUARD_F)
        f.finish(listOf(2, 1))
        assertEquals(49, f.player.inv.count(COINS))
        assertEquals(0, f.player.inv.count(TINDERBOX))
    }

    @Test fun `declining the tinderbox costs nothing`() {
        val f = Fixture()
        f.player.inv[0] = InvObj(COINS, 50)
        f.talkTo(GUARD)
        f.finish(listOf(2, 2))
        assertEquals(50, f.player.inv.count(COINS))
        assertEquals(0, f.player.inv.count(TINDERBOX))
    }

    @Test fun `a player with a tinderbox is not sold another`() {
        val f = Fixture()
        f.player.inv[0] = InvObj(COINS, 50)
        f.player.inv[1] = InvObj(TINDERBOX, 1)
        f.talkTo(GUARD)
        f.finish(listOf(2))
        assertEquals(50, f.player.inv.count(COINS))
        assertEquals(1, f.player.inv.count(TINDERBOX))
    }

    @Test fun `the guard sells an unlit torch for 200 coins`() {
        val f = Fixture()
        f.player.inv[0] = InvObj(COINS, 250)
        f.talkTo(GUARD_F)
        f.finish(listOf(3, 1))
        assertEquals(50, f.player.inv.count(COINS))
        assertEquals(1, f.player.inv.count(TORCH))
    }

    @Test fun `the guard keeps the torch from a player short of coins`() {
        val f = Fixture()
        f.player.inv[0] = InvObj(COINS, 199)
        f.talkTo(GUARD)
        f.finish(listOf(3, 1))
        assertEquals(199, f.player.inv.count(COINS))
        assertEquals(0, f.player.inv.count(TORCH))
    }

    @Test fun `spending the last coins frees a slot for the torch`() {
        val f = Fixture()
        f.fillInventory()
        f.player.inv[0] = InvObj(COINS, 200)
        f.talkTo(GUARD)
        f.finish(listOf(3, 1))
        assertEquals(0, f.player.inv.count(COINS))
        assertEquals(1, f.player.inv.count(TORCH))
        assertEquals(0, f.groundCount(TORCH))
    }

    @Test fun `a full pack drops the torch instead of losing it`() {
        val f = Fixture()
        f.fillInventory()
        f.player.inv[0] = InvObj(COINS, 210)
        f.talkTo(GUARD)
        f.finish(listOf(3, 1))
        assertEquals(10, f.player.inv.count(COINS))
        assertEquals(0, f.player.inv.count(TORCH))
        assertEquals(1, f.groundCount(TORCH))
    }

    private class Fixture {
        private val events = EventBus()
        private val client = RecordingClient()
        private val coroutine = GameCoroutine("sophanem-guard-test")
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
                invMap["inv.inv"] = inv
                invMap["inv.worn"] = worn
            }

        init {
            val scripts = ScriptContext(events, CheatCommandMap(), EngineQueueCache())
            val objRepo = ObjRepository(clock, objRegistry)
            with(SophanemBankGuards(objRepo)) { scripts.startup() }
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
        private const val COINS = "obj.coins"
        private const val TINDERBOX = "obj.tinderbox"
        private const val TORCH = "obj.torch_unlit"
        private const val GUARD = "npc.contact_sophanem_guard_male_01"
        private const val GUARD_F = "npc.contact_sophanem_guard_female_01"
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
