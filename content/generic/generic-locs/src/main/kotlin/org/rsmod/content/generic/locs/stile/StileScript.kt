package org.rsmod.content.generic.locs.stile

import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.script.onOpLoc1
import org.rsmod.game.loc.BoundLocInfo
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

/**
 * Steps over a fence stile. A stile spans the two tiles of its own footprint, one either side of
 * the fence, and climbing it moves the player to whichever of those tiles they are not standing on.
 */
class StileScript : PluginScript() {
    override fun ScriptContext.startup() {
        for (stile in STILES) {
            onOpLoc1(stile) { climbOver(it.loc) }
        }
    }

    private fun ProtectedAccess.climbOver(loc: BoundLocInfo) {
        val start = coords
        val far =
            loc.coords.translate(loc.adjustedWidth - 1, loc.adjustedLength - 1)
        val dest = if (start == far) loc.coords else far
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

    private companion object {
        private const val STEP_DELAY = 30
        private const val CROSS_DELAY = 94

        private const val FACE_SOUTH = 0
        private const val FACE_WEST = 512
        private const val FACE_NORTH = 1024
        private const val FACE_EAST = 1536

        private val STILES = listOf("loc.fullstyle", "loc.qip_sheep_shearer_fullstyle")
    }
}
