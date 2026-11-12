package org.rsmod.content.generic.locs.passage

import dev.openrune.types.ObjectServerType
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.script.onOpLoc1
import org.rsmod.game.loc.BoundLocInfo
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

/**
 * Large gates and doors with no open model of their own, such as the Tree Gnome Stronghold's main
 * gate and the Grand Tree's doors: opening one walks the player through to the far side.
 */
class PassageGateScript : PluginScript() {
    override fun ScriptContext.startup() {
        for (loc in PASSAGES) {
            onOpLoc1(loc) { passThrough(it.loc, it.type) }
        }
    }

    private suspend fun ProtectedAccess.passThrough(loc: BoundLocInfo, type: ObjectServerType) {
        val dest =
            PassageCrossing.across(coords, loc.coords, type.width, type.length, loc.angleId)
                ?: return
        arriveDelay()
        teleport(dest)
    }

    private companion object {
        val PASSAGES = listOf("loc.gnome_areagate", "loc.treedoorl", "loc.treedoorr")
    }
}
