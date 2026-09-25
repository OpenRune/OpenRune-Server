package org.rsmod.content.other.pets.dialogue

import jakarta.inject.Inject
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.content.other.pets.onPetOp
import org.rsmod.game.entity.Npc
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class OlmletDialogue @Inject constructor() : PluginScript() {
    override fun ScriptContext.startup() {
        onPetOp(OLMLET, "Talk-to") { talkOlmlet(it) }
        onPetOp(PUPPADILE, "Talk-to") { talkPuppadile(it) }
        onPetOp(TEKTINY, "Talk-to") { talkTektiny(it) }
        onPetOp(ENRAGED_TEKTINY, "Talk-to") { talkTektiny(it) }
        onPetOp(VANGUARD, "Talk-to") { talkVanguard(it) }
        onPetOp(VASA_MINIRIO, "Talk-to") { talkVasaMinirio(it) }
        onPetOp(VESPINA, "Talk-to") { talkVespina(it) }
        onPetOp(FLYING_VESPINA, "Talk-to") { talkVespina(it) }
    }

    private suspend fun ProtectedAccess.talkOlmlet(npc: Npc) =
        startDialogue(npc) {
            chatNpc(happy, "Hee hee! What shall we talk about, human?")
            while (true) {
                val choice =
                    choice4(
                        "Where do creatures like you come from?",
                        1,
                        "You look like a dragon.",
                        2,
                        "Can you tell me secrets about your home?",
                        3,
                        "Maybe another time.",
                        4,
                    )
                when (choice) {
                    1 -> {
                        chatPlayer(quiz, "Where do creatures like you come from?")
                        chatNpc(
                            happy,
                            "From eggs, of course! You can't make an olmlet without breaking an egg.",
                        )
                        chatPlayer(neutral, "That's... informative. Thank you.")
                        chatNpc(happy, "Hee hee! What's next, human?")
                    }
                    2 -> {
                        chatPlayer(neutral, "You look like a dragon.")
                        chatNpc(
                            angry,
                            "And humans look like monkeys. Badly shaved monkeys. What's your point, human?",
                        )
                        chatPlayer(quiz, "Are you related to dragons?")
                        chatNpc(
                            angry,
                            "My sire was an olm. I'm an olm. I don't go around asking you about your " +
                                "parents' species, do I?",
                        )
                        chatPlayer(neutral, "... no, I suppose you don't.")
                        chatNpc(
                            happy,
                            "Hee hee! Let's change the subject before someone gets insulted. What shall " +
                                "we talk about instead, human?",
                        )
                    }
                    3 -> {
                        chatPlayer(quiz, "Can you tell me secrets about your home?")
                        chatNpc(
                            happy,
                            "Ooh, it was lovely. I lived in an eggshell. I was safe in there, dreaming of " +
                                "the life I would lead when I hatched, and the caverns I could rule.",
                        )
                        chatNpc(
                            worried,
                            "Then suddenly I felt a trembling of the ground, and my shell shattered.",
                        )
                        chatNpc(
                            sad,
                            "Through its cracks I saw the world for the first time, just in time to " +
                                "watch my sire die.",
                        )
                        chatNpc(
                            neutral,
                            "It was a terrible shock for a newly hatched olmlet, but I try not to let it " +
                                "affect my mood. So what else shall we talk about, human?",
                        )
                    }
                    else -> {
                        chatPlayer(neutral, "Maybe another time.")
                        return@startDialogue
                    }
                }
            }
        }

    private suspend fun ProtectedAccess.talkPuppadile(npc: Npc) =
        startDialogue(npc) {
            when (access.random.of(3)) {
                0 -> {
                    chatPlayer(happy, "What lovely teeth you have!")
                    chatNpc(neutral, "All the better for chomping stuff.")
                    chatPlayer(shocked, "WOW! No chill?")
                    chatNpc(neutral, "Hey, its a dog-eat-dogadile world out there!")
                }
                1 -> {
                    chatPlayer(happy, "Want to play fetch?")
                    chatNpc(happy, "Play meat!")
                    chatPlayer(neutral, "No, fetch... you know, I throw stick and you bring it back?")
                    chatNpc(neutral, "You fetch meat.")
                    chatPlayer(angry, "It doesn't just grow on trees you know!")
                    chatNpc(neutral, "...")
                }
                else -> chatNpc(happy, "Om nom nom good meat.")
            }
        }

    private suspend fun ProtectedAccess.talkTektiny(npc: Npc) =
        startDialogue(npc) {
            when (access.random.of(2)) {
                0 -> {
                    chatPlayer(neutral, "You look hot.")
                    chatNpc(neutral, "Heat required for forge.")
                    chatPlayer(quiz, "What are you making?")
                    chatNpc(neutral, "Heat.")
                    chatPlayer(neutral, "Well, forge ahead then.")
                }
                else -> {
                    chatPlayer(quiz, "What are you-")
                    chatNpc(angry, "STOP!")
                    chatPlayer(confused, "???")
                    chatNpc(happy, "HAMMER TIME!")
                }
            }
        }

    private suspend fun ProtectedAccess.talkVanguard(npc: Npc) =
        startDialogue(npc) {
            chatNpc(neutral, "Learn ye of the Judgement of the Vanguard. The form we have taken.")
            chatPlayer(happy, "But you're so little and cute!")
            chatNpc(
                angry,
                "Learn ye this form was not given from the grace of our Lord Xeric. He does not " +
                    "forgive those who choose poorly.",
            )
        }

    private suspend fun ProtectedAccess.talkVasaMinirio(npc: Npc) =
        startDialogue(npc) {
            chatNpc(angry, "The Dark Altar! The power it has given. Xeric, you cannot comprehend!")
            chatPlayer(confused, "Excuse me?")
            chatNpc(
                madlaugh,
                "I will take Kourend for myself, for I am no longer the priest. I am the god!",
            )
            chatPlayer(worried, "What have I done...")
        }

    private suspend fun ProtectedAccess.talkVespina(npc: Npc) =
        startDialogue(npc) {
            when (access.random.of(2)) {
                0 -> {
                    chatPlayer(happy, "Hello")
                    chatNpc(neutral, "Bzzzzt!")
                }
                else -> {
                    chatPlayer(quiz, "Bzzz bzzz bzz?")
                    chatNpc(angry, "Buzz off human.")
                    chatPlayer(
                        angry,
                        "It's you that's following me! Maybe I should invest in a large flyswat...",
                    )
                    chatNpc(angry, "Bzzzzt!")
                }
            }
        }

    private companion object {
        const val OLMLET = "npc.raids_olm_pet"
        const val PUPPADILE = "npc.dogadile_pet"
        const val TEKTINY = "npc.tekton_pet"
        const val ENRAGED_TEKTINY = "npc.tekton_enraged_pet"
        const val VANGUARD = "npc.vanguard_pet"
        const val VASA_MINIRIO = "npc.vasa_pet"
        const val VESPINA = "npc.vespula_pet"
        const val FLYING_VESPINA = "npc.vespula_flying_pet"
    }
}
