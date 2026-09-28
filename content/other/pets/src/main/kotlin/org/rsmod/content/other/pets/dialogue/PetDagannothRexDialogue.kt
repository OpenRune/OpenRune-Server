package org.rsmod.content.other.pets.dialogue

import jakarta.inject.Inject
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.content.other.pets.onPetOp
import org.rsmod.game.entity.Npc
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class PetDagannothRexDialogue @Inject constructor() : PluginScript() {
    override fun ScriptContext.startup() {
        onPetOp(NPC, "Talk-to") { talk(it) }
    }

    private suspend fun ProtectedAccess.talk(npc: Npc) =
        startDialogue(npc) {
            chatPlayer(quiz, "Do you have any berserker rings?")
            chatNpc(neutral, "Nope.")
            chatPlayer(quiz, "You sure?")
            chatNpc(neutral, "Yes.")
            chatPlayer(
                shifty,
                "So, if I tipped you upside down and shook you, you'd not drop any berserker rings?",
            )
            chatNpc(neutral, "Nope.")
            chatPlayer(
                quiz,
                "What if I endlessly killed your father for weeks on end, would I get one then?",
            )
            chatNpc(neutral, "Been done by someone, nope.")
        }

    private companion object {
        const val NPC = "npc.rex_pet"
    }
}
