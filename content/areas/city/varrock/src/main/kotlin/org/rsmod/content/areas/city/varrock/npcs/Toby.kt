package org.rsmod.content.areas.city.varrock.npcs

import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.script.onOpNpc1
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

// https://oldschool.runescape.wiki/w/Transcript:Toby
// TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
// TODO(content): [diary:varrock] reward claims and "Can I have another Varrock platebody?"
// TODO(content): [system:achievement_diary] open the Achievement Diaries tab in "Who are you?"
class Toby : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1("npc.toby_varrock_diary") { startDialogue(it.npc) { toby() } }
    }

    private suspend fun Dialogue.toby() {
        chatNpc(happy, "Hello!")
        while (true) {
            val option =
                choice3(
                    "Who are you?",
                    1,
                    "I have a question about my Achievement Diary",
                    2,
                    "Bye!",
                    3,
                )
            when (option) {
                1 -> {
                    chatPlayer(quiz, "Who are you?")
                    chatNpc(happy, "I'm Toby, the taskmaster for the Varrock Achievement Diary.")
                    whatIsTheDiary()
                }
                2 -> {
                    chatPlayer(quiz, "I have a question about my Achievement diary.")
                    if (!diaryQuestion()) return
                }
                3 -> {
                    bye()
                    return
                }
            }
        }
    }

    private suspend fun Dialogue.whatIsTheDiary() {
        chatPlayer(quiz, "What is the Achievement Diary?")
        chatNpc(
            happy,
            "It's a diary that helps you keep track of particular achievements. In Varrock and " +
                "the surrounding area it can help you discover some quite useful things. " +
                "Eventually, with enough exploration, the inhabitants will",
        )
        chatNpc(happy, "reward you.")
        chatNpc(happy, "You can see the list of tasks on the side-panel.")
    }

    private suspend fun Dialogue.diaryQuestion(): Boolean {
        val option =
            choice4(
                "What is the Achievement Diary?",
                1,
                "What are the rewards?",
                2,
                "How do I claim the rewards?",
                3,
                "Bye!",
                4,
            )
        when (option) {
            1 -> whatIsTheDiary()
            2 -> {
                rewards()
                return false
            }
            3 -> {
                chatPlayer(quiz, "How do I claim the rewards?")
                chatNpc(
                    happy,
                    "Just complete the tasks in the Varrock area so they're ticked off, then " +
                        "come and speak to me for your rewards.",
                )
            }
            4 -> {
                bye()
                return false
            }
        }
        return true
    }

    private suspend fun Dialogue.rewards() {
        chatNpc(
            happy,
            "Well, there are four different Varrock armours, which match up with the four levels " +
                "of difficulty. Each has the same rewards as the previous level and some " +
                "additional benefits too... which tier of rewards would you like to",
        )
        chatNpc(happy, "know more about?")
        val option =
            choice4(
                "Easy Rewards.",
                1,
                "Medium Rewards.",
                2,
                "Hard Rewards.",
                3,
                "Elite Rewards.",
                4,
            )
        when (option) {
            1 -> {
                chatPlayer(quiz, "Tell me more about the Easy rewards please!")
                chatNpc(
                    happy,
                    "If you complete all of the easy tasks in Varrock, you have a 10% chance of " +
                        "mining two ores at once up to coal and a 10% chance of smelting 2 bars " +
                        "at once up to steel, when using the Edgeville furnace. Zaff will",
                )
                chatNpc(happy, "stock 15 Battlestaves per day.")
            }
            2 -> {
                chatPlayer(quiz, "Tell me more about the Medium rewards please!")
                chatNpc(
                    happy,
                    "In addition to the easy rewards, you have a 10% chance of mining two ores " +
                        "at once up to mithril and a 10% chance of smelting 2 bars at once up to " +
                        "mithril, when using the Edgeville furnace. Zaff will stock 30",
                )
                chatNpc(happy, "Battlestaves per day")
            }
            3 -> {
                chatPlayer(quiz, "Tell me more about the Hard rewards please!")
                chatNpc(
                    happy,
                    "In addition to the easy and medium benefits, you have a 10% chance of " +
                        "mining two ores at once up to adamant and a 10% chance of smelting 2 " +
                        "bars at once up to adamant, when using the Edgeville furnace. Zaff",
                )
                chatNpc(
                    happy,
                    "will stock 60 Battlestaves per day and you can access the bank at the " +
                        "Cooking guild.",
                )
            }
            4 -> {
                chatPlayer(quiz, "Tell me more about the Elite rewards please!")
                chatNpc(
                    happy,
                    "In addition to the previous tiers of rewards, you have a 10% chance of " +
                        "mining two ores at once up to runite and a 10% chance of smelting 2 " +
                        "bars at once up to runite, when using the Edgeville furnace. Zaff will " +
                        "stock",
                )
                chatNpc(
                    happy,
                    "120 Battlestaves per day and the plate functions as a Prospector top.",
                )
            }
        }
        chatPlayer(happy, "Thanks!")
    }

    private suspend fun Dialogue.bye() {
        chatPlayer(happy, "Bye!")
        chatNpc(happy, "See you later.")
    }
}
