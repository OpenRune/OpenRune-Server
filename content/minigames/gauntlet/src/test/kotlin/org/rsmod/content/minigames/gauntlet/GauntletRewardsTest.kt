package org.rsmod.content.minigames.gauntlet

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import dtx.rs.RSDropTable
import dtx.rs.locs
import kotlin.random.Random
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.ResourceLock
import org.mockito.Mockito.*
import org.rsmod.api.droptable.DropRollItem
import org.rsmod.api.droptable.DropTableRegistry
import org.rsmod.api.droptable.rsPlayerGuaranteedTable
import org.rsmod.api.player.vars.VarPlayerIntMapSetter
import org.rsmod.api.random.GameRandom
import org.rsmod.api.repo.obj.ObjRepository
import org.rsmod.content.interfaces.collectionlog.CollectionLog
import org.rsmod.content.minigames.gauntlet.layout.GauntletLayout
import org.rsmod.content.other.pets.PetRewards
import org.rsmod.game.entity.Player
import org.rsmod.game.obj.Obj

@ResourceLock("ServerCacheManager")
class GauntletRewardsTest {
    @Test fun `completion is awarded once and records the correct native mode count`() {
        for (mode in GauntletMode.entries) {
            val f = Fixture()
            val run = run(mode)
            f.rewards.complete(f.player, run)
            f.rewards.complete(f.player, run)
            assertTrue(f.player.gauntletRewardAvailable)
            assertTrue(f.player.gauntletRewardVisible)
            assertEquals(if (mode.corrupted) RewardTier.CORRUPTED.ordinal else RewardTier.NORMAL.ordinal, f.player.gauntletRewardTier)
            val suffix = if (mode.corrupted) "_hm" else ""
            assertEquals(1, f.player.vars["varp.total_completed_gauntlet$suffix"])
        }
    }

    @Test fun `preparation alone cannot claim boss loot and zero misses do not improve the tier`() {
        val f = Fixture()
        f.player.gauntletPoints = 999
        val run = run()
        f.rewards.settleByDamage(f.player, run)
        assertFalse(f.player.gauntletRewardAvailable)
        run.bossDamage = 1
        f.rewards.settleByDamage(f.player, run)
        assertEquals(RewardTier.JUNK.ordinal, f.player.gauntletRewardTier)
        run.bossDamage = 50
        f.rewards.settleByDamage(f.player, run)
        assertEquals(RewardTier.INCOMPLETE.ordinal, f.player.gauntletRewardTier)
    }

    @Test fun `pending corrupted chest survives native permanent var save load and awards once`() {
        val f = Fixture()
        f.rewards.complete(f.player, run(GauntletMode.CORRUPTED))
        val savedTier = f.player.gauntletRewardTier
        val savedFlags = f.player.vars["varp.gauntlet_flags"]
        val loaded = Fixture()
        loaded.player.gauntletRewardTier = savedTier
        VarPlayerIntMapSetter.set(loaded.player, "varp.gauntlet_flags", savedFlags)
        loaded.claim()
        loaded.claim()
        assertEquals(1, loaded.player.inv.sumOf { if (it?.id == SEED.asRSCM()) it.count else 0 })
        assertFalse(loaded.player.gauntletRewardAvailable)
        assertFalse(loaded.player.gauntletRewardVisible)
        assertEquals(0, loaded.player.gauntletRewardTier)
        verify(loaded.log).grant(loaded.player, SEED, 1, "Corrupted Gauntlet")
        verifyNoMoreInteractions(loaded.log)
    }

    @Test fun `full inventory preserves the pending chest until space exists`() {
        val f = Fixture()
        f.rewards.complete(f.player, run())
        for (slot in 0 until f.player.inv.size) f.fixture.put(slot, "obj.bronze_dagger")
        f.claim()
        assertTrue(f.player.gauntletRewardAvailable)
        verifyNoInteractions(f.log)
        f.player.inv[0] = null
        f.claim()
        assertFalse(f.player.gauntletRewardAvailable)
        assertEquals(1, f.player.inv.sumOf { if (it?.id == SEED.asRSCM()) it.count else 0 })
    }

    @Test fun `testloot awards and logs real chest drops without changing run escrow chest or count`() {
        val f = Fixture()
        f.fixture.put(0, "obj.coins", 55, 17)
        f.player.inGauntlet = true
        GauntletHolding.store(f.player)
        f.rewards.complete(f.player, run(GauntletMode.CORRUPTED))
        val count = f.player.vars["varp.total_completed_gauntlet_hm"]
        val held = f.player.holdingInventory
        val expected = Obj.fromOwner(f.player, f.player.coords, SEED, 1)
        `when`(f.objects.add(any(Obj::class.java) ?: expected, eq(300), anyInt())).thenReturn(true)
        GauntletTestLoot(f.registry, f.random, f.objects, f.log).generate(f.player, 2, true)
        verify(f.objects, times(2)).add(any(Obj::class.java) ?: expected, eq(300), anyInt())
        verify(f.log, times(2)).grant(f.player, SEED, 1, "Corrupted Gauntlet")
        assertEquals(count, f.player.vars["varp.total_completed_gauntlet_hm"])
        assertTrue(f.player.gauntletRewardAvailable)
        assertTrue(f.player.inGauntlet)
        assertEquals(held, f.player.holdingInventory)
        GauntletHolding.restore(f.player)
        assertEquals(55, f.player.inv[0]!!.count)
        assertEquals(17, f.player.inv[0]!!.vars)
    }

    @Test fun `a failed drop delivery does not falsely grant collection log credit`() {
        val f = Fixture()
        assertThrows(IllegalStateException::class.java) {
            GauntletTestLoot(f.registry, f.random, f.objects, f.log).generate(f.player, 1, false)
        }
        verifyNoInteractions(f.log)
    }

    private class Fixture {
        val fixture = GauntletTestFixture()
        val player get() = fixture.player
        val registry = mock(DropTableRegistry::class.java)
        val objects = mock(ObjRepository::class.java)
        val random = mock(GameRandom::class.java)
        val log = mock(CollectionLog::class.java)
        val rewards = GauntletRewards(objects, mock(PetRewards::class.java), registry, random, log)
        init {
            for (key in listOf(GauntletRewardKeys.NORMAL, GauntletRewardKeys.CORRUPTED)) {
                val table: RSDropTable<Player, DropRollItem> = RSDropTable(
                    tableIdentifier = key, locs = locs(key),
                    guaranteed = rsPlayerGuaranteedTable { SEED count 1 },
                )
                `when`(registry.forLoc(key)).thenReturn(table)
            }
        }
        fun claim() {
            fixture.launch { with(rewards) { claim() } }
            if (fixture.result == null) fixture.advance()
            assertNotNull(fixture.result)
        }
    }
    private fun run(mode: GauntletMode = GauntletMode.NORMAL) = GauntletRun(GauntletLayout.generate(Random(8)), mode, emptyMap())
    companion object {
        private const val SEED = "obj.prif_weapon_seed_enhanced"

        @JvmStatic @BeforeAll fun cache() { ServerCacheManager.init(240).close() }
    }
}
