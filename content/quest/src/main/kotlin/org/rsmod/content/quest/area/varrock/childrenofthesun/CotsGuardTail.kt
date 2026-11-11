package org.rsmod.content.quest.area.varrock.childrenofthesun

import kotlin.math.abs
import org.rsmod.map.CoordGrid

internal enum class TailResult {
    Continue,
    Spotted,
    TooFar,
    Arrived,
}

/**
 * The guard's path through south-east Varrock. Between the lookouts the guard walks one tile per
 * tick; at each lookout it stops, turns around and, once the turn has played out, spots anyone in
 * front of him with line of sight.
 */
internal object CotsRoute {
    val Door: CoordGrid = CoordGrid(3259, 3400)

    private val Waypoints =
        listOf(
            CoordGrid(3228, 3430),
            CoordGrid(3236, 3430),
            CoordGrid(3237, 3429),
            CoordGrid(3239, 3429),
            CoordGrid(3241, 3429),
            CoordGrid(3242, 3428),
            CoordGrid(3242, 3424),
            CoordGrid(3243, 3423),
            CoordGrid(3243, 3410),
            CoordGrid(3242, 3409),
            CoordGrid(3242, 3402),
            CoordGrid(3241, 3401),
            CoordGrid(3239, 3401),
            CoordGrid(3237, 3401),
            CoordGrid(3235, 3399),
            CoordGrid(3235, 3390),
            CoordGrid(3236, 3389),
            CoordGrid(3248, 3389),
            CoordGrid(3249, 3390),
            CoordGrid(3249, 3396),
            CoordGrid(3250, 3397),
            CoordGrid(3253, 3397),
            CoordGrid(3255, 3397),
            CoordGrid(3258, 3400),
            CoordGrid(3259, 3400),
        )

    val Lookouts: List<CoordGrid> =
        listOf(
            CoordGrid(3239, 3429),
            CoordGrid(3243, 3412),
            CoordGrid(3239, 3401),
            CoordGrid(3243, 3389),
            CoordGrid(3253, 3397),
        )

    val Tiles: List<CoordGrid> = expand(Waypoints)

    val LookoutIndices: Set<Int> = Lookouts.map { Tiles.indexOf(it) }.toSet()

    private fun expand(waypoints: List<CoordGrid>): List<CoordGrid> {
        val tiles = ArrayList<CoordGrid>()
        tiles += waypoints.first()
        for ((from, to) in waypoints.zipWithNext()) {
            val dx = to.x - from.x
            val dz = to.z - from.z
            require(dx == 0 || dz == 0 || abs(dx) == abs(dz)) {
                "Route leg $from -> $to is not straight or diagonal."
            }
            val stepX = dx.coerceIn(-1, 1)
            val stepZ = dz.coerceIn(-1, 1)
            var current = from
            while (current != to) {
                current = CoordGrid(current.x + stepX, current.z + stepZ, current.level)
                tiles += current
            }
        }
        return tiles
    }
}

internal class GuardTail(
    private val route: List<CoordGrid>,
    private val lookouts: Set<Int>,
    private val canSee: (CoordGrid, CoordGrid) -> Boolean,
) {
    private enum class Phase {
        Setup,
        Walking,
        Stopped,
        Turning,
        Looking,
    }

    private var phase = Phase.Setup
    private var phaseTicks = 0

    var index: Int = 0
        private set

    val guard: CoordGrid
        get() = route[index]

    val isLooking: Boolean
        get() = phase == Phase.Looking

    val startedTurning: Boolean
        get() = phase == Phase.Turning && phaseTicks == 0

    val lookingAt: CoordGrid
        get() = route[(index - 1).coerceAtLeast(0)]

    fun depart() {
        if (route.size > 1) {
            index = 1
        }
        enter(Phase.Walking)
    }

    /** [at] is where the guard npc really stands; it lags [guard] until the npc takes its step. */
    fun tick(player: CoordGrid, at: CoordGrid = guard): TailResult {
        if (player.level != at.level || distance(at, player) > MaxDistance) {
            return TailResult.TooFar
        }
        when (phase) {
            Phase.Setup -> if (++phaseTicks >= SetupTicks) enter(Phase.Walking)
            Phase.Walking -> {
                if (at != guard) {
                    return TailResult.Continue
                }
                if (index == route.lastIndex) {
                    return TailResult.Arrived
                }
                index++
                if (index in lookouts) {
                    enter(Phase.Stopped)
                }
            }
            Phase.Stopped -> if (at == guard && ++phaseTicks >= StopTicks) enter(Phase.Turning)
            Phase.Turning -> {
                if (++phaseTicks < TurnTicks) {
                    return TailResult.Continue
                }
                enter(Phase.Looking)
                return look(player, at)
            }
            Phase.Looking -> return look(player, at)
        }
        return TailResult.Continue
    }

    private fun look(player: CoordGrid, at: CoordGrid): TailResult {
        val inFront = isInFront(at, player)
        if (inFront && distance(at, player) <= SightRange && canSee(at, player)) {
            return TailResult.Spotted
        }
        if (++phaseTicks >= LookTicks) {
            enter(Phase.Walking)
        }
        return TailResult.Continue
    }

    private fun isInFront(at: CoordGrid, player: CoordGrid): Boolean {
        val facing = lookingAt
        val dx = player.x - at.x
        val dz = player.z - at.z
        val fx = facing.x - at.x
        val fz = facing.z - at.z
        return dx * fx + dz * fz > 0
    }

    private fun enter(next: Phase) {
        phase = next
        phaseTicks = 0
    }

    private fun distance(from: CoordGrid, to: CoordGrid): Int =
        maxOf(abs(to.x - from.x), abs(to.z - from.z))

    companion object {
        const val SetupTicks = 3
        const val StopTicks = 7
        const val TurnTicks = 2
        const val LookTicks = 5
        const val MaxDistance = 16
        const val SightRange = 16
    }
}
