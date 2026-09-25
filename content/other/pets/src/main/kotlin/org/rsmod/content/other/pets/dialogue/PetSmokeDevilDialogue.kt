package org.rsmod.content.other.pets.dialogue

import jakarta.inject.Inject
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.content.other.pets.onPetOp
import org.rsmod.game.entity.Npc
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class PetSmokeDevilDialogue @Inject constructor() : PluginScript() {
    override fun ScriptContext.startup() {
        onPetOp(NPC, "Talk-to") { talk(it) }
        onPetOp(NPC_THERMONUCLEAR, "Talk-to") { talk(it) }
    }

    private suspend fun ProtectedAccess.talk(npc: Npc) =
        startDialogue(npc) {
            chatPlayer(quiz, "Are you safe to keep as a pet?")
            chatNpc(neutral, "Yes, human.")
            val choice =
                choice4(
                    "So where's the fire?",
                    1,
                    "So you don't cause lung disease?",
                    2,
                    "So you don't leave soot everywhere?",
                    3,
                    "Never mind.",
                    4,
                )
            when (choice) {
                1 -> {
                    chatPlayer(quiz, "So where's the fire?")
                    chatNpc(neutral, "I don't have a fire.")
                    chatPlayer(laugh, "No smoke without fire!")
                    chatNpc(bored, "Please stop, human.")
                }
                2 -> {
                    chatPlayer(quiz, "So you don't cause lung disease?")
                    chatNpc(neutral, "We're safe if you don't inhale.")
                    chatPlayer(happy, "Good to know.")
                }
                3 -> {
                    chatPlayer(quiz, "So you don't leave soot everywhere?")
                    chatNpc(
                        angry,
                        "Would you defecate on the floor like a camel? Neither would I. So no, I do not " +
                            "leave soot everywhere.",
                    )
                    chatPlayer(happy, "That's nice of you.")
                }
                else -> chatPlayer(neutral, "Never mind.")
            }
        }

    private companion object {
        const val NPC = "npc.smoke_pet"
        const val NPC_THERMONUCLEAR = "npc.smoke_pet_old"
    }
}
