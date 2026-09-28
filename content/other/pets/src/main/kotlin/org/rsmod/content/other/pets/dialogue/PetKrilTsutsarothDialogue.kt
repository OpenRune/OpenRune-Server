package org.rsmod.content.other.pets.dialogue

import jakarta.inject.Inject
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.content.other.pets.onPetOp
import org.rsmod.game.entity.Npc
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class PetKrilTsutsarothDialogue @Inject constructor() : PluginScript() {
    override fun ScriptContext.startup() {
        onPetOp(NPC, "Talk-to") { talk(it) }
    }

    private suspend fun ProtectedAccess.talk(npc: Npc) =
        startDialogue(npc) {
            chatPlayer(quiz, "How's life in the light?")
            chatNpc(neutral, "Burns slightly.")
            chatPlayer(neutral, "You seem much nicer than your father. He's mean.")
            chatNpc(neutral, "If you were stuck in a very dark cave for centuries you'd be pretty annoyed too.")
            chatPlayer(neutral, "I guess.")
            chatNpc(happy, "He's actually quite mellow really.")
            chatPlayer(confused, "Uh.... Yeah.")
        }

    private companion object {
        const val NPC = "npc.zamorak_pet"
    }
}
