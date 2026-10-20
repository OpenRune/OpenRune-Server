package org.rsmod.content.areas.misc.goblinvillage.locs

import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.script.onOpLoc1
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

/**
 * The cave under Goblin Village, where Mudknuckles cooks. The two ends sit far enough apart that
 * neither the plain nor the dungeon ladder translation reaches them, so both are explicit.
 */
class GoblinCaveLadders : PluginScript() {
    override fun ScriptContext.startup() {
        onOpLoc1("loc.100_goblin_ladder_down") { climb(CAVE) }
        for (ladder in UP_LADDERS) {
            onOpLoc1(ladder) { climb(SURFACE) }
        }
    }

    private suspend fun ProtectedAccess.climb(dest: CoordGrid) {
        arriveDelay()
        anim("seq.human_reachforladder")
        delay(1)
        telejump(dest)
    }

    private companion object {
        private val SURFACE = CoordGrid(2960, 3506, 0)
        private val CAVE = CoordGrid(2981, 9875, 0)

        private val UP_LADDERS =
            listOf("loc.100_goblin_ladder_up", "loc.100_goblin_ladder_up_charred")
    }
}
