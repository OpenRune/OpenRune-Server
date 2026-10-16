package org.rsmod.content.bosses.brutus

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import kotlin.coroutines.Continuation
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.coroutines.startCoroutine
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.junit.jupiter.api.parallel.ResourceLock
import org.rsmod.annotations.InternalApi
import org.rsmod.api.inv.storage.PlayerItemStorage
import org.rsmod.api.invtx.InvTransactionsScript
import org.rsmod.api.obj.charges.ObjChargeManager
import org.rsmod.api.player.dialogue.align.TextAlignment
import org.rsmod.api.player.input.ResumePCountDialogInput
import org.rsmod.api.player.input.ResumePauseButtonInput
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.protect.ProtectedAccessContextFactory
import org.rsmod.content.bosses.brutus.CowbellAmuletCharging.Companion.AIR_RUNE
import org.rsmod.content.bosses.brutus.CowbellAmuletCharging.Companion.CHARGED
import org.rsmod.content.bosses.brutus.CowbellAmuletCharging.Companion.CHARGES
import org.rsmod.content.bosses.brutus.CowbellAmuletCharging.Companion.EMPTY
import org.rsmod.coroutine.GameCoroutine
import org.rsmod.events.EventBus
import org.rsmod.game.cheat.CheatCommandMap
import org.rsmod.game.client.Client
import org.rsmod.game.entity.Player
import org.rsmod.game.inv.InvObj
import org.rsmod.game.inv.InvVirtualStorageHolder
import org.rsmod.game.inv.Inventory
import org.rsmod.game.inv.isType
import org.rsmod.game.queue.EngineQueueCache
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.ScriptContext

@Execution(ExecutionMode.SAME_THREAD)
@ResourceLock("ServerCacheManager")
class CowbellAmuletChargingTest {
    @Test
    fun `charging takes exactly the chosen runes and charges the amulet in place`() {
        val f = Fixture()
        f.player.inv[3] = InvObj(EMPTY)
        f.player.inv[7] = InvObj(AIR_RUNE, 25)
        f.run { with(f.script) { charge(3) } }
        f.finish(count = 10)
        assertEquals(15, f.player.inv.count(AIR_RUNE))
        assertTrue(f.player.inv[3].isType(CHARGED))
        assertEquals(10, f.charges(3))
    }

    @Test
    fun `a request above the runes held is capped at the runes held`() {
        val f = Fixture()
        f.player.inv[0] = InvObj(EMPTY)
        f.player.inv[1] = InvObj(AIR_RUNE, 4)
        f.run { with(f.script) { charge(0) } }
        f.finish(count = Int.MAX_VALUE)
        assertEquals(0, f.player.inv.count(AIR_RUNE))
        assertEquals(4, f.charges(0))
    }

    @Test
    fun `charges stop at a thousand and the rest of the runes stay`() {
        val f = Fixture()
        f.player.inv[0] = f.charged(995)
        f.player.inv[1] = InvObj(AIR_RUNE, 50)
        f.run { with(f.script) { charge(0) } }
        f.finish(count = 50)
        assertEquals(1_000, f.charges(0))
        assertEquals(45, f.player.inv.count(AIR_RUNE))
    }

    @Test
    fun `a full amulet takes no runes`() {
        val f = Fixture()
        f.player.inv[0] = f.charged(1_000)
        f.player.inv[1] = InvObj(AIR_RUNE, 50)
        f.run { with(f.script) { charge(0) } }
        f.finish()
        assertEquals(1_000, f.charges(0))
        assertEquals(50, f.player.inv.count(AIR_RUNE))
    }

    @Test
    fun `runes that vanish during the prompt charge nothing`() {
        val f = Fixture()
        f.player.inv[0] = InvObj(EMPTY)
        f.player.inv[1] = InvObj(AIR_RUNE, 10)
        f.run { with(f.script) { charge(0) } }
        f.player.inv[1] = InvObj(AIR_RUNE, 3)
        f.finish(count = 10)
        assertEquals(3, f.player.inv.count(AIR_RUNE))
        assertTrue(f.player.inv[0].isType(EMPTY))
    }

    @Test
    fun `an amulet that vanishes during the prompt keeps the runes`() {
        val f = Fixture()
        f.player.inv[0] = InvObj(EMPTY)
        f.player.inv[1] = InvObj(AIR_RUNE, 10)
        f.run { with(f.script) { charge(0) } }
        f.player.inv[0] = null
        f.finish(count = 10)
        assertEquals(10, f.player.inv.count(AIR_RUNE))
    }

    @Test
    fun `uncharging returns every rune and empties the amulet`() {
        val f = Fixture()
        f.player.inv[0] = f.charged(37)
        f.player.inv[1] = InvObj(AIR_RUNE, 5)
        f.run { with(f.script) { uncharge(0) } }
        f.finish()
        assertEquals(42, f.player.inv.count(AIR_RUNE))
        assertTrue(f.player.inv[0].isType(EMPTY))
        assertEquals(0, f.charges(0))
    }

    @Test
    fun `uncharging with no room for the runes keeps the charges`() {
        val f = Fixture()
        f.player.inv[0] = f.charged(37)
        for (slot in 1 until 28) f.player.inv[slot] = InvObj("obj.coins", 1)
        f.run { with(f.script) { uncharge(0) } }
        f.finish()
        assertEquals(37, f.charges(0))
        assertEquals(0, f.player.inv.count(AIR_RUNE))
    }

    @Test
    fun `uncharging a full rune stack keeps the charges`() {
        val f = Fixture()
        f.player.inv[0] = f.charged(37)
        f.player.inv[1] = InvObj(AIR_RUNE, Int.MAX_VALUE)
        f.run { with(f.script) { uncharge(0) } }
        f.finish()
        assertEquals(37, f.charges(0))
        assertEquals(Int.MAX_VALUE, f.player.inv.count(AIR_RUNE))
    }

    @Test
    fun `a teleport uses one charge and the last one empties the amulet`() {
        val f = Fixture()
        val amulet = f.charged(2)
        val once = CowbellAmuletCharging.afterTeleport(amulet, 1)
        assertTrue(once.isType(CHARGED))
        assertEquals(1, ObjChargeManager().getCharges(once, CHARGES))
        val empty = CowbellAmuletCharging.afterTeleport(once, 0)
        assertTrue(empty.isType(EMPTY))
        assertEquals(0, empty.vars)
    }

    private class Fixture {
        private val client = RecordingClient()
        private val coroutine = GameCoroutine("cowbell-amulet-test")
        private var result: Result<Unit>? = null
        private val context =
            ProtectedAccessContextFactory.empty().copy(
                getEventBus = { EventBus() },
                getAlignment = { TextAlignment() },
            )
        val script = CowbellAmuletCharging(ObjChargeManager())

        @OptIn(InternalApi::class)
        val player =
            Player().apply {
                this.client = this@Fixture.client
                uuid = 792L
                slotId = 1
                assignUid()
                coords = CoordGrid(3259, 3276, 0)
                currentMapClock = 100
                processedMapClock = 100
                inv = Inventory(checkNotNull(ServerCacheManager.getInventory("inv.inv".asRSCM())), arrayOfNulls(28))
                worn = Inventory(checkNotNull(ServerCacheManager.getInventory("inv.worn".asRSCM())), arrayOfNulls(14))
            }

        fun charged(count: Int): InvObj {
            val inv = Inventory(checkNotNull(ServerCacheManager.getInventory("inv.inv".asRSCM())), arrayOfNulls(28))
            inv[0] = InvObj(EMPTY)
            ObjChargeManager().addCharges(inv, 0, count, CHARGES, CowbellAmuletCharging.MAX_CHARGES)
            return checkNotNull(inv[0])
        }

        fun charges(slot: Int): Int = ObjChargeManager().getCharges(player.inv[slot], CHARGES)

        fun run(block: suspend ProtectedAccess.() -> Unit) {
            result = null
            player.activeCoroutine = coroutine
            val access = ProtectedAccess(player, coroutine, context)
            val body: suspend () -> Unit = { access.block() }
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

        fun finish(count: Int = 0) {
            repeat(50) {
                if (coroutine.isIdle) return
                when {
                    coroutine.isAwaiting(ResumePCountDialogInput::class) ->
                        coroutine.resumeWith(ResumePCountDialogInput(count))
                    coroutine.isAwaiting(ResumePauseButtonInput::class) -> {
                        val parent =
                            listOf("messagebox", "objectbox", "objectbox_double").first {
                                player.ui.containsModal("interface.$it")
                            }
                        val input =
                            when (parent) {
                                "objectbox" -> ResumePauseButtonInput("component.objectbox:universe", 0)
                                "objectbox_double" -> ResumePauseButtonInput("component.objectbox_double:pausebutton", -1)
                                else -> ResumePauseButtonInput("component.messagebox:continue", -1)
                            }
                        coroutine.resumeWith(input)
                    }
                    else -> {
                        player.currentMapClock++
                        player.processedMapClock = player.currentMapClock
                        coroutine.advance()
                    }
                }
                result?.getOrThrow()
            }
            fail<Unit>("Interaction did not finish")
        }
    }

    private class RecordingClient : Client<Any, Any> {
        override fun write(message: Any) {}

        override fun close() {}

        override fun read(player: Player) {}

        override fun flush() {}

        override fun flushHighPriority() {}

        override fun unregister(service: Any, player: Player) {}
    }

    companion object {
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
                val field = Class.forName(owner).getDeclaredField(name).apply { isAccessible = true }
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
