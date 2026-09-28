package org.rsmod.content.areas.city.lumbridge

import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.stat.agilityLvl
import org.rsmod.api.script.onApLoc1
import org.rsmod.game.loc.BoundLocInfo
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class LumbridgeSteppingStone : PluginScript() {
    override fun ScriptContext.startup() {
        onApLoc1("loc.lumbridge_diary_desert_shortcut") { apCross(it.loc) }
    }

    private suspend fun ProtectedAccess.apCross(stone: BoundLocInfo) {
        if (isWithinApRange(stone, distance = STONE_AP_RANGE)) {
            cross()
        }
    }

    private suspend fun ProtectedAccess.cross() {
        if (player.agilityLvl < STONE_AGILITY) {
            mes("You need an Agility level of $STONE_AGILITY to negotiate this obstacle.")
            return
        }
        val north = coords.z > STONE.z
        val dest = if (north) SOUTH_BANK else NORTH_BANK
        val facing = if (north) FACE_SOUTH else FACE_NORTH

        exactMove(coords, STONE, delay1 = HOP1_START_CYCLES, delay2 = HOP1_END_CYCLES, dir = facing)
        anim("seq.human_spot_jump", delay = HOP1_ANIM_DELAY)
        soundSynth(JUMP_SOUND, delay = HOP1_SOUND_DELAY)
        delay(2)

        exactMove(coords, dest, delay1 = HOP2_START_CYCLES, delay2 = HOP2_END_CYCLES, dir = facing)
        anim("seq.human_spot_jump")
        soundSynth(JUMP_SOUND, delay = HOP2_SOUND_DELAY)
        delay(1)
    }

    private companion object {
        const val STONE_AGILITY = 66
        const val STONE_AP_RANGE = 4

        const val HOP1_START_CYCLES = 30
        const val HOP1_END_CYCLES = 45
        const val HOP1_ANIM_DELAY = 15
        const val HOP1_SOUND_DELAY = 20

        const val HOP2_START_CYCLES = 15
        const val HOP2_END_CYCLES = 28
        const val HOP2_SOUND_DELAY = 5

        const val FACE_SOUTH = 0
        const val FACE_NORTH = 1024

        const val JUMP_SOUND = "synth.varrock_fence_jump"

        val NORTH_BANK = CoordGrid(3212, 3138, 0)
        val STONE = CoordGrid(3214, 3135, 0)
        val SOUTH_BANK = CoordGrid(3214, 3131, 0)
    }
}
