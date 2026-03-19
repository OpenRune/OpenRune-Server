package org.rsmod.api.game.process.npc.mode

import dev.openrune.util.Coord
import dev.openrune.util.NpcPatrol
import jakarta.inject.Inject
import org.rsmod.game.entity.Npc
import org.rsmod.map.CoordGrid
import org.rsmod.routefinder.collision.CollisionFlagMap

public class NpcPatrolModeProcessor @Inject constructor(private val collision: CollisionFlagMap) {
    public fun process(npc: Npc) {
        val patrol = npc.type.patrol ?: return
        npc.updateIdleCycles()
        npc.patrol(patrol)
    }

    private fun Npc.updateIdleCycles() {
        if (hasMovedPreviousCycle) {
            patrolIdleCycles = 0
        } else {
            patrolIdleCycles++
        }
    }

    private fun Npc.patrol(patrol: NpcPatrol) {
        val waypoint = patrol[patrolWaypointIndex % patrol.size]
        if (patrolIdleCycles > IDLE_TELE_DELAY) {
            teleport(collision, waypoint.destination.toCoordGrid())
        }
        if (coords == waypoint.destination.toCoordGrid()) {
            if (waypoint.pauseDelay > patrolPauseCycles) {
                patrolPauseCycles++
                patrolIdleCycles = 0
            } else {
                patrolPauseCycles = 0
                val nextWaypoint = patrol[++patrolWaypointIndex % patrol.size]
                if (nextWaypoint.destination.level != level) {
                    teleport(collision, nextWaypoint.destination.toCoordGrid())
                } else {
                    walk(nextWaypoint.destination.toCoordGrid())
                }
            }
        } else {
            walk(waypoint.destination.toCoordGrid())
        }
    }

    public companion object {
        /**
         * The amount of cycles that the patrol npc can wait without moving before it teleports to
         * its next patrol waypoint.
         */
        public const val IDLE_TELE_DELAY: Int = 30

        public fun Coord.toCoordGrid(): CoordGrid = CoordGrid(x, z, level)
    }
}
