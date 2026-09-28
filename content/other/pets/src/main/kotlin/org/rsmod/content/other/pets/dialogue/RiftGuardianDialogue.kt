package org.rsmod.content.other.pets.dialogue

import jakarta.inject.Inject
import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.content.other.pets.onPetOp
import org.rsmod.game.entity.Npc
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class RiftGuardianDialogue @Inject constructor() : PluginScript() {
    override fun ScriptContext.startup() {
        for (npc in RIFT_FORMS) {
            onPetOp(npc, "Talk-to") { talk(it) }
        }
        onPetOp(GREATISH, "Talk-to") { talkGreatish(it) }
    }

    private suspend fun ProtectedAccess.talk(npc: Npc) =
        startDialogue(npc) {
            val choice =
                choice5(
                    "Can you see your own rift?",
                    1,
                    "Where would you like to go today, Rifty?",
                    2,
                    "Hey! What's that?",
                    3,
                    "About your colour-changing...",
                    4,
                    "Actually, never mind.",
                    5,
                )
            when (choice) {
                1 -> {
                    chatPlayer(quiz, "Can you see your own rift?")
                    chatNpc(
                        neutral,
                        "No. From time to time I feel it shift and change inside me though. It is " +
                            "an odd feeling.",
                    )
                }
                2 -> {
                    chatPlayer(happy, "Where would you like me to take you today Rifty?")
                    chatNpc(
                        angry,
                        "Please do not call me that... we are a species of honour, " +
                            "${player.displayName}.",
                    )
                    chatPlayer(sad, "Sorry.")
                }
                3 -> {
                    chatPlayer(shocked, "Hey! What's that!")
                    mesbox("You quickly poke your hand through the rift guardian's rift.")
                    chatNpc(shocked, "Huh, what?! Where?")
                    chatPlayer(laugh, "Not the best guardian it seems.")
                }
                4 -> colourChanging()
                else -> chatPlayer(neutral, "Actually, never mind.")
            }
        }

    private suspend fun ProtectedAccess.talkGreatish(npc: Npc) =
        startDialogue(npc) {
            val choice =
                choice3(
                    "How are you doing today?",
                    1,
                    "About your colour-changing...",
                    2,
                    "Actually, never mind.",
                    3,
                )
            when (choice) {
                1 -> {
                    chatPlayer(quiz, "How are you doing today?")
                    chatNpc(
                        worried,
                        "The power of the Abyss grows. I fear for what will happen if our defence " +
                            "against it fails.",
                    )
                    chatPlayer(
                        happy,
                        "The Guardians of the Rift have things under control. You don't need to " +
                            "worry.",
                    )
                    chatNpc(neutral, "Let us hope you are correct.")
                }
                2 -> colourChanging()
                else -> chatPlayer(neutral, "Actually, never mind.")
            }
        }

    private suspend fun Dialogue.colourChanging() {
        chatPlayer(quiz, "About your colour-changing...")
        chatNpc(
            neutral,
            "If you take me to a Runecrafting Altar, I will adopt the altar's colour as you bind " +
                "essence there.",
        )
        val choice =
            choice2(
                "I want you to stay this colour instead.",
                1,
                "Okay, carry on adopting the colour of altars.",
                2,
            )
        when (choice) {
            1 -> {
                chatPlayer(neutral, "I want you to stay this colour instead.")
                chatNpc(
                    neutral,
                    "Very well, I will remain locked to this colour, even if you bind essence at a " +
                        "Runecrafting Altar.",
                )
            }
            else -> {
                chatPlayer(neutral, "Okay, carry on adopting the colour of altars.")
                chatNpc(
                    neutral,
                    "Very well, I will adopt the altar's colour next time you bind essence at a " +
                        "Runecrafting Altar.",
                )
            }
        }
    }

    private companion object {
        val RIFT_FORMS =
            listOf(
                "npc.skillpet_runecrafting_fire",
                "npc.skillpet_runecrafting_air",
                "npc.skillpet_runecrafting_mind",
                "npc.skillpet_runecrafting_water",
                "npc.skillpet_runecrafting_earth",
                "npc.skillpet_runecrafting_body",
                "npc.skillpet_runecrafting_cosmic",
                "npc.skillpet_runecrafting_chaos",
                "npc.skillpet_runecrafting_nature",
                "npc.skillpet_runecrafting_law",
                "npc.skillpet_runecrafting_death",
                "npc.skillpet_runecrafting_soul",
                "npc.skillpet_runecrafting_astral",
                "npc.skillpet_runecrafting_blood",
                "npc.skillpet_runecrafting_wrath",
            )
        const val GREATISH = "npc.skillpet_runecrafting_gotr"
    }
}
