package org.rsmod.content.generic.locs.stile

import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.script.onOpLoc1
import org.rsmod.game.loc.BoundLocInfo
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

/**
 * Steps over a fence stile. A stile spans the two tiles of its own footprint, one either side of
 * the fence: the player walks onto the tile on their side, then climbs to the other.
 */
class StileScript : PluginScript() {
    override fun ScriptContext.startup() {
        for (stile in STILES) {
            onOpLoc1(stile) { climbStile(it.loc) }
        }
    }

    private suspend fun ProtectedAccess.climbStile(loc: BoundLocInfo) {
        val near = loc.coords
        val far = near.translate(loc.adjustedWidth - 1, loc.adjustedLength - 1)
        val fromNear = if (far.z != near.z) coords.z <= near.z else coords.x <= near.x
        val (start, dest) = if (fromNear) near to far else far to near
        if (coords != start) {
            playerMove(start)
            if (coords != start) return
        }
        climbOver(start, dest)
    }

    private companion object {
        private val STILES = listOf("loc.fullstyle", "loc.qip_sheep_shearer_fullstyle")
    }
}
