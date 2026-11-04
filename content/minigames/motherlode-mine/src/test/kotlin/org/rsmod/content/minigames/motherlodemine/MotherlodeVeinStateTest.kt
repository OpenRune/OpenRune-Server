package org.rsmod.content.minigames.motherlodemine

import dev.openrune.ServerCacheManager
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.ResourceLock
import org.rsmod.api.random.GameRandom
import org.rsmod.content.minigames.motherlodemine.configs.MotherlodeConstants
import org.rsmod.game.MapClock
import org.rsmod.game.loc.BoundLocInfo
import org.rsmod.game.loc.LocEntity
import org.rsmod.map.CoordGrid

@ResourceLock("ServerCacheManager")
class MotherlodeVeinStateTest {
    @Test fun `upper vein expires without any further mining`() {
        val clock = MapClock(100)
        val state = MotherlodeVeinState(clock, FixedRandom)
        val vein = vein(3755, 5680)
        state.startIfAbsent(vein, upperFloor = true)
        clock.cycle += MotherlodeConstants.UPPER_VEIN_DEPLETE_TICKS.first - 1
        assertTrue(state.dueUpperVeins().isEmpty())
        clock.tick()
        assertEquals(listOf(vein), state.dueUpperVeins())
    }

    @Test fun `another miner cannot extend an upper vein lifetime`() {
        val clock = MapClock(100)
        val state = MotherlodeVeinState(clock, FixedRandom)
        val vein = vein(3755, 5680)
        state.startIfAbsent(vein, true)
        clock.cycle += 30
        state.startIfAbsent(vein, true)
        clock.cycle = 100 + MotherlodeConstants.UPPER_VEIN_DEPLETE_TICKS.first
        assertEquals(listOf(vein), state.dueUpperVeins())
    }

    @Test fun `lower veins keep their mining-triggered depletion`() {
        val clock = MapClock(100)
        val state = MotherlodeVeinState(clock, FixedRandom)
        val vein = vein(3755, 5650)
        state.startIfAbsent(vein, false)
        clock.cycle += MotherlodeConstants.LOWER_VEIN_DEPLETE_TICKS.last
        assertTrue(state.isDepleted(vein.coords))
        assertTrue(state.dueUpperVeins().isEmpty())
    }

    @Test fun `respawned vein starts a fresh lifetime`() {
        val clock = MapClock(100)
        val state = MotherlodeVeinState(clock, FixedRandom)
        val vein = vein(3755, 5680)
        state.startIfAbsent(vein, true)
        clock.cycle += MotherlodeConstants.UPPER_VEIN_DEPLETE_TICKS.last
        state.clear(vein.coords)
        assertTrue(state.dueUpperVeins().isEmpty())
        state.startIfAbsent(vein, true)
        assertFalse(state.isDepleted(vein.coords))
    }

    private fun vein(x: Int, z: Int) = BoundLocInfo(CoordGrid(x, z), LocEntity(26661, 10, 0), 2, 1, 1, 0)

    private object FixedRandom : GameRandom {
        override fun of(maxExclusive: Int): Int = 0
        override fun of(minInclusive: Int, maxInclusive: Int): Int = minInclusive
        override fun randomDouble(): Double = 0.0
    }

    companion object {
        @JvmStatic @BeforeAll fun cache() { ServerCacheManager.init(241).close() }
    }
}
