package org.rsmod.content.other.pets.dialogue

import jakarta.inject.Inject
import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.content.other.pets.onPetOp
import org.rsmod.game.entity.Npc
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class IkkleHydraDialogue @Inject constructor() : PluginScript() {
    override fun ScriptContext.startup() {
        onPetOp(SERPENTINE, "Talk-to") { talkSerpentine(it) }
        onPetOp(ELECTRIC, "Talk-to") { talkOther(it) }
        onPetOp(FIRE, "Talk-to") { talkOther(it) }
        onPetOp(EXTINGUISHED, "Talk-to") { talkOther(it) }
    }

    private suspend fun ProtectedAccess.talkSerpentine(npc: Npc) =
        startDialogue(npc) {
            when (access.random.of(3)) {
                0 -> {
                    chatPlayer(quiz, "How do you ever agree on what to do?")
                    chatNpcSpecific("Right-most head", SERPENTINE, neutral, "Well I'm in charge.")
                    chatNpcSpecific("Left-most head", SERPENTINE, angry, "No, I am.")
                    chatNpcSpecific(
                        "2nd head from the left",
                        SERPENTINE,
                        angry,
                        "Nope, I say what we do.",
                    )
                    chatNpcSpecific("Central head", SERPENTINE, verymad, "Really I control everything!")
                    chatPlayer(worried, "Ok then!")
                }
                1 -> chemicalBath()
                else -> coolestCreature()
            }
        }

    private suspend fun ProtectedAccess.talkOther(npc: Npc) =
        startDialogue(npc) {
            when (access.random.of(3)) {
                0 -> {
                    chatPlayer(quiz, "So... you're alchemical; does that mean I can turn you into gold?")
                    chatNpc(neutral, "No, I like my form as it is.")
                    chatPlayer(sad, "But... I love gold.")
                    chatNpc(shifty, "I'll turn to gold when drakes fly.")
                }
                1 -> chemicalBath()
                else -> coolestCreature()
            }
        }

    private suspend fun Dialogue.chemicalBath() {
        chatPlayer(happy, "So how was that chemical bath?!")
        chatNpc(angry, "Nasty! I don't want to do that again")
    }

    private suspend fun Dialogue.coolestCreature() {
        chatPlayer(quiz, "Which creature is cooler, Drakes, Hydras or Wyrms?")
        chatNpc(neutral, "I may be biased in this situation... bit silly to ask me isn't it?")
        chatPlayer(happy, "Alright alright... don't lose your head.")
    }

    private companion object {
        const val SERPENTINE = "npc.hydra_pet"
        const val ELECTRIC = "npc.hydra_pet_electric"
        const val FIRE = "npc.hydra_pet_fire"
        const val EXTINGUISHED = "npc.hydra_pet_extinguished"
    }
}
