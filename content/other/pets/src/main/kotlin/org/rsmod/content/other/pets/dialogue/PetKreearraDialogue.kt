package org.rsmod.content.other.pets.dialogue

import jakarta.inject.Inject
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.content.other.pets.onPetOp
import org.rsmod.game.entity.Npc
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class PetKreearraDialogue @Inject constructor() : PluginScript() {
    override fun ScriptContext.startup() {
        onPetOp(NPC, "Talk-to") { talk(it) }
    }

    private suspend fun ProtectedAccess.talk(npc: Npc) =
        startDialogue(npc) {
            chatPlayer(confused, "Huh... that's odd... I thought that would be big news.")
            chatNpc(quiz, "You thought what would be big news?")
            chatPlayer(
                neutral,
                "Well there seems to be an absence of a certain ornithological piece: a headline " +
                    "regarding mass awareness of a certain avian variety.",
            )
            chatNpc(confused, "What are you talking about?")
            chatPlayer(shifty, "Oh have you not heard? It was my understanding that everyone had heard....")
            chatNpc(shocked, "Heard wha...... OH NO!!!!?!?!!?!?")
            chatPlayer(
                happy,
                "OH WELL THE BIRD, BIRD, BIRD, BIRD BIRD IS THE WORD. OH WELL THE BIRD, BIRD, BIRD, " +
                    "BIRD BIRD IS THE WORD.",
            )
            mesbox("There's a slight pause as Kree'Arra Jr. goes stiff.")
        }

    private companion object {
        const val NPC = "npc.armadyl_pet"
    }
}
