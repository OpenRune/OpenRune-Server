package org.rsmod.content.bosses.royaltitans

import com.github.michaelbull.logging.InlineLogger
import dev.openrune.types.MoveRestrict
import jakarta.inject.Inject
import jakarta.inject.Singleton
import java.util.IdentityHashMap
import org.rsmod.api.bosses.runtime.BossDeps
import org.rsmod.api.bosses.runtime.encounter
import org.rsmod.api.npc.apPlayer2
import org.rsmod.api.npc.interact.AiPlayerInteractions
import org.rsmod.api.player.isValidTarget
import org.rsmod.api.route.StepFactory
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.map.CoordGrid
import org.rsmod.routefinder.collision.CollisionStrategy
import org.rsmod.routefinder.flag.CollisionFlag

/**
 * Keeps each titan patrolling its own half of the arena. While engaged it holds the player
 * interaction so the encounter can attack; once it has attacked for a while it drops the
 * interaction, walks to another point in its strip, then re-engages. The titans never leave their
 * strip, so they never chase a player out of the arena.
 *
 * The arena map is walkable only down the five-wide corridor: the two side ledges the titans patrol
 * are `BLOCK_WALK`. They therefore walk with [MoveRestrict.Blocked], which is the engine's
 * "moves only across blocked tiles" restriction, so no collision is carved out of the map and
 * players stay confined to the corridor. The walkable rows are measured from the collision map
 * ([measureStrip]) rather than assumed, and every destination is checked with the same greedy
 * stepper the movement processor uses ([canReach]): the ledge narrows at its ends, so a row that
 * fits at the titan's current column is not necessarily reachable from every other column.
 * While engaged the restriction is [MoveRestrict.Normal].
 *
 * The interaction has to be dropped through [Npc.ignoreCombatInteractions]: an active `ApPlayer2`
 * interaction aborts the npc's route every cycle and re-applies its facing, so a plain
 * `clearInteraction()` walk never survives the first tick.
 */
@Singleton
class RoyalTitansPatrol
@Inject
constructor(
    private val deps: BossDeps,
    private val aiPlayerInteractions: AiPlayerInteractions,
    private val stepFactory: StepFactory,
) {
    private val logger = InlineLogger()
    private val states: MutableMap<Npc, State> = IdentityHashMap()

    fun start(npc: Npc, target: Player, westStrip: Boolean) {
        val state = states.getOrPut(npc) { State(target, westStrip) }
        state.target = target
        if (state.started) return
        state.started = true
        state.since = deps.mapClock.cycle
        state.dwell = FIRST_DWELL
        state.playableZ = measureStrip(npc)
        npc.moveRestrict = MoveRestrict.Normal
        lockFacing(npc, state)
        tick(npc)
    }

    fun stop(npc: Npc) {
        states.remove(npc)
    }

    /**
     * The rows of the ledge the titan can stand on, i.e. the contiguous band of `BLOCK_WALK` tiles
     * its whole body fits in at its current column. Walking with [MoveRestrict.Blocked] only
     * accepts blocked tiles, so the band is both what the map allows and what the restriction
     * accepts.
     */
    private fun measureStrip(npc: Npc): IntRange {
        val level = npc.coords.level
        val baseX = npc.coords.x
        val baseZ = npc.coords.z
        val size = npc.size
        fun fits(z: Int): Boolean {
            for (dz in 0 until size) {
                for (dx in 0 until size) {
                    val flags = deps.collision[baseX + dx, z + dz, level]
                    if (flags and CollisionFlag.BLOCK_WALK == 0) return false
                }
            }
            return true
        }
        var low = baseZ
        while (low > 0 && fits(low - 1)) low--
        var high = baseZ
        while (high < MAX_Z && fits(high + 1)) high++
        return low..high
    }

    private fun tick(npc: Npc) {
        deps.worldQueues.add(1) {
            val state = states[npc] ?: return@add
            if (!npc.isSlotAssigned || npc.hitpoints <= 0 || !state.target.isValidTarget()) {
                states.remove(npc)
                return@add
            }
            runCatching { advance(npc, state) }
                .onFailure {
                    logger.warn(it) {
                        "[royal-titans] patrol failed for ${npc.type.internalName}"
                    }
                    recover(npc, state)
                    states.remove(npc)
                }
            if (states.containsKey(npc)) {
                tick(npc)
            }
        }
    }

    /** Leaves the titan in a fighting state when the patrol itself breaks, never half-walking. */
    private fun recover(npc: Npc, state: State) {
        npc.abortRoute()
        npc.ignoreCombatInteractions = false
        npc.moveRestrict = MoveRestrict.Normal
        if (!npc.isSlotAssigned || npc.hitpoints <= 0 || !state.target.isValidTarget()) return
        npc.apPlayer2(state.target, aiPlayerInteractions)
    }

    private fun advance(npc: Npc, state: State) {
        val tick = deps.mapClock.cycle
        when (state.mode) {
            Mode.Engaged -> {
                if (!npc.isFacingLocked) {
                    lockFacing(npc, state)
                }
                val busy = tick < deps.encounter(npc).busyUntil
                val waited = tick - state.since
                when {
                    !busy && waited >= state.dwell -> beginWalk(npc, state, tick)
                    waited >= MAX_STAND_TICKS -> {
                        beginWalk(npc, state, tick)
                    }
                }
            }
            Mode.Walking -> {
                when {
                    npc.routeDestination.isEmpty() -> arrive(npc, state)
                    tick - state.since >= MAX_WALK_TICKS -> {
                        logger.warn {
                            "[royal-titans] patrol stuck ${npc.type.internalName} " +
                                "from=(${state.walkFrom.x}, ${state.walkFrom.z}) " +
                                "now=(${npc.coords.x}, ${npc.coords.z}) " +
                                "dest=(${state.walkDest.x}, ${state.walkDest.z}) " +
                                "moveRestrict=${npc.moveRestrict} speed=${npc.moveSpeed}"
                        }
                        arrive(npc, state)
                    }
                }
            }
        }
    }

    private fun beginWalk(npc: Npc, state: State, tick: Int) {
        if (npc.movementLocked) return
        val dest =
            destination(npc, state)
                ?: run {
                    // No reachable tile right now (e.g. the player is on a row where the ledge
                    // narrows); stay engaged and try again after the normal dwell instead of
                    // walking into a wall and stalling until the walk timeout.
                    state.since = tick
                    return
                }
        state.mode = Mode.Walking
        state.since = tick
        state.walkFrom = npc.coords
        state.walkDest = dest
        npc.ignoreCombatInteractions = true
        npc.clearInteraction()
        npc.moveRestrict = MoveRestrict.Blocked
        npc.walk(dest) { arrive(npc, state) }
    }

    /**
     * [StepFactory.createPath] is the same greedy stepper the npc movement processor runs, so a
     * path that ends on [dest] is exactly a walk the engine will complete this cycle.
     */
    private fun canReach(npc: Npc, dest: CoordGrid): Boolean {
        if (dest == npc.coords) return true
        val selfFlag = npc.blockWalkCollisionFlag
        if (selfFlag != null) {
            npc.removeBlockWalkCollision(deps.collision, npc.coords)
        }
        val path =
            try {
                stepFactory.createPath(
                    source = npc.coords,
                    destination = dest,
                    size = npc.size,
                    extraFlag = 0,
                    collision = CollisionStrategy.Blocked,
                )
            } finally {
                if (selfFlag != null) {
                    npc.addBlockWalkCollision(deps.collision, npc.coords)
                }
            }
        return path.lastOrNull() == dest
    }

    /**
     * The titan attacks one to two ticks after its last step in the capture, never on the same tick
     * it stops walking, and it turns before the first attack. The interaction is therefore resumed
     * [SETTLE_TICKS] after the route ends, which is also when the front/back dwell starts counting.
     */
    private fun arrive(npc: Npc, state: State) {
        if (state.mode != Mode.Walking) return
        state.mode = Mode.Engaged
        state.since = deps.mapClock.cycle
        state.dwell = SETTLE_TICKS
        npc.abortRoute()
        npc.moveRestrict = MoveRestrict.Normal
        npc.ignoreCombatInteractions = false
        if (!npc.isSlotAssigned || npc.hitpoints <= 0 || !state.target.isValidTarget()) return
        lockFacing(npc, state)
        deps.worldQueues.add(SETTLE_TICKS) {
            if (states[npc] !== state || state.mode != Mode.Engaged) return@add
            if (!npc.isSlotAssigned || npc.hitpoints <= 0 || !state.target.isValidTarget()) return@add
            if (!npc.isFacingLocked) {
                lockFacing(npc, state)
            }
            val front = isAtFront(npc, state)
            state.since = deps.mapClock.cycle
            state.dwell = if (front) FRONT_DWELL else BACK_DWELL
            logger.debug {
                "[royal-titans] patrol re-engage ${npc.type.internalName} " +
                    "(${npc.coords.x}, ${npc.coords.z}) front=$front dwell=${state.dwell}"
            }
            npc.apPlayer2(state.target, aiPlayerInteractions)
        }
    }

    private fun isAtFront(npc: Npc, state: State): Boolean {
        val spawnX = npc.spawnCoords.x
        return if (state.westStrip) npc.coords.x >= spawnX else npc.coords.x <= spawnX
    }

    /**
     * Pins the titan's facing to its own arena half instead of the player it is fighting. The lock
     * square sits well outside the strip so the angle stays cardinal while it walks, and it is
     * re-applied whenever something (e.g. a phase transition) clears it.
     */
    private fun lockFacing(npc: Npc, state: State) {
        val offset = if (state.westStrip) FACE_DISTANCE else -FACE_DISTANCE
        val target = CoordGrid(npc.coords.x + offset, npc.coords.z, npc.coords.level)
        npc.lockFacing(target, targetWidth = 1, targetLength = 1)
    }

    private fun destination(npc: Npc, state: State): CoordGrid? {
        val origin = npc.spawnCoords
        val lowX = if (state.westStrip) origin.x - STRIP_HALF_WIDTH else origin.x
        val highX = if (state.westStrip) origin.x else origin.x + STRIP_HALF_WIDTH
        val preferred = if (deps.random.of(2) == 0) lowX else highX
        val x = if (preferred == npc.coords.x) (if (preferred == lowX) highX else lowX) else preferred
        val otherX = if (x == lowX) highX else lowX
        val z = (state.target.coords.z + deps.random.of(-1, 1)).coerceIn(state.playableZ)
        // Prefer the requested row, then nearby rows, then the other column. A row can be
        // walkable at one column but blocked at another where the ledge narrows, so only offer
        // destinations with a complete route for the titan's own size and walk restriction.
        for (candidateX in intArrayOf(x, otherX)) {
            for (offset in Z_OFFSETS) {
                val candidateZ = z + offset
                if (candidateZ !in state.playableZ) continue
                val candidate = CoordGrid(candidateX, candidateZ, origin.level)
                if (canReach(npc, candidate)) return candidate
            }
        }
        return null
    }

    private enum class Mode {
        Engaged,
        Walking,
    }

    private class State(var target: Player, val westStrip: Boolean) {
        var mode: Mode = Mode.Engaged
        var since: Int = 0
        var dwell: Int = STAND_TICKS
        var started: Boolean = false
        var playableZ: IntRange = 0..0
        var walkFrom: CoordGrid = CoordGrid.NULL
        var walkDest: CoordGrid = CoordGrid.NULL
    }

    private companion object {
        private const val STRIP_HALF_WIDTH = 2
        private val Z_OFFSETS = intArrayOf(0, -1, 1, -2, 2, -3, 3, -4, 4)
        private const val STAND_TICKS = 14
        private const val FIRST_DWELL = 4
        private const val FRONT_DWELL = 30
        private const val BACK_DWELL = 14
        private const val SETTLE_TICKS = 2
        private const val MAX_STAND_TICKS = 60
        private const val MAX_WALK_TICKS = 20
        private const val FACE_DISTANCE = 8
        private const val MAX_Z = 16383
    }
}
