package org.rsmod.content.areas.wilderness.locs

import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.stat.agilityLvl
import org.rsmod.api.script.onOpLoc1
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

/**
 * The tight gap in the rocks west of the Chaos Temple. Both of its tiles block movement, so the
 * three steps through are forced rather than routed, bookended by the sidestep animations.
 */
class TightGapShortcut : PluginScript() {
    override fun ScriptContext.startup() {
        onOpLoc1("loc.burthorpe_diary_shortcut") { squeeze() }
    }

    private suspend fun ProtectedAccess.squeeze() {
        arriveDelay()
        if (player.agilityLvl < GAP_AGILITY) {
            mes("You need an Agility level of $GAP_AGILITY to negotiate this obstacle.")
            return
        }
        val westbound = coords.z < WEST_END.z
        val path = if (westbound) WESTBOUND else EASTBOUND

        anim(ENTER_SEQ)
        soundSynth("synth.tight_gap_squeeze", loops = SQUEEZE_LOOPS)
        var from = coords
        for (step in path) {
            exactMove(from, step, 0, STEP_CYCLES, facing(from, step))
            delay(1)
            from = step
        }
        anim(LEAVE_SEQ)
    }

    private fun facing(from: CoordGrid, to: CoordGrid): Int {
        val north = to.z > from.z
        val south = to.z < from.z
        val east = to.x > from.x
        val west = to.x < from.x
        return when {
            north && west -> FACE_NORTH_WEST
            north && east -> FACE_NORTH_EAST
            south && west -> FACE_SOUTH_WEST
            south && east -> FACE_SOUTH_EAST
            north -> FACE_NORTH
            south -> FACE_SOUTH
            east -> FACE_EAST
            else -> FACE_WEST
        }
    }

    private companion object {
        private const val GAP_AGILITY = 14
        private const val ENTER_SEQ = "seq.human_into_sidestepl"
        private const val LEAVE_SEQ = "seq.human_outof_sidestepl"
        private const val SQUEEZE_LOOPS = 3
        private const val STEP_CYCLES = 30

        private const val FACE_SOUTH = 0
        private const val FACE_SOUTH_WEST = 256
        private const val FACE_WEST = 512
        private const val FACE_NORTH_WEST = 768
        private const val FACE_NORTH = 1024
        private const val FACE_NORTH_EAST = 1280
        private const val FACE_EAST = 1536
        private const val FACE_SOUTH_EAST = 1792

        private val SOUTH_END = CoordGrid(2928, 3520, 0)
        private val WEST_END = CoordGrid(2926, 3522, 0)

        private val WESTBOUND =
            listOf(CoordGrid(2928, 3521, 0), CoordGrid(2927, 3522, 0), WEST_END)
        private val EASTBOUND =
            listOf(CoordGrid(2927, 3522, 0), CoordGrid(2928, 3521, 0), SOUTH_END)
    }
}
