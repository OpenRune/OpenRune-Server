package org.rsmod.content.other.pets.dialogue

import jakarta.inject.Inject
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.content.other.pets.onPetOp
import org.rsmod.game.entity.Npc
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class VetionJrDialogue @Inject constructor() : PluginScript() {
    override fun ScriptContext.startup() {
        for (npc in FORMS) {
            onPetOp(npc, "Talk-to") { talk(it) }
        }
    }

    private suspend fun ProtectedAccess.talk(npc: Npc) =
        startDialogue(npc) {
            chatPlayer(quiz, "Who is the true lord and king of the lands?")
            chatNpc(neutral, "The mighty heir and lord of the Wilderness.")
            chatPlayer(quiz, "Where is he? Why hasn't he lifted your burden?")
            chatNpc(sad, "I have not fulfilled my purpose.")
            chatPlayer(quiz, "What is your purpose?")
            chatNpc(
                sad,
                "Not what is, what was. A great war tore this land apart and, for my failings " +
                    "in protecting this land, I carry the burden of its waste.",
            )
        }

    private companion object {
        val FORMS =
            listOf("npc.vetionpet", "npc.vetionpet_2", "npc.vetionpet_legacy", "npc.vetionpet_2_legacy")
    }
}
