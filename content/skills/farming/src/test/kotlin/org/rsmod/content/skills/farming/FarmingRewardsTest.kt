package org.rsmod.content.skills.farming

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
import org.rsmod.api.player.vars.VarPlayerIntMapSetter
import org.rsmod.api.random.GameRandom
import org.rsmod.api.registry.npc.NpcRegistry
import org.rsmod.api.repo.npc.NpcRepository
import org.rsmod.content.other.pets.PetFollowers
import org.rsmod.content.other.pets.PetInsurance
import org.rsmod.content.other.pets.PetRewards
import org.rsmod.content.other.pets.Pets
import org.rsmod.events.EventBus
import org.rsmod.game.MapClock
import org.rsmod.game.entity.NpcList
import org.rsmod.game.entity.Player
import org.rsmod.game.inv.InvObj
import org.rsmod.game.inv.Inventory
import org.rsmod.routefinder.collision.CollisionFlagMap

internal object FarmingRewardTestSupport {
    fun pets(random: GameRandom, events: EventBus): PetRewards {
        val collision = CollisionFlagMap()
        val npcs = NpcList()
        val clock = MapClock(100)
        val repo = NpcRepository(clock, NpcRegistry(npcs, collision, events), npcs)
        return PetRewards(PetFollowers(repo, npcs, clock, collision), PetInsurance(), random)
    }

    fun rewards(random: GameRandom, events: EventBus): FarmingRewards =
        FarmingRewards(pets(random, events), random)
}

@Execution(ExecutionMode.SAME_THREAD)
@ResourceLock("ServerCacheManager")
class FarmingRewardsTest {
    @Test
    fun `level gained by the action cannot improve its pet roll`() {
        val f = Fixture(listOf(0, 1))
        f.player.statMap.setBaseLevel(STAT_FARMING, 64)
        f.player.statMap.setCurrentLevel(STAT_FARMING, 99)
        val reward = f.rewards.prepare(f.player, f.crop)
        assertEquals(FarmingRewardRoll.PET, reward)
        assertEquals(f.crop.petChance - 64 * 25, f.random.bounds.first())
        assertEquals(0, f.player.inv.count(PET))
        assertFalse(f.insurance.isInsured(f.player, Pets["tangleroot"]))
        f.player.statMap.setBaseLevel(STAT_FARMING, 65)
        f.rewards.complete(f.player, reward)
        assertEquals(1, f.player.inv.count(PET))
        assertTrue(f.insurance.isInsured(f.player, Pets["tangleroot"]))
    }

    @Test
    fun `incoming seed takes the final slot before a bag pet and preserves insurance`() {
        val f = Fixture(listOf(0, 0))
        f.fill(27)
        val reward = f.rewards.prepare(f.player, f.crop)
        assertEquals(FarmingRewardRoll.BOTH, reward)
        f.rewards.complete(f.player, reward)
        assertEquals(1, f.player.inv.count(SEED))
        assertEquals(0, f.player.inv.count(PET))
        assertTrue(f.insurance.isInsured(f.player, Pets["tangleroot"]))
        assertEquals(27, f.player.inv.count(SPADE))
    }

    @Test
    fun `produce already occupying the final slot is never displaced by a rare reward`() {
        val f = Fixture(listOf(0, 0))
        f.fill(27)
        val reward = f.rewards.prepare(f.player, f.crop)
        f.player.inv[27] = InvObj(checkNotNull(f.crop.produce))
        f.rewards.complete(f.player, reward)
        assertEquals(1, f.player.inv.count(checkNotNull(f.crop.produce)))
        assertEquals(0, f.player.inv.count(SEED))
        assertEquals(0, f.player.inv.count(PET))
        assertTrue(f.insurance.isInsured(f.player, Pets["tangleroot"]))
    }

    @Test
    fun `overflowing seed stack is unchanged and does not prevent a bag pet`() {
        val f = Fixture(listOf(0, 0))
        f.player.inv[0] = InvObj(SEED, Int.MAX_VALUE)
        val reward = f.rewards.prepare(f.player, f.crop)
        f.rewards.complete(f.player, reward)
        assertEquals(Int.MAX_VALUE, f.player.inv.count(SEED))
        assertEquals(1, f.player.inv.count(PET))
        assertTrue(f.insurance.isInsured(f.player, Pets["tangleroot"]))
    }

    @Test
    fun `opted out players still roll the pet but never receive or roll a seed`() {
        val f = Fixture(listOf(1))
        VarPlayerIntMapSetter.set(f.player, "varp.farming_hespori_seed_opt_out", 1)
        assertEquals(FarmingRewardRoll.NONE, f.rewards.prepare(f.player, f.crop))
        assertEquals(listOf(f.crop.petChance - 99 * 25), f.random.bounds)
        assertEquals(0, f.player.inv.count(SEED))
    }

    private class Fixture(rolls: List<Int>) {
        val random = FixedRandom(rolls)
        private val events = EventBus()
        val insurance = PetInsurance()
        val rewards = FarmingRewardTestSupport.rewards(random, events)
        val crop = FarmingCrops.bySeed("obj.guam_seed")!!

        @OptIn(InternalApi::class)
        val player = Player().apply {
            client = SilentClient()
            inv = Inventory(checkNotNull(ServerCacheManager.getInventory("inv.inv".asRSCM())), arrayOfNulls(28))
            statMap.setBaseLevel(STAT_FARMING, 99)
            statMap.setCurrentLevel(STAT_FARMING, 99)
            VarPlayerIntMapSetter.set(this, "varp.follower_obj", "obj.skillpetmining".asRSCM())
        }
        fun fill(slots: Int) { repeat(slots) { player.inv[it] = InvObj(SPADE) } }
    }

    private class FixedRandom(rolls: List<Int>) : GameRandom {
        private val rolls = rolls.iterator()
        val bounds = mutableListOf<Int>()
        override fun of(maxExclusive: Int): Int {
            bounds += maxExclusive
            return rolls.next().also { require(it in 0 until maxExclusive) }
        }
        override fun of(minInclusive: Int, maxInclusive: Int): Int = error("Unexpected ranged roll")
        override fun randomDouble(): Double = error("Unexpected double roll")
    }

    companion object {
        private const val PET = "obj.skillpetfarming"
        private const val SEED = "obj.hespori_seed"
        private val RESTORED = mutableListOf<() -> Unit>()

        @JvmStatic @BeforeAll fun cache() {
            loadFarmingTestCache()
            installFarmingTestInventory(RESTORED)
        }

        @JvmStatic @AfterAll fun restore() {
            RESTORED.asReversed().forEach { it() }
            RESTORED.clear()
        }
    }
}
