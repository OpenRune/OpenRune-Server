package org.rsmod.content.other.pets.dialogue

import jakarta.inject.Inject
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.content.other.pets.onPetOp
import org.rsmod.game.entity.Npc
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class PetPenanceQueenDialogue @Inject constructor() : PluginScript() {
    override fun ScriptContext.startup() {
        onPetOp(NPC, "Talk-to") { talk(it) }
    }

    private suspend fun ProtectedAccess.talk(npc: Npc) =
        startDialogue(npc) {
            chatPlayer(sad, "Of all the high gamble rewards I could have won, I won you...")
            chatNpc(laugh, "Keep trying, human. You'll never win that Dragon Chainbody.")
        }

    private companion object {
        const val NPC = "npc.penance_pet"
    }
}
