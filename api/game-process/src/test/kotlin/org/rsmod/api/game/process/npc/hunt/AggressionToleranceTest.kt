package org.rsmod.api.game.process.npc.hunt

import dev.openrune.ServerCacheManager
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.junit.jupiter.api.parallel.ResourceLock
import org.rsmod.api.npc.aggression.AggressionTolerance
import org.rsmod.api.npc.aggression.AggressionTolerance.Companion.ABSENT_TICKS
import org.rsmod.api.npc.aggression.AggressionTolerance.Companion.LEAVE_DISTANCE
import org.rsmod.api.npc.aggression.AggressionTolerance.Companion.TOLERANCE_TICKS
import org.rsmod.game.MapClock
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.map.CoordGrid

@Execution(ExecutionMode.SAME_THREAD)
@ResourceLock("ServerCacheManager")
class AggressionToleranceTest {
    private val clock = MapClock(cycle = 50)
    private val tolerance = AggressionTolerance(clock)
    private val here = CoordGrid(1773, 3460, 0)

    @Test
    fun `players are not tolerant when they first arrive`() {
        val player = player(here)
        assertFalse(tolerance.isTolerant(player))
    }

    @Test
    fun `players become tolerant after ten minutes of being seen`() {
        val player = player(here)
        tolerance.isTolerant(player)
        stayFor(player, TOLERANCE_TICKS - 1)
        assertFalse(tolerance.isTolerant(player))
        clock.cycle += 1
        assertTrue(tolerance.isTolerant(player))
    }

    @Test
    fun `ten minutes is one thousand game ticks`() {
        assertEquals(10 * 60 * 1000 / 600, TOLERANCE_TICKS)
    }

    @Test
    fun `moving far from where the timer started restarts it`() {
        val player = player(here)
        tolerance.isTolerant(player)
        stayFor(player, TOLERANCE_TICKS)
        assertTrue(tolerance.isTolerant(player))

        player.coords = here.translateX(LEAVE_DISTANCE + 1)
        assertFalse(tolerance.isTolerant(player))
        player.coords = here
        assertFalse(tolerance.isTolerant(player))
    }

    @Test
    fun `wandering inside the area does not restart the timer`() {
        val player = player(here)
        tolerance.isTolerant(player)
        stayFor(player, TOLERANCE_TICKS)
        player.coords = here.translate(LEAVE_DISTANCE, -LEAVE_DISTANCE)
        assertTrue(tolerance.isTolerant(player))
    }

    @Test
    fun `going unseen for a while restarts the timer`() {
        val player = player(here)
        tolerance.isTolerant(player)
        stayFor(player, TOLERANCE_TICKS)
        clock.cycle += ABSENT_TICKS + 1
        assertFalse(tolerance.isTolerant(player))
    }

    @Test
    fun `reset clears an existing timer`() {
        val player = player(here)
        tolerance.isTolerant(player)
        stayFor(player, TOLERANCE_TICKS)
        tolerance.reset(player)
        assertFalse(tolerance.isTolerant(player))
    }

    @Test
    fun `timers are tracked per player`() {
        val veteran = player(here)
        tolerance.isTolerant(veteran)
        stayFor(veteran, TOLERANCE_TICKS)
        val newcomer = player(here)
        assertTrue(tolerance.isTolerant(veteran))
        assertFalse(tolerance.isTolerant(newcomer))
    }

    @Test
    fun `only enrolled npc types are covered`() {
        val player = player(here)
        tolerance.isTolerant(player)
        stayFor(player, TOLERANCE_TICKS)
        val rocks = Npc("npc.zeah_sandcrab_inactive", here)
        val other = Npc("npc.horror_rockcrab", here)
        assertFalse(tolerance.isTolerant(rocks, player))

        tolerance.enroll("npc.zeah_sandcrab_inactive")
        assertTrue(tolerance.appliesTo(rocks))
        assertTrue(tolerance.isTolerant(rocks, player))
        assertFalse(tolerance.appliesTo(other))
        assertFalse(tolerance.isTolerant(other, player))
    }

    private fun player(at: CoordGrid) = Player().apply { coords = at }

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
