package org.rsmod.content.bosses.araxxor

import org.rsmod.api.player.hook.TeleportType
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.stat.hitpoints
import org.rsmod.api.script.onOpLoc1
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

/** The public araxyte cave is separate from Araxxor's private, task-locked arena. */
internal class SpiderCaveTravel : PluginScript() {
    override fun ScriptContext.startup() {
        onOpLoc1("loc.araxyte_cave_entry") { pass(INNER, "seq.human_climbing_down") }
        onOpLoc1("loc.araxyte_cave_exit") { pass(OUTER, "seq.human_climbing") }
    }

    private suspend fun ProtectedAccess.pass(destination: CoordGrid, animation: String) {
        if (player.pendingLogout || player.loggingOut || player.hitpoints == 0) return
        val origin = coords
        arriveDelay()
        anim(animation)
        try {
            delay(2)
            if (coords != origin || player.pendingLogout || player.loggingOut || player.hitpoints == 0) return
            resetAnim()
            telejump(destination, TeleportType.Exempt)
        } finally {
            resetAnim()
        }
    }

    internal companion object {
        val INNER = CoordGrid(3679, 9797)
        val OUTER = CoordGrid(3658, 3403)
    }
}
