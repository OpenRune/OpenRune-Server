package org.rsmod.content.generic.locs.stile

import org.rsmod.api.player.hook.TeleportType
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.script.onOpLoc1
import org.rsmod.game.loc.BoundLocInfo
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

/**
 * Steps over a fence stile. A stile's footprint is two tiles, one either side of the fence, and
 * blocks walking. The player walks to the tile just outside the footprint on their side, in line
 * with the crossing, steps onto the near footprint tile, climbs to the far one and steps off it on
 * the other side.
 */
class StileScript : PluginScript() {
    override fun ScriptContext.startup() {
        for (stile in STILES) {
            onOpLoc1(stile) { climbStile(it.loc) }
        }
    }

    private suspend fun ProtectedAccess.climbStile(loc: BoundLocInfo) {
        val south = loc.coords
        val north = south.translate(loc.adjustedWidth - 1, loc.adjustedLength - 1)
        val alongZ = north.z != south.z
        val fromSouth = if (alongZ) coords.z <= south.z else coords.x <= south.x
        val (start, dest) = if (fromSouth) south to north else north to south
        val stepX = dest.x - start.x
        val stepZ = dest.z - start.z
        val approach = start.translate(-stepX, -stepZ)
        val exit = dest.translate(stepX, stepZ)
        if (coords != approach) {
            playerMoveWithMinDelay(approach)
            while (coords != approach && !player.routeDestination.isEmpty()) {
                delay(1)
            }
            if (coords != approach) return
        }
        stepTo(start)
        delay(STEP_ON_TICKS)
        climbOver(start, dest)
        delay(CLIMB_OVER_TICKS)
        stepTo(exit)
    }

    private fun ProtectedAccess.stepTo(dest: CoordGrid) {
        teleport(dest, TeleportType.Exempt)
    }

    private companion object {
        private const val STEP_ON_TICKS = 2
        private val STILES = listOf("loc.fullstyle", "loc.qip_sheep_shearer_fullstyle")
    }
}
