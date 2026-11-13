package org.rsmod.content.skills.agility.werewolf

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.rsmod.map.CoordGrid

class SkullballPitchTest {
    @Test
    fun `a kick rolls a tile a tick until its four tiles are done`() {
        val pitch = pitch()
        val path = rollOut(pitch, tile(10, 10), dx = 0, dz = 1, steps = 4)

        assertEquals((11..14).map { tile(10, it) }, path)
    }

    @Test
    fun `a bounce sends the ball back half of what is left`() {
        val pitch = pitch(walls = setOf(tile(12, 10)))
        val path = rollOut(pitch, tile(10, 10), dx = 1, dz = 0, steps = 9)

        assertEquals(listOf(11, 10, 9, 8, 7).map { tile(it, 10) }, path)
    }

    @Test
    fun `a ball with a single tile left stops against a wall`() {
        val pitch = pitch(walls = setOf(tile(11, 10)))

        val roll = pitch.roll(tile(10, 10), dx = 1, dz = 0, steps = 1, scored = 0)

        assertEquals(BallRoll.Stopped(bounced = true), roll)
    }

    @Test
    fun `a diagonal roll bounces off only the side that blocks`() {
        val pitch = pitch(walls = setOf(tile(11, 10), tile(11, 11)))

        val roll = pitch.roll(tile(10, 10), dx = 1, dz = 1, steps = 6, scored = 0)

        assertEquals(tile(9, 11), (roll as BallRoll.Rolled).to)
        assertEquals(-1, roll.dx)
        assertEquals(1, roll.dz)
    }

    @Test
    fun `a goal centre lets the ball through only along the goal`() {
        val goal = SkullballGoal(tile(10, 12), dx = 0, dz = 1)
        val pitch = pitch(goals = listOf(goal), walls = setOf(tile(10, 12)))

        val along = pitch.roll(tile(10, 11), dx = 0, dz = 1, steps = 2, scored = 0)
        val across = pitch.roll(tile(9, 12), dx = 1, dz = 0, steps = 1, scored = 0)

        assertEquals(BallRoll.Rolled(tile(10, 12), 0, 1, 1, false, 0, true), along)
        assertEquals(BallRoll.Stopped(bounced = true), across)
    }

    @Test
    fun `a ball going back through a goal is not going the scoring way`() {
        val goal = SkullballGoal(tile(10, 12), dx = 0, dz = 1)
        val pitch = pitch(goals = listOf(goal))

        val roll =
            pitch.roll(tile(10, 13), dx = 0, dz = -1, steps = 2, scored = 0) as BallRoll.Rolled

        assertEquals(0, roll.goal)
        assertFalse(roll.forward)
    }

    @Test
    fun `the hole takes the ball only once every goal is scored`() {
        val goals = listOf(SkullballGoal(tile(10, 12), 0, 1), SkullballGoal(tile(10, 20), 0, 1))
        val hole = SkullballGoal(tile(5, 5), dx = 0, dz = 1)
        val pitch = pitch(goals = goals, hole = hole, walls = setOf(tile(5, 6)))

        val early = pitch.roll(tile(5, 5), dx = 0, dz = 1, steps = 3, scored = 1)
        val done = pitch.roll(tile(5, 5), dx = 0, dz = 1, steps = 3, scored = 2)
        val wrongWay = pitch.roll(tile(5, 5), dx = 1, dz = 0, steps = 3, scored = 2)

        assertTrue(early is BallRoll.Rolled)
        assertEquals(BallRoll.Holed, done)
        assertTrue(wrongWay is BallRoll.Rolled)
    }

    @Test
    fun `goals are read from loc angles`() {
        val ways = (0..3).map { SkullballGoal.of(tile(0, 0), it) }.map { it.dx to it.dz }

        assertEquals(listOf(0 to 1, 1 to 0, 0 to -1, -1 to 0), ways)
    }

    @Test
    fun `a game inside par pays the full xp`() {
        assertEquals(750, xp(elapsed = 0))
        assertEquals(750, xp(elapsed = 400))
    }

    @Test
    fun `each whole three seconds over par costs eight xp`() {
        assertEquals(750, xp(elapsed = 404))
        assertEquals(742, xp(elapsed = 405))
        assertEquals(742, xp(elapsed = 409))
        assertEquals(734, xp(elapsed = 410))
        assertEquals(750 - 8 * 60, xp(elapsed = 400 + 5 * 60))
    }

    @Test
    fun `a very slow game pays nothing rather than less than nothing`() {
        assertEquals(6, xp(elapsed = 400 + 5 * 93))
        assertEquals(0, xp(elapsed = 400 + 5 * 94))
        assertEquals(0, xp(elapsed = Int.MAX_VALUE))
    }

    private fun xp(elapsed: Int): Int =
        SkullballPitch.xp(elapsed, xp = 750, parTicks = 400, penaltyTicks = 5, penaltyXp = 8)

    private fun pitch(
        goals: List<SkullballGoal> = emptyList(),
        hole: SkullballGoal = SkullballGoal(tile(0, 0), 0, 1),
        walls: Set<CoordGrid> = emptySet(),
    ): SkullballPitch =
        SkullballPitch(goals, hole) { from, dx, dz -> from.translate(dx, dz) in walls }

    private fun rollOut(
        pitch: SkullballPitch,
        start: CoordGrid,
        dx: Int,
        dz: Int,
        steps: Int,
    ): List<CoordGrid> {
        val path = mutableListOf<CoordGrid>()
        var roll = pitch.roll(start, dx, dz, steps, scored = 0)
        while (roll is BallRoll.Rolled) {
            path += roll.to
            roll = pitch.roll(roll.to, roll.dx, roll.dz, roll.steps, scored = 0)
        }
        return path
    }

    private fun tile(x: Int, z: Int): CoordGrid = CoordGrid(x, z, 0)
}
