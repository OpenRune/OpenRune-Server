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
        onOpLoc1("loc.goblin_ladder_bottom") { climbInPlace(LOOKOUT_LEVEL) }
        onOpLoc1("loc.goblin_ladder_top") { climbInPlace(0) }
    }

    /** The lookout by the south gate: its ladder joins level 0 and level 2 on the same tiles. */
    private suspend fun ProtectedAccess.climbInPlace(level: Int) {
        arriveDelay()
        anim("seq.human_reachforladder")
        delay(1)
        telejump(CoordGrid(player.coords.x, player.coords.z, level))
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
        private const val LOOKOUT_LEVEL = 2

        private val UP_LADDERS =
            listOf("loc.100_goblin_ladder_up", "loc.100_goblin_ladder_up_charred")
    }
}
