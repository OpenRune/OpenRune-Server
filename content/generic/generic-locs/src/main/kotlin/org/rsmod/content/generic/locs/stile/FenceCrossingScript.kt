package org.rsmod.content.generic.locs.stile

import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.script.onOpLoc1
import org.rsmod.game.loc.BoundLocInfo
import org.rsmod.game.loc.LocAngle
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

/**
 * Climbs over a one-tile gap in a fence, landing on the tile beyond it. At angle west or east the
 * fence runs north to south, so the crossing is east-west; at north or south it is north-south.
 */
class FenceCrossingScript : PluginScript() {
    override fun ScriptContext.startup() {
        for (fence in FENCES) {
            onOpLoc1(fence) { climbFence(it.loc) }
        }
    }

    private suspend fun ProtectedAccess.climbFence(fence: BoundLocInfo) {
        arriveDelay()
        val start = coords
        climbOver(start, farSide(fence, start))
        delay(CLIMB_OVER_TICKS)
    }

    private fun farSide(fence: BoundLocInfo, start: CoordGrid): CoordGrid =
        if (fence.angle == LocAngle.West || fence.angle == LocAngle.East) {
            fence.coords.translateX(if (start.x <= fence.coords.x) 1 else -1)
        } else {
            fence.coords.translateZ(if (start.z <= fence.coords.z) 1 else -1)
        }

    private companion object {
        private val FENCES = listOf("loc.gertrudefence")
    }
}
