package org.rsmod.content.areas.city.desert.bedabin.npcs

import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.script.onOpNpc1
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

// TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
class BedabinTownsfolk : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1("npc.al_shabim") { startDialogue(it.npc) { alShabim() } }
        onOpNpc1("npc.bedabin_fighter") { startDialogue(it.npc) { fighter() } }
        onOpNpc1("npc.bedabin_guard") { startDialogue(it.npc) { guard() } }
        onOpNpc1("npc.fourdiamonds_indiana") { startDialogue(it.npc) { asgarniaSmith() } }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Al_Shabim (before The Tourist Trap)
    // TODO(content): [quest:the_tourist_trap] quest and after-quest dialogue
    private suspend fun Dialogue.alShabim() {
        chatNpc(happy, "Hello Effendi!")
        chatNpc(happy, "I am Al Shabim, greetings on behalf of the Bedabin nomads.")
        if (choice2("Who are you?", true, "What is this place?", false)) {
            chatPlayer(quiz, "Who are you?")
            chatNpc(happy, "I am Al Shabim Effendi! I am the leader of the Bedabin peoples!")
            val option =
                choice3(
                    "Okay thanks!",
                    1,
                    "What is there to do around here?",
                    2,
                    "What is this place?",
                    3,
                )
            when (option) {
                1 -> okayThanks()
                2 -> whatToDo()
                3 -> whatIsThisPlace()
            }
            return
        }
        whatIsThisPlace()
    }

    private suspend fun Dialogue.whatIsThisPlace() {
        chatPlayer(quiz, "What is this place?")
        chatNpc(
            neutral,
            "This is the home of the Bedabin. We're a peaceful tribe of desert dwellers. Some " +
                "idiots call us 'Tenti's', a childish name borne of ignorance.",
        )
        chatNpc(
            happy,
            "We're renowned for surviving in the harshest desert climate. We also grow the " +
                "'Bedabin ambrosia.'... A pineapple of such delicious sumptiousness that it defies " +
                "description.",
        )
        chatNpc(happy, "Take a look around our camp if you like!")
        if (choice2("Okay Thanks!", true, "What is there to do around here?", false)) {
            okayThanks()
        } else {
            whatToDo()
        }
    }

    private suspend fun Dialogue.okayThanks() {
        chatPlayer(happy, "Okay Thanks!")
        chatNpc(happy, "Good day Effendi!")
    }

    private suspend fun Dialogue.whatToDo() {
        chatPlayer(quiz, "What is there to do around here?")
        chatNpc(
            neutral,
            "Well, we are all very busy most of the time, tending to the pineapples. They are " +
                "grown in a secret location, to stop thieves from raiding our most precious prize.",
        )
    }

    // https://oldschool.runescape.wiki/w/Transcript:Bedabin_Nomad_Fighter
    private suspend fun Dialogue.fighter() {
        chatNpc(happy, "Hello Effendi! How can I help you?")
        while (true) {
            val option =
                choice3(
                    "What is this place?",
                    1,
                    "Where is the Shantay Pass?",
                    2,
                    "Okay, thanks",
                    3,
                )
            when (option) {
                1 -> {
                    chatPlayer(quiz, "What is this place?")
                    chatNpc(
                        happy,
                        "This is the camp of the Bedabin. Talk to our leader, Al Shabim, he'll be " +
                            "happy to chat.",
                    )
                }
                2 -> {
                    chatPlayer(quiz, "Where is the Shantay Pass?")
                    chatNpc(
                        neutral,
                        "It is north-east of here effendi, across the trackless desert. It will be " +
                            "a thirsty trip!",
                    )
                }
                3 -> {
                    chatPlayer(happy, "Okay, thanks")
                    return
                }
            }
        }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Bedabin_Nomad_Guard
    // TODO(content): [quest:the_tourist_trap] thanks after the quest
    private suspend fun Dialogue.guard() {
        chatNpc(
            neutral,
            "Sorry, but you can't use the tent without permission. Orders of Al Shabim.",
        )
    }

    // https://oldschool.runescape.wiki/w/Transcript:Asgarnia_Smith
    // TODO(content): [quest:desert_treasure_i] requirement check, pre-quest dialogue with the
    //  quest start, and after-quest dialogue
    private suspend fun Dialogue.asgarniaSmith() {
        chatPlayer(happy, "Hello there.")
        mesbox("He seems to be lost in his own thoughts...")
    }
}
