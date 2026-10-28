package org.rsmod.content.skills.runecrafting.essence

import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import org.rsmod.api.enums.NamedEnums.essence_mine_teleporter_exits
import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.player.output.soundSynth
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.script.onOpLoc1
import org.rsmod.content.skills.runecrafting.essenceMineTeleporter
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
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

        private const val UNRECORDED_TELEPORTER_NPC = "npc.head_wizard"

        fun randomEssenceMineDest(access: ProtectedAccess): CoordGrid =
            access.mapFindSquareNone(RUNE_ESSENCE_MINE, 0, LANDING_RADIUS) ?: RUNE_ESSENCE_MINE

        fun randomizeLocation(base: CoordGrid): CoordGrid {
            if ((0..1).random() == 0) {
                return base
            }
            return CoordGrid(
                x = base.x + (-1..1).random(),
                z = base.z + (-1..1).random(),
            )
        }

        fun checkTeleporter(npc: Npc) {
            check(exitFor(npc.type.id) != null) {
                "Npc is not a rune essence mine teleporter: ${npc.type.internalName}"
            }
        }

        fun returnExit(player: Player): CoordGrid {
            val recorded = exitFor(player.essenceMineTeleporter)
            return recorded ?: checkNotNull(exitFor(UNRECORDED_TELEPORTER_NPC.asRSCM(RSCMType.NPC)))
        }

        private fun exitFor(npcId: Int): CoordGrid? =
            essence_mine_teleporter_exits.backing.entries
                .firstOrNull { (npc, _) -> npc.id == npcId }
                ?.value
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
    EssencePortals.checkTeleporter(npc)
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
    player.essenceMineTeleporter = npc.type.id
    access.telejump(dest)
}

suspend fun Dialogue.teleportToMainLand() {
    access.spotanim("spotanim.curse_impact", delay = 15, height = 124)
    player.soundSynth("synth.teleport_all", delay = 15)
    delay(1)
    access.telejump(EssencePortals.randomizeLocation(EssencePortals.returnExit(player)))
}
