package org.rsmod.content.areas.revenants

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.util.Wearpos
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.parallel.ResourceLock
import org.mockito.Mockito.mock
import org.rsmod.api.inv.storage.PlayerItemStorage
import org.rsmod.api.invtx.InvTransactionsScript
import org.rsmod.events.EventBus
import org.rsmod.game.cheat.CheatCommandMap
import org.rsmod.game.entity.Player
import org.rsmod.game.inv.InvObj
import org.rsmod.game.inv.Inventory
import org.rsmod.game.queue.EngineQueueCache
import org.rsmod.plugin.scripts.ScriptContext

@ResourceLock("ServerCacheManager")
internal class RevenantItemsTest {
    @Test fun `all charged forms round trip exact ether and preserve absorption`() {
        for (kind in RevenantCharges.kinds) {
            val p = player(); val item = InvObj(kind.empty); p.inv[0] = item; p.inv[1] = InvObj(RevenantCharges.ETHER, kind.max)
            assertTrue(RevenantCharges.transfer(p, p.inv, 0, item, kind.max, false), kind.empty)
            val charged = p.inv[0]!!; assertEquals(kind.max, RevenantCharges.count(charged.copy()))
            assertEquals(kind.charged.asRSCM(), charged.id); Assertions.assertNull(p.inv[1])
            assertTrue(RevenantCharges.transfer(p, p.inv, 0, charged, kind.max, true))
            assertEquals(item, p.inv[0]); assertEquals(kind.max, p.inv.physicalCount(RevenantCharges.ETHER))
        }
        val toggled = RevenantCharges.toggle(InvObj(RevenantCharges.EMPTY_BRACELET))
        assertTrue(RevenantCharges.absorbing(RevenantCharges.withCount(toggled, 1)))
        assertTrue(RevenantCharges.absorbing(RevenantCharges.withCount(toggled, 0)))
    }

    @Test fun `stale item and insufficient materials cannot spend ether`() {
        val p = player(); val original = InvObj(RevenantCharges.weapons[0].empty); p.inv[0] = original.copy(); p.inv[1] = InvObj(RevenantCharges.ETHER, 20)
        assertFalse(RevenantCharges.transfer(p, p.inv, 0, original, 20, false))
        val replacement = p.inv[0]!!
        assertFalse(RevenantCharges.transfer(p, p.inv, 0, replacement, 21, false))
        assertFalse(RevenantCharges.transfer(p, p.inv, 0, replacement, -1, false))
        assertSame(replacement, p.inv[0]); assertEquals(20, p.inv.physicalCount(RevenantCharges.ETHER))
    }

    @Test fun `full inventory refund fails atomically`() {
        val p = player(); val charged = RevenantCharges.withCount(InvObj(RevenantCharges.weapons[0].empty), 1001); p.inv[0] = charged
        for (slot in 1..27) p.inv[slot] = InvObj("obj.dragon_dagger")
        assertFalse(RevenantCharges.transfer(p, p.inv, 0, charged, 1001, true)); assertSame(charged, p.inv[0]); assertEquals(0, p.inv.physicalCount(RevenantCharges.ETHER))
    }

    @Test fun `bracelet consumes one even on zero damage and stops protection when empty`() {
        val p = player(); val slot = Wearpos.Hands.slot; p.worn[slot] = RevenantCharges.withCount(InvObj(RevenantCharges.EMPTY_BRACELET), 2)
        assertTrue(RevenantCharges.protected(p)); assertEquals(7, RevenantCharges.reduceDamage(p, 31)); assertEquals(1, RevenantCharges.count(p.worn[slot]!!))
        assertEquals(0, RevenantCharges.reduceDamage(p, 0)); assertFalse(RevenantCharges.protected(p)); assertEquals(31, RevenantCharges.reduceDamage(p, 31))
    }

    @Test fun `absorption keeps excess ground ether and respects the toggle`() {
        val p = player(); val slot = Wearpos.Hands.slot; p.worn[slot] = RevenantCharges.withCount(InvObj(RevenantCharges.EMPTY_BRACELET), 15998)
        assertEquals(10, RevenantCharges.absorb(p, 10))
        p.worn[slot] = RevenantCharges.toggle(p.worn[slot]!!)
        assertEquals(8, RevenantCharges.absorb(p, 10)); assertEquals(16000, RevenantCharges.count(p.worn[slot]!!))
        assertEquals(10, RevenantCharges.absorb(p, 10))
    }

    @Test fun `admission combines inventory and bank only once`() {
        val p = player(); val bank = Inventory(ServerCacheManager.getInventory("inv.bank".asRSCM())!!, arrayOfNulls(1000))
        p.inv[0] = InvObj("obj.coins", 40000); bank[0] = InvObj("obj.coins", 60000)
        assertTrue(RevenantAccessScript.pay(p, bank)); assertEquals(0, p.inv.physicalCount("obj.coins")); assertEquals(0, bank.physicalCount("obj.coins"))
        assertTrue(RevenantAccessScript.pay(p, bank)); assertEquals(1, p.revenantAdmission and 1)
        val poor = player(); poor.inv[0] = InvObj("obj.coins", 99999)
        assertFalse(RevenantAccessScript.pay(poor, bank)); assertEquals(99999, poor.inv.physicalCount("obj.coins")); assertEquals(0, poor.revenantAdmission)
    }

    @Test fun `all six artefacts and notes exchange for correct coins and cannot replay`() {
        val p = player()
        AncientArtefact.entries.forEachIndexed { slot, a -> p.inv[slot] = InvObj(a.item) }
        p.inv[6] = InvObj("obj.cert_wild_cave_artifact_500", 2)
        val snapshot = RevenantTraderScript.carried(p)
        assertEquals(32_500_000L, RevenantTraderScript.total(snapshot))
        assertTrue(RevenantTraderScript.exchange(p, snapshot)); assertEquals(32500000, p.inv.physicalCount("obj.coins")); assertTrue(RevenantTraderScript.carried(p).isEmpty())
        assertFalse(RevenantTraderScript.exchange(p, snapshot)); assertEquals(32500000, p.inv.physicalCount("obj.coins"))
    }

    @Test fun `coin overflow preserves the artefacts`() {
        val p = player(); val item = InvObj("obj.cert_wild_cave_artifact_16000", 200); p.inv[0] = item
        assertFalse(RevenantTraderScript.exchange(p, RevenantTraderScript.carried(p))); assertSame(item, p.inv[0]); assertEquals(0, p.inv.physicalCount("obj.coins"))
    }
    private fun player() = Player().apply {
        inv = Inventory(ServerCacheManager.getInventory("inv.inv".asRSCM())!!, arrayOfNulls(28))
        worn = Inventory(ServerCacheManager.getInventory("inv.worn".asRSCM())!!, arrayOfNulls(14))
    }
    companion object { @JvmStatic @BeforeAll fun setup() {
        ServerCacheManager.init(240).close()
        with(InvTransactionsScript(mock(PlayerItemStorage::class.java))) { ScriptContext(EventBus(), CheatCommandMap(), EngineQueueCache()).startup() }
    } }
}
