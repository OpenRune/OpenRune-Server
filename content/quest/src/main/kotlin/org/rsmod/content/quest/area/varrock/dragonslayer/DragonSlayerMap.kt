package org.rsmod.content.quest.area.varrock.dragonslayer

import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.script.onOpHeld1
import org.rsmod.api.script.onOpHeldU
import org.rsmod.content.quest.area.varrock.dragonslayer.DragonSlayerQuest.Companion.CRANDOR_MAP
import org.rsmod.content.quest.area.varrock.dragonslayer.DragonSlayerQuest.Companion.LOZAR_PIECE
import org.rsmod.content.quest.area.varrock.dragonslayer.DragonSlayerQuest.Companion.MELZAR_PIECE
import org.rsmod.content.quest.area.varrock.dragonslayer.DragonSlayerQuest.Companion.THALZAR_PIECE
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

/** Studying the three map parts and joining them into the Crandor map. */
class DragonSlayerMap : PluginScript() {
    override fun ScriptContext.startup() {
        onOpHeld1(MELZAR_PIECE) { study(MELZAR_PIECE, "that you found in Melzar's Maze.") }
        onOpHeld1(THALZAR_PIECE) {
            study(THALZAR_PIECE, "that you found in a secret chest in the Dwarven Mine.")
        }
        onOpHeld1(LOZAR_PIECE) { study(LOZAR_PIECE, "that you got from Wormbrain, the goblin thief.") }
        onOpHeld1(CRANDOR_MAP) {
            startDialogue { objbox(CRANDOR_MAP, "A map of the route to Crandor.") }
        }
        onOpHeldU(MELZAR_PIECE, THALZAR_PIECE) { combine() }
        onOpHeldU(MELZAR_PIECE, LOZAR_PIECE) { combine() }
        onOpHeldU(THALZAR_PIECE, LOZAR_PIECE) { combine() }
    }

    private suspend fun ProtectedAccess.study(piece: String, source: String) {
        startDialogue {
            objbox(
                piece,
                "This is a piece of map $source You will need to join it to the other two map " +
                    "pieces before you can see the route to Crandor.",
            )
        }
    }

    private suspend fun ProtectedAccess.combine() {
        val combined =
            player.exchange(
                take = listOf(MELZAR_PIECE to 1, THALZAR_PIECE to 1, LOZAR_PIECE to 1),
                give = listOf(CRANDOR_MAP to 1),
            )
        if (!combined) {
            mes("You still need one more piece of map.")
            return
        }
        startDialogue {
            objbox(
                CRANDOR_MAP,
                "You put the three pieces together and assemble a map that shows the route " +
                    "through the reefs to Crandor.",
            )
        }
    }
}
