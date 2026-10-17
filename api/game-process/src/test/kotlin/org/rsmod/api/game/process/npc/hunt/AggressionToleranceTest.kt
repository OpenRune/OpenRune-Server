package org.rsmod.api.game.process.npc.hunt

import dev.openrune.ServerCacheManager
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.junit.jupiter.api.parallel.ResourceLock
import org.rsmod.api.npc.aggression.AggressionTolerance
import org.rsmod.api.npc.aggression.AggressionTolerance.Companion.TOLERANCE_TICKS
import org.rsmod.game.MapClock
import org.rsmod.game.entity.Player
import org.rsmod.map.CoordGrid

@Execution(ExecutionMode.SAME_THREAD)
@ResourceLock("ServerCacheManager")
class AggressionToleranceTest {
    private val clock = MapClock(cycle = 50)
    private val tolerance = AggressionTolerance(clock)
    private val here = CoordGrid(1773, 3460, 0)

    @Test
    fun `players become tolerant after ten minutes of being seen`() {
        val player = Player().apply { coords = here }
        tolerance.isTolerant(player)
        stayFor(player, TOLERANCE_TICKS - 1)
        assertFalse(tolerance.isTolerant(player))
        clock.cycle += 1
        assertTrue(tolerance.isTolerant(player))
    }

    private fun stayFor(player: Player, ticks: Int) {
        var left = ticks
        while (left > 0) {
            val step = minOf(left, 25)
            clock.cycle += step
            tolerance.isTolerant(player)
            left -= step
        }
    }

    companion object {
        @JvmStatic
        @BeforeAll
        fun cache() {
            ServerCacheManager.init(240).close()
        }
    }
}
