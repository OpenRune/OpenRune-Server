package org.rsmod.content.areas.city.varrock.scripts

import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.stat.agilityLvl
import org.rsmod.api.script.onOpLoc1
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class VarrockShortcutScript : PluginScript() {
    override fun ScriptContext.startup() {
        onOpLoc1("loc.lumbridge_sc_fencejump") { jumpFence() }
    }

    private suspend fun ProtectedAccess.jumpFence() {
        arriveDelay()
        if (player.agilityLvl < FENCE_AGILITY) {
            mes("You need an Agility level of $FENCE_AGILITY to negotiate this obstacle.")
            return
        }
        val north = coords.z >= FENCE_NORTH_SIDE.z
        val dest = if (north) FENCE_SOUTH_SIDE else FENCE_NORTH_SIDE
        val facing = if (north) FACE_SOUTH else FACE_NORTH
        exactMove(coords, dest, delay1 = JUMP_START_CYCLES, delay2 = JUMP_END_CYCLES, dir = facing)
        anim("seq.human_spot_jump")
        soundSynth("synth.varrock_fence_jump")
        delay(1)
    }

    private companion object {
        const val FENCE_AGILITY = 13
        const val JUMP_START_CYCLES = 15
        const val JUMP_END_CYCLES = 30
        const val FACE_SOUTH = 0
        const val FACE_NORTH = 1024
        val FENCE_SOUTH_SIDE = CoordGrid(3240, 3334, 0)
        val FENCE_NORTH_SIDE = CoordGrid(3240, 3335, 0)
    }
}
