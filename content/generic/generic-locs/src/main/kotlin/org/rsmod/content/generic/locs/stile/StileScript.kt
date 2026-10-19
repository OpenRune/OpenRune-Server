package org.rsmod.content.generic.locs.stile

import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.script.onOpLoc1
import org.rsmod.game.loc.BoundLocInfo
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

/**
 * Steps over a fence stile. A stile spans the two tiles of its own footprint, one either side of
 * the fence, and climbing it moves the player to whichever of those tiles they are not standing on.
 */
class StileScript : PluginScript() {
    override fun ScriptContext.startup() {
        for (stile in STILES) {
            onOpLoc1(stile) { climbStile(it.loc) }
        }
    }

    private fun ProtectedAccess.climbStile(loc: BoundLocInfo) {
        val start = coords
        val far = loc.coords.translate(loc.adjustedWidth - 1, loc.adjustedLength - 1)
        val dest = if (start == far) loc.coords else far
        climbOver(start, dest)
    }

    private companion object {
        private val STILES = listOf("loc.fullstyle", "loc.qip_sheep_shearer_fullstyle")
    }
}
