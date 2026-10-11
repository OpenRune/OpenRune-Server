package org.rsmod.api.invtx

import dev.openrune.ServerCacheManager
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.parallel.ResourceLock
import org.rsmod.api.inv.storage.*
import org.rsmod.events.EventBus
import org.rsmod.game.cheat.CheatCommandMap
import org.rsmod.game.entity.Player
import org.rsmod.game.inv.InvObj
import org.rsmod.game.inv.Inventory
import org.rsmod.game.queue.EngineQueueCache
import org.rsmod.plugin.scripts.ScriptContext

@ResourceLock("ServerCacheManager")
internal class VirtualStorageTransactionTest {
    @BeforeEach fun cache() { ServerCacheManager.init(240).close() }

    @Test fun `full inventory and partial redirect failure preserve both stores`() {
        val f = Fixture(35)
        f.player.inv.indices.forEach { f.player.inv[it] = InvObj("obj.lobster") }
        assertTrue(f.player.invAdd(f.player.inv, "obj.coal", 2).failure)
        assertEquals(35, f.hook.stored)
        assertEquals(28, f.player.inv.occupiedSpace())
        val dry = f.player.invAdd(f.player.inv, "obj.coal", 1, autoCommit = false)
        assertTrue(dry.success); assertEquals(35, f.hook.stored)
        dry.commitAll(); assertEquals(36, f.hook.stored)
    }

    @Test fun `dry run combining storage and inventory changes neither until commit`() {
        val f = Fixture(35)
        val dry = f.player.invAdd(f.player.inv, "obj.coal", 2, autoCommit = false)
        assertTrue(dry.success); assertEquals(35, f.hook.stored); assertEquals(0, f.player.inv.physicalCount("obj.coal"))
        dry.commitAll(); assertEquals(36, f.hook.stored); assertEquals(1, f.player.inv.physicalCount("obj.coal"))
    }

    @Test fun `mixed removal commits together and non-strict shortage consumes available total`() {
        val f = Fixture(4)
        f.player.inv.indices.take(6).forEach { f.player.inv[it] = InvObj("obj.coal") }
        val dry = f.player.invDel(f.player.inv, "obj.coal", 100, strict = false, autoCommit = false)
        assertTrue(dry.success); assertEquals(4, f.hook.stored); assertEquals(6, f.player.inv.physicalCount("obj.coal"))
        dry.commitAll(); assertEquals(0, f.hook.stored); assertEquals(0, f.player.inv.physicalCount("obj.coal")); assertEquals(10, dry.completed())
    }

    @Test fun `changed storage rejects stale commit before physical mutation`() {
        val f = Fixture(35)
        val dry = f.player.invAdd(f.player.inv, "obj.coal", 2, autoCommit = false)
        f.hook.stored = 36
        assertThrows(IllegalStateException::class.java) { dry.commitAll() }
        assertEquals(0, f.player.inv.physicalCount("obj.coal")); assertEquals(36, f.hook.stored)
    }

    @Test fun `ignored storage takes the normal physical route`() {
        val f = Fixture(35)
        assertTrue(f.player.invAdd(f.player.inv, "obj.coal", 2, ignoreVirtualStorage = true).success)
        assertEquals(35, f.hook.stored); assertEquals(2, f.player.inv.physicalCount("obj.coal"))
    }

    @Test fun `noted products and invalid counts never enter virtual storage`() {
        val f = Fixture(0)
        assertTrue(f.player.invAdd(f.player.inv, "obj.coal", 2, cert = true).success)
        assertEquals(0, f.hook.stored)
        assertEquals(1, f.player.inv.occupiedSpace())
        val snapshot = f.player.inv.objs.copyOf()
        for (count in listOf(0, -1)) {
            f.player.invAdd(f.player.inv, "obj.coal", count)
            f.player.invDel(f.player.inv, "obj.coal", count)
            assertEquals(0, f.hook.stored)
            assertArrayEquals(snapshot, f.player.inv.objs)
        }
    }
    private class Fixture(count: Int) {
        val player = Player().apply { inv = Inventory.create("inv.inv") }
        val hook = Coal(count)
        init { with(InvTransactionsScript(PlayerItemStorage(setOf(hook)))) { ScriptContext(EventBus(), CheatCommandMap(), EngineQueueCache()).startup() } }
    }
    private class Coal(var stored: Int) : PlayerItemStorageHook {
        override fun shouldProcess(ctx: PlayerItemStorageContext) = ctx.inventory === ctx.player.inv && ctx.itemInternal == "obj.coal"
        override fun contains(ctx: PlayerItemStorageContext) = stored
        override fun incomingCapacity(ctx: PlayerItemStorageContext) = 36 - stored
        override fun remove(ctx: PlayerItemStorageContext, amount: Int): Int { val n = minOf(stored, amount); stored -= n; return n }
        override fun add(ctx: PlayerItemStorageContext, amount: Int): Int { val n = minOf(36 - stored, amount); stored += n; return n }
    }
}
