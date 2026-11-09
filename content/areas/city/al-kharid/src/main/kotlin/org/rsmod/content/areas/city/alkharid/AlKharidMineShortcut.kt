package org.rsmod.content.areas.city.alkharid

import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.stat.agilityLvl
import org.rsmod.api.script.onOpLoc1
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

/**
 * The rocks at the north end of the Al Kharid mine, climbed to cross between the two ledges.
 * The climbing sound is the one Lumbridge already names for its raft.
 */
class AlKharidMineShortcut : PluginScript() {
    override fun ScriptContext.startup() {
        onOpLoc1("loc.alkharid_mine_sc_bottom") { climb(EAST_LEDGE) }
        onOpLoc1("loc.alkharid_mine_sc_top") { climb(WEST_LEDGE) }
    }

    private suspend fun ProtectedAccess.climb(dest: CoordGrid) {
        if (player.agilityLvl < AGILITY_LEVEL) {
            mes("You need an Agility level of $AGILITY_LEVEL to negotiate this obstacle.")
            return
        }
        soundSynth(CLIMB_SYNTH, loops = CLIMB_LOOPS)
        delay(1)
        telejump(dest)
        delay(CLIMB_DELAY)
    }

    private companion object {
        private const val AGILITY_LEVEL = 38
        private const val CLIMB_SYNTH = "synth.raft_climb"
        private const val CLIMB_LOOPS = 4
        private const val CLIMB_DELAY = 3

        private val WEST_LEDGE = CoordGrid(3303, 3315, 0)
        private val EAST_LEDGE = CoordGrid(3305, 3315, 0)
    }
}
