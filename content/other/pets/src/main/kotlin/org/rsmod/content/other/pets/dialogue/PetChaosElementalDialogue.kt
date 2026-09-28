package org.rsmod.content.other.pets.dialogue

import jakarta.inject.Inject
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.content.other.pets.onPetOp
import org.rsmod.game.entity.Npc
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class PetChaosElementalDialogue @Inject constructor() : PluginScript() {
    override fun ScriptContext.startup() {
        onPetOp(NPC, "Talk-to") { talk(it) }
    }

    private suspend fun ProtectedAccess.talk(npc: Npc) =
        startDialogue(npc) {
            chatPlayer(quiz, "Is it true a level 3 skiller caught one of your siblings?")
            chatNpc(
                sad,
                "Yes, they killed my mummy, kidnapped my brother, smiled about it and went to sleep.",
            )
            chatPlayer(
                happy,
                "Aww, well you have me now! I shall call you Squishy and you shall be mine and you " +
                    "shall be my Squishy.",
            )
            chatPlayer(happy, "Come on, Squishy come on, little Squishy!")
        }

    private companion object {
        const val NPC = "npc.chaos_elemental_pet"
    }
}
