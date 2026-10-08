package org.rsmod.content.generic.locs.stile

import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.map.CoordGrid

internal const val CLIMB_OVER_TICKS = 3

private const val STEP_DELAY = 30
private const val CROSS_DELAY = 94

private const val FACE_SOUTH = 0
private const val FACE_WEST = 512
private const val FACE_NORTH = 1024
private const val FACE_EAST = 1536

internal fun ProtectedAccess.climbOver(start: CoordGrid, dest: CoordGrid) {
    exactMove(start, dest, STEP_DELAY, CROSS_DELAY, facing(start, dest))
    anim("seq.human_walk_style", delay = STEP_DELAY)
}

private fun facing(start: CoordGrid, dest: CoordGrid): Int =
    when {
        dest.z > start.z -> FACE_NORTH
        dest.z < start.z -> FACE_SOUTH
        dest.x > start.x -> FACE_EAST
        else -> FACE_WEST
    }
