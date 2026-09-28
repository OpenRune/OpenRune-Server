package org.rsmod.content.other.pets.dialogue

import jakarta.inject.Inject
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.content.other.pets.onPetOp
import org.rsmod.game.entity.Npc
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class PetSnakelingDialogue @Inject constructor() : PluginScript() {
    override fun ScriptContext.startup() {
        for (npc in NPCS) {
            onPetOp(npc, "Talk-to") { talk(it) }
        }
    }

    private suspend fun ProtectedAccess.talk(npc: Npc) =
        startDialogue(npc) {
            chatPlayer(happy, "Hey little snake!")
            chatNpc(neutral, "Soon, Zulrah shall establish dominion over this plane.")
            chatPlayer(happy, "Wanna play fetch?")
            chatNpc(neutral, "Submit to the almighty Zulrah.")
            chatPlayer(quiz, "Walkies? Or slidies...?")
            chatNpc(neutral, "Zulrah's wilderness as a God will soon be demonstrated.")
            chatPlayer(sad, "I give up...")
        }

    private companion object {
        val NPCS = listOf("npc.snake_pet_green", "npc.snake_pet_blue", "npc.snake_pet_orange")
    }
}
