package org.rsmod.content.minigames.gauntlet

import dev.openrune.ServerCacheManager
import dev.openrune.types.InvScope
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.ResourceLock
import org.mockito.Mockito.mock
import org.rsmod.api.instances.InstanceManager
import org.rsmod.game.inv.InvObj
import org.rsmod.game.inv.Inventory

@ResourceLock("ServerCacheManager")
class GauntletNativeTransactionsTest {
    @Test fun `native item use finishes Egniol once and only then grants XP`() {
        for (corrupted in listOf(false, true)) {
            val f = GauntletTestFixture()
            f.register(GauntletItems(mock(InstanceManager::class.java), mock(GauntletRuns::class.java)))
            val dust = gauntletObj("crystal_shard_crushed", corrupted)
            f.put(0, dust, 10); f.put(1, "obj.gauntlet_potion_unfinished")
            val xp = f.player.statMap.getFineXP("stat.herblore")
            f.use(0, 1)
            assertNotNull(f.result)
            assertEquals(1, f.player.inv.count("obj.gauntlet_potion_3"))
            assertEquals(0, f.player.inv.count(dust))
            assertTrue(f.player.statMap.getFineXP("stat.herblore") > xp)
        }
    }

    @Test fun `insufficient dust preserves both native inputs and XP`() {
        val f = GauntletTestFixture()
        f.register(GauntletItems(mock(InstanceManager::class.java), mock(GauntletRuns::class.java)))
        f.put(0, "obj.gauntlet_crystal_shard_crushed", 9); f.put(1, "obj.gauntlet_potion_unfinished")
        val before = f.player.inv.objs.copyOf()
        val xp = f.player.statMap.getFineXP("stat.herblore")
        f.use(0, 1)
        assertArrayEquals(before, f.player.inv.objs)
        assertEquals(xp, f.player.statMap.getFineXP("stat.herblore"))
    }

    @Test fun `full inventory rolls back shard consumption when product cannot fit`() {
        val f = GauntletTestFixture()
        repeat(28) { f.put(it, "obj.gauntlet_food") }
        f.put(0, "obj.gauntlet_crystal_shard", 100)
        val before = f.player.inv.objs.copyOf()
        assertFalse(f.player.exchangeGauntlet(listOf("obj.gauntlet_crystal_shard" to 40), "obj.gauntlet_teleport_crystal"))
        assertArrayEquals(before, f.player.inv.objs)
    }

    @Test fun `missing later ingredient rolls back earlier deletes`() {
        val f = GauntletTestFixture()
        f.put(0, "obj.gauntlet_crystal_shard", 100)
        val before = f.player.inv.objs.copyOf()
        assertFalse(f.player.exchangeGauntlet(listOf("obj.gauntlet_crystal_shard" to 20, "obj.gauntlet_generic_component" to 1), "obj.gauntlet_melee_t1"))
        assertArrayEquals(before, f.player.inv.objs)
    }

    @Test fun `escrow restores exact inventory equipment slots and charged item vars once`() {
        val f = GauntletTestFixture()
        f.put(27, "obj.emberlight", vars = 421)
        f.player.worn[3] = InvObj("obj.arclight", vars = 734)
        val inventory = f.player.inv.objs.copyOf(); val worn = f.player.worn.objs.copyOf()
        GauntletHolding.store(f.player)
        assertTrue(f.player.holdingInventory)
        val holding = f.player.invMap.getValue("inv.gauntlet_holding")
        assertEquals(InvScope.Perm, holding.type.scope)
        GauntletHolding.giveStartingKit(f.player, GauntletMode.NORMAL)
        GauntletHolding.restore(f.player)
        assertArrayEquals(inventory, f.player.inv.objs)
        assertArrayEquals(worn, f.player.worn.objs)
        assertFalse(f.player.holdingInventory)
        assertTrue(holding.isEmpty())
        GauntletHolding.restore(f.player)
        assertArrayEquals(inventory, f.player.inv.objs)
    }

    @Test fun `saved permanent escrow survives a new player instance`() {
        val before = GauntletTestFixture()
        before.put(12, "obj.emberlight", vars = 667)
        GauntletHolding.store(before.player)
        val after = GauntletTestFixture()
        after.player.vars.backing.putAll(before.player.vars.backing)
        val saved = before.player.invMap.getValue("inv.gauntlet_holding")
        after.player.invMap["inv.gauntlet_holding"] = Inventory(saved.type, saved.objs.copyOf())
        GauntletHolding.restore(after.player)
        assertEquals(667, after.player.inv[12]?.vars)
        assertFalse(after.player.holdingInventory)
    }

    @Test fun `a second escrow store cannot replace existing equipment`() {
        val f = GauntletTestFixture(); f.put(0, "obj.emberlight", vars = 19)
        GauntletHolding.store(f.player)
        assertThrows(IllegalStateException::class.java) { GauntletHolding.store(f.player) }
        GauntletHolding.restore(f.player)
        assertEquals(19, f.player.inv[0]?.vars)
    }

    companion object { @JvmStatic @BeforeAll fun cache() { ServerCacheManager.init(240).close() } }
}
