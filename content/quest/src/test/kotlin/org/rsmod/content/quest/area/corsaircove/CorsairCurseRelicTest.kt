package org.rsmod.content.quest.area.corsaircove

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.junit.jupiter.api.parallel.ResourceLock
import org.rsmod.annotations.InternalApi
import org.rsmod.api.inv.storage.PlayerItemStorage
import org.rsmod.api.invtx.InvTransactionsScript
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.protect.ProtectedAccessContextFactory
import org.rsmod.coroutine.GameCoroutine
import org.rsmod.events.EventBus
import org.rsmod.game.cheat.CheatCommandMap
import org.rsmod.game.client.Client
import org.rsmod.game.entity.Player
import org.rsmod.game.inv.InvObj
import org.rsmod.game.inv.InvVirtualStorageHolder
import org.rsmod.game.inv.Inventory
import org.rsmod.game.queue.EngineQueueCache
import org.rsmod.plugin.scripts.ScriptContext

@Execution(ExecutionMode.SAME_THREAD)
@ResourceLock("ServerCacheManager")
class CorsairCurseRelicTest {
    @Test
    fun `taking the relic moves exactly one relic and advances the counter`() {
        val f = Fixture()
        assertTrue(f.access.takeRelicFromTock())
        assertEquals(1, f.player.inv.count(CorsairCurseRefs.Relic))
        assertEquals(CorsairCurseCounts.ArsenRelicTaken, f.player.vars[ThiefVarbit])
    }

    @Test
    fun `a full inventory does not take the relic or advance the counter`() {
        val f = Fixture(fullInventory = true)
        val before = f.player.inv.objs.toList()
        assertFalse(f.access.takeRelicFromTock())
        assertEquals(before, f.player.inv.objs.toList())
        assertEquals(0, f.player.vars[ThiefVarbit])
    }

    @Test
    fun `returning the relic removes exactly one and advances the counter`() {
        val f = Fixture()
        f.player.inv[0] = InvObj(CorsairCurseRefs.Relic, 1)
        assertTrue(f.access.returnRelicToTess())
        assertEquals(0, f.player.inv.count(CorsairCurseRefs.Relic))
        assertEquals(CorsairCurseCounts.ArsenRelicReturned, f.player.vars[ThiefVarbit])
    }

    @Test
    fun `returning without a relic changes nothing`() {
        val f = Fixture()
        val before = f.player.inv.objs.toList()
        assertFalse(f.access.returnRelicToTess())
        assertEquals(before, f.player.inv.objs.toList())
        assertEquals(0, f.player.vars[ThiefVarbit])
    }

    @Test
    fun `taking and returning the relic accounts for every item`() {
        val f = Fixture()
        assertTrue(f.access.takeRelicFromTock())
        assertEquals(1, f.player.inv.count(CorsairCurseRefs.Relic))
        assertTrue(f.access.returnRelicToTess())
        assertEquals(0, f.player.inv.count(CorsairCurseRefs.Relic))
        assertEquals(CorsairCurseCounts.ArsenRelicReturned, f.player.vars[ThiefVarbit])
    }

    private class Fixture(fullInventory: Boolean = false) {
        private val client = RecordingClient()

        @OptIn(InternalApi::class)
        val player = Player().apply {
            this.client = client
            uuid = 321L
            slotId = 1
            assignUid()
            currentMapClock = 100
            processedMapClock = 100
            inv = Inventory(checkNotNull(ServerCacheManager.getInventory("inv.inv".asRSCM())), arrayOfNulls(28))
            worn = Inventory(checkNotNull(ServerCacheManager.getInventory("inv.worn".asRSCM())), arrayOfNulls(14))
        }

        val access = ProtectedAccess(
            player,
            GameCoroutine("corsair-relic-test"),
            ProtectedAccessContextFactory.empty().copy(getEventBus = { EventBus() }),
        )

        init {
            if (fullInventory) {
                for (slot in 0 until 28) player.inv[slot] = InvObj("obj.coins", 1)
            }
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

    private companion object {
        private const val ThiefVarbit = "varbit.corscurs_thief"

        private val restored = mutableListOf<() -> Unit>()

        @OptIn(InternalApi::class)
        @JvmStatic
        @BeforeAll
        fun cache() {
            ServerCacheManager.init(241).close()
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
