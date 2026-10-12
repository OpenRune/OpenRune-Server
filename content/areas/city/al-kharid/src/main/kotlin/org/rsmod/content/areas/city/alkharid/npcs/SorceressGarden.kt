package org.rsmod.content.areas.city.alkharid.npcs

import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.script.onOpNpc1
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

// TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
class SorceressGarden : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1("npc.100_osman_cat") { startDialogue(it.npc) { delMonty() } }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Del-Monty
    // TODO(content): [quest:dragon_slayer_ii] catspeak without the amulet after the enchantment
    private suspend fun Dialogue.delMonty() {
        if (CATSPEAK_AMULETS.none { it in player.worn }) {
            chatPlayer(happy, "Hey kitty!")
            chatNpc(angry, "Hiss!")
            return
        }
        chatNpc(quiz, "Hello, no-fur. What are you doing in my mistress's garden?")
        val option = choice3("Looking for sq'irks.", 1, "Talking to cats.", 2, "Nothing much.", 3)
        when (option) {
            1 -> lookingForSqirks()
            2 -> talkingToCats()
            3 -> {
                chatPlayer(neutral, "Nothing much.")
                chatNpc(bored, "Yawn! Nice talking to you then, no-fur.")
            }
        }
    }

    private suspend fun Dialogue.lookingForSqirks() {
        chatPlayer(neutral, "Looking for sq'irks.")
        chatNpc(
            happy,
            "If it's sq'irks you're after then you've come to the right place. We've got four " +
                "seasons' worth of them.",
        )
        chatPlayer(quiz, "I've a couple of questions.")
        chatNpc(happy, "I'd be happy to help a friend of the feline.")
        chatPlayer(confused, "What do you mean?")
        chatNpc(neutral, "The Sphinx gave you that amulet, so she must hold you in high regard.")
        chatPlayer(
            neutral,
            "If I remember correctly, it was the High Priest of Sophanem who gave it to me.",
        )
        chatNpc(
            neutral,
            "He and the Sphinx are as thick as thieves, but regardless, I think you've a bit of " +
                "a cat feel about you.",
        )
        chatNpc(quiz, "Now what are these questions?")
        do {
            val option =
                choice4(
                    "What are the creatures inside the gardens?",
                    1,
                    "How do you get into the seasonal gardens?",
                    2,
                    "How is it that the gardens are in different seasons?",
                    3,
                    "How do I get out of here?",
                    4,
                )
            when (option) {
                1 -> {
                    chatPlayer(quiz, "What are the creatures inside the gardens?")
                    chatNpc(quiz, "Oh, you mean the gardeners?")
                    chatPlayer(neutral, "The strange, floaty creatures.")
                    chatNpc(
                        neutral,
                        "Yes, the gardeners. Don't let them see you or they'll teleport you out " +
                            "here. They're very protective of their crops.",
                    )
                }
                2 -> {
                    chatPlayer(quiz, "How do you get into the seasonal gardens?")
                    chatNpc(
                        neutral,
                        "I get in through gaps in the hedge. You are a little too big to squeeze " +
                            "through. I think you'll have to use the gates.",
                    )
                    chatNpc(
                        neutral,
                        "I think the gates are locked, so you'll have to have good Thieving " +
                            "skills to get in.",
                    )
                }
                3 -> {
                    chatPlayer(quiz, "How is it that the gardens are in different seasons?")
                    chatNpc(quiz, "How did you get here?")
                    chatPlayer(confused, "Magic?")
                    chatNpc(
                        neutral,
                        "Exactly. The Sorceress likes to have a good supply of in-season sq'irks " +
                            "and herbs at all times.",
                    )
                }
                4 -> {
                    chatPlayer(quiz, "How do I get out of here?")
                    chatNpc(neutral, "Take a drink from the fountain.")
                }
            }
        } while (anotherQuestion())
    }

    private suspend fun Dialogue.talkingToCats() {
        chatPlayer(happy, "Talking to cats.")
        chatNpc(happy, "A noble and rewarding past-time.")
        do {
            val option =
                choice3(
                    "How did you get here?",
                    1,
                    "What are you doing here?",
                    2,
                    "Who are you?",
                    3,
                )
            when (option) {
                1 -> {
                    chatPlayer(quiz, "How did you get here?")
                    chatNpc(
                        angry,
                        "Every time I play with spiders in the Sorceress's house, her silly " +
                            "apprentice completely freaks out and teleports me here!",
                    )
                    chatPlayer(quiz, "So you've been stuck here since?")
                    chatNpc(laugh, "No, silly! I drink from the fountain whenever I want to leave.")
                }
                2 -> {
                    chatPlayer(quiz, "What are you doing here?")
                    chatNpc(
                        neutral,
                        "I get this strange urge for sq'irks. It's quite peculiar. I think I may " +
                            "be addicted.",
                    )
                    chatPlayer(
                        neutral,
                        "I think I know someone else who may be in a similar position.",
                    )
                    chatNpc(quiz, "Don't tell me. Osman, right?")
                    chatPlayer(shocked, "You know him too?")
                    chatNpc(
                        neutral,
                        "Oh, he used to come here all the time. Then one day, he just stopped.",
                    )
                }
                3 -> {
                    chatPlayer(quiz, "Who are you?")
                    chatNpc(happy, "Del-Monty the cat, at your service.")
                    chatPlayer(
                        quiz,
                        "Are you a famous adventurer who was turned into a cat by a vindictive " +
                            "mage?",
                    )
                    chatNpc(
                        neutral,
                        "No; as I said, I'm Del-Monty the cat, connoisseur of exotic fruits.",
                    )
                    chatPlayer(quiz, "In that case, can you tell me anything about sq'irks?")
                    chatNpc(
                        happy,
                        "But of course! Their juice is an excellent source of energy for " +
                            "runners, and in the riper varieties they are known to heighten " +
                            "one's Thieving abilities.",
                    )
                }
            }
        } while (anotherQuestion())
    }

    private suspend fun Dialogue.anotherQuestion(): Boolean {
        val another =
            choice2(
                "Thanks, I have another question though.",
                true,
                "Thanks for your help.",
                false,
            )
        if (another) {
            chatPlayer(happy, "Thanks, I have another question though.")
        } else {
            chatPlayer(happy, "Thanks for your help.")
        }
        return another
    }

    private companion object {
        val CATSPEAK_AMULETS =
            listOf("obj.ics_little_amulet_of_catspeak", "obj.twocats_amuletofcatspeak")
    }
}
