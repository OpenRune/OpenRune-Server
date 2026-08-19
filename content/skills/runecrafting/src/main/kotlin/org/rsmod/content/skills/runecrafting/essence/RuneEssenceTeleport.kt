package org.rsmod.content.skills.runecrafting.essence

import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.player.output.soundSynth
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.script.onOpLoc1
import org.rsmod.game.entity.Npc
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class EssencePortals : PluginScript() {
    override fun ScriptContext.startup() {
        onOpLoc1("loc.blankrunestone_exit_portal") { teleportToMainLand() }
    }

    companion object {

        private val RUNE_ESSENCE_MINE = CoordGrid(2910, 4832)
        private const val LANDING_RADIUS = 20

        private val AUBURY = CoordGrid(3253, 3401)
        private val SEDRIDOR = CoordGrid(3105, 9572)

        fun randomEssenceMineDest(access: ProtectedAccess): CoordGrid =
            access.mapFindSquareNone(RUNE_ESSENCE_MINE, 0, LANDING_RADIUS) ?: RUNE_ESSENCE_MINE

        fun randomizeLocation(base: CoordGrid): CoordGrid {
            if ((0..1).random() == 0) {
                return base
            }
            return CoordGrid(x = base.x + (-1..1).random(), z = base.z + (-1..1).random())
        }

        fun auburyDest(): CoordGrid = randomizeLocation(AUBURY)

        fun sedridorDest(): CoordGrid = randomizeLocation(SEDRIDOR)
    }
}

suspend fun ProtectedAccess.teleportToRuneEssenceMine(npc: Npc) {
    startDialogue(npc) { teleportToRuneEssenceMine() }
}

suspend fun ProtectedAccess.teleportToMainLand() {
    startDialogue { teleportToMainLand() }
}

suspend fun Dialogue.teleportToRuneEssenceMine() {
    val npc = checkNotNull(npc) { "Rune essence mine teleport requires an npc dialogue context." }
    val dest = EssencePortals.randomEssenceMineDest(access)

    npc.say("Seventior Disthine Molenko!")
    npc.spotanim("spotanim.curse_casting", height = 92)
    player.soundSynth("synth.curse_cast_and_fire")
    delay(1)
    npc.facePlayer(player)
    delay(1)
    npc.resetFaceEntity()
    access.spotanim("spotanim.curse_impact", delay = 15, height = 124)
    player.soundSynth("synth.curse_hit", delay = 15)
    delay(1)
    access.telejump(dest)
}

suspend fun Dialogue.teleportToMainLand() {
    access.spotanim("spotanim.curse_impact", delay = 15, height = 124)
    player.soundSynth("synth.teleport_all", delay = 15)
    delay(1)
    when (player.vars["varbit.essencemine_portal"]) {
        0 -> access.telejump(EssencePortals.sedridorDest())
        1 -> access.telejump(EssencePortals.auburyDest())
    }
}
