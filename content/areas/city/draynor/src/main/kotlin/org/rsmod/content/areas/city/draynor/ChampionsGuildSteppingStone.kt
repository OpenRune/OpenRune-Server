package org.rsmod.content.areas.city.draynor

import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.stat.agilityLvl
import org.rsmod.api.script.onApLoc1
import org.rsmod.game.loc.BoundLocInfo
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class ChampionsGuildSteppingStone : PluginScript() {
    override fun ScriptContext.startup() {
        onApLoc1("loc.lumbridge_sc_stepstone") { apCross(it.loc) }
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
        val east = coords.x > STONE.x
        val dest = if (east) WEST_BANK else EAST_BANK
        val facing = if (east) FACE_WEST else FACE_EAST

        spam("You attempt to balance on the stepping stone.")

        hop(STONE, facing)
        delay(HOP_TICKS)

        hop(dest, facing)
        statAdvance(AGILITY, SUCCESS_XP)
        delay(HOP_TICKS)

        spam("You manage to make it to the other side of the river.")
    }

    private fun ProtectedAccess.hop(to: CoordGrid, facing: Int) {
        exactMove(coords, to, delay1 = HOP_START_CYCLES, delay2 = HOP_END_CYCLES, dir = facing)
        anim("seq.human_steppingstonejump", delay = HOP_ANIM_DELAY)
        soundSynth(JUMP_SOUND, delay = HOP_SOUND_DELAY)
    }

    private companion object {
        const val STONE_AGILITY = 31
        const val STONE_AP_RANGE = 3
        const val SUCCESS_XP = 3.0

        const val HOP_START_CYCLES = 47
        const val HOP_END_CYCLES = 59
        const val HOP_ANIM_DELAY = 19
        const val HOP_SOUND_DELAY = 59
        const val HOP_TICKS = 3

        const val FACE_WEST = 512
        const val FACE_EAST = 1536

        const val AGILITY = "stat.agility"
        const val JUMP_SOUND = "synth.varrock_fence_jump"

        val WEST_BANK = CoordGrid(3149, 3363, 0)
        val STONE = CoordGrid(3151, 3363, 0)
        val EAST_BANK = CoordGrid(3154, 3363, 0)
    }
}
