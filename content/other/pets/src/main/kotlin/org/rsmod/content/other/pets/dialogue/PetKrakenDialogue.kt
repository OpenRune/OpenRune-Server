package org.rsmod.content.other.pets.dialogue

import jakarta.inject.Inject
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.content.other.pets.onPetOp
import org.rsmod.game.entity.Npc
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class PetKrakenDialogue @Inject constructor() : PluginScript() {
    override fun ScriptContext.startup() {
        onPetOp(NPC, "Talk-to") { talk(it) }
    }

    private suspend fun ProtectedAccess.talk(npc: Npc) =
        startDialogue(npc) {
            chatPlayer(laugh, "What's Kraken?")
            chatNpc(bored, "Not heard that one before.")
            chatPlayer(quiz, "How are you actually walking on land?")
            chatNpc(
                neutral,
                "We have another leg, just below the center of our body that we use to move across " +
                    "solid surfaces.",
            )
            chatPlayer(confused, "That's.... interesting.")
        }

    private companion object {
        const val NPC = "npc.kraken_pet"
    }
}
