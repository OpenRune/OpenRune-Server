package org.rsmod.content.areas.wilderness.locs

import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.script.onOpLoc1
import org.rsmod.game.loc.BoundLocInfo
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

/**
 * The ditch runs east to west and its footprint is two tiles deep, so the two tiles a player can
 * stand on are one south of it and one north of it. Crossing lands on whichever one they left.
 */
class WildernessDitchScript : PluginScript() {
    override fun ScriptContext.startup() {
        onOpLoc1("loc.ditch_wilderness_cover") { cross(it.loc) }
    }

    private suspend fun ProtectedAccess.cross(ditch: BoundLocInfo) {
        arriveDelay()
        val northbound = coords.z <= ditch.coords.z
        val span = ditch.adjustedLength + 1
        val dest = coords.translateZ(if (northbound) span else -span)
        val facing = if (northbound) FACE_NORTH else FACE_SOUTH

        exactMove(coords, dest, LEAP_DELAY, LAND_DELAY, facing)
        anim("seq.wild_ditch_jump")
        soundSynth("synth.wilderness_ditch_jump", delay = SOUND_DELAY)
        delay(CROSS_CYCLES)
    }

    private companion object {
        private const val LEAP_DELAY = 33
        private const val LAND_DELAY = 60
        private const val SOUND_DELAY = 25
        private const val CROSS_CYCLES = 2

        private const val FACE_SOUTH = 0
        private const val FACE_NORTH = 1024
    }
}
