package org.rsmod.content.skills.agility.werewolf

import org.rsmod.map.CoordGrid

internal class SkullballGoal(val centre: CoordGrid, val dx: Int, val dz: Int) {
    fun isAlong(dx: Int, dz: Int): Boolean =
        (dx == this.dx && dz == this.dz) || (dx == -this.dx && dz == -this.dz)

    companion object {
        private val WAYS = listOf(0 to 1, 1 to 0, 0 to -1, -1 to 0)

        /** [way] is a loc angle: 0 north, 1 east, 2 south, 3 west. */
        fun of(centre: CoordGrid, way: Int): SkullballGoal {
            val (dx, dz) = WAYS[way]
            return SkullballGoal(centre, dx, dz)
        }
    }
}

internal sealed interface BallRoll {
    /**
     * The ball moved onto [to] and heads [dx]/[dz] with [steps] left. [goal] is the index of the
     * goal whose centre it rolled onto, or `-1`, and [forward] whether it went the scoring way.
     */
    data class Rolled(
        val to: CoordGrid,
        val dx: Int,
        val dz: Int,
        val steps: Int,
        val bounced: Boolean,
        val goal: Int,
        val forward: Boolean,
    ) : BallRoll

    data class Stopped(val bounced: Boolean) : BallRoll

    data object Holed : BallRoll
}

/**
 * The skullball pitch: one tick of a rolling ball. The ball moves a tile a tick, through a goal's
 * centre only along the goal's way, and off anything else that blocks it; [blocked] says whether a
 * step leaves the pitch's open ground. Once every goal is scored, a ball rolling the [hole]'s way
 * from its tile drops into the hole.
 */
internal class SkullballPitch(
    val goals: List<SkullballGoal>,
    val hole: SkullballGoal,
    private val blocked: (from: CoordGrid, dx: Int, dz: Int) -> Boolean,
) {
    fun roll(from: CoordGrid, dx: Int, dz: Int, steps: Int, scored: Int): BallRoll {
        if (steps <= 0 || (dx == 0 && dz == 0)) {
            return BallRoll.Stopped(bounced = false)
        }
        if (scored == goals.size && from == hole.centre && dx == hole.dx && dz == hole.dz) {
            return BallRoll.Holed
        }
        if (enters(from, dx, dz)) {
            return rolled(from.translate(dx, dz), dx, dz, steps - 1, bounced = false)
        }
        // UNVERIFIED: a bounce halving what is left of the roll, fitted to the wiki's tip that a
        // shot east into the wall at goal 10 stops where three taps west do.
        val left = steps / 2
        val (bx, bz) = rebound(from, dx, dz)
        if (left == 0 || !enters(from, bx, bz)) {
            return BallRoll.Stopped(bounced = true)
        }
        return rolled(from.translate(bx, bz), bx, bz, left - 1, bounced = true)
    }

    private fun rolled(to: CoordGrid, dx: Int, dz: Int, steps: Int, bounced: Boolean): BallRoll {
        val goal = goals.indexOfFirst { it.centre == to }
        val forward = goal >= 0 && goals[goal].dx == dx && goals[goal].dz == dz
        return BallRoll.Rolled(to, dx, dz, steps, bounced, goal, forward)
    }

    private fun enters(from: CoordGrid, dx: Int, dz: Int): Boolean {
        val to = from.translate(dx, dz)
        val goal = goals.firstOrNull { it.centre == to }
        if (goal != null) {
            return goal.isAlong(dx, dz)
        }
        return !blocked(from, dx, dz)
    }

    private fun rebound(from: CoordGrid, dx: Int, dz: Int): Pair<Int, Int> {
        if (dx == 0 || dz == 0) {
            return -dx to -dz
        }
        val alongX = enters(from, dx, 0)
        val alongZ = enters(from, 0, dz)
        return when {
            alongX == alongZ -> -dx to -dz
            alongX -> dx to -dz
            else -> -dx to dz
        }
    }

    companion object {
        /**
         * The agility xp for a game whose final shot came [elapsed] ticks after the first goal:
         * [xp] within [parTicks], less [penaltyXp] for each whole [penaltyTicks] over.
         */
        fun xp(elapsed: Int, xp: Int, parTicks: Int, penaltyTicks: Int, penaltyXp: Int): Int {
            val over = elapsed - parTicks
            if (over <= 0) {
                return xp
            }
            // UNVERIFIED: whole periods only, and no floor but zero.
            val lost = (over / penaltyTicks).toLong() * penaltyXp
            return (xp - lost).coerceAtLeast(0).toInt()
        }
    }
}
