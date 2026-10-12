package org.rsmod.content.areas.city.edgeville.npcs

import org.rsmod.api.config.constants
import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.player.output.ClientScripts.toplevelSidebuttonSwitch
import org.rsmod.api.script.onOpNpc1
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

// https://oldschool.runescape.wiki/w/Transcript:Lesser_Fanatic
// TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
// TODO(content): [diary:wilderness] reward claims and "Can I have another sword?"
// TODO(content): [diary:wilderness] hard: ecumenical key sales; elite: noted lava dragon bones
class LesserFanatic : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1("npc.lesser_fanatic_diary") {
            startDialogue(it.npc) {
                chatNpc(happy, "Hello!")
                mainMenu()
            }
        }
    }

    private suspend fun Dialogue.mainMenu() {
        val option =
            choice3(
                "Who are you?",
                1,
                "I have a question about my Achievement Diary.",
                2,
                "Bye!",
                3,
            )
        when (option) {
            1 -> {
                chatPlayer(quiz, "Who are you?")
                chatNpc(happy, "I'm Jeffery, the taskmaster for the Wilderness Achievement Diary.")
                whatIsTheDiary()
            }
            2 -> diaryQuestion()
            3 -> bye()
        }
    }

    private suspend fun Dialogue.whatIsTheDiary() {
        chatPlayer(quiz, "What is the Achievement Diary?")
        chatNpc(
            neutral,
            "It's a diary that helps you keep track of particular achievements. In the " +
                "Wilderness it can help you discover some quite useful things. Eventually, with " +
                "enough exploration, the inhabitants will reward you.",
        )
        toplevelSidebuttonSwitch(player, constants.toplevel_details)
        chatNpc(neutral, "You can see the list of tasks on the side-panel.")
        mainMenu()
    }

    private suspend fun Dialogue.diaryQuestion() {
        chatPlayer(quiz, "I have a question about my Achievement diary.")
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
            2 -> rewards()
            3 -> {
                chatPlayer(quiz, "How do I claim the rewards?")
                chatNpc(
                    neutral,
                    "Just complete the tasks in the Wilderness so they're ticked off, then come " +
                        "and speak to me for your rewards.",
                )
                mainMenu()
            }
            4 -> bye()
        }
    }

    private suspend fun Dialogue.rewards() {
        chatNpc(
            happy,
            "Well, there are four different Wilderness Swords, which match up with the four " +
                "levels of difficulty. Each has the same rewards as the previous level and some " +
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
                    "If you complete all of the easy tasks in the Wilderness, you can speak to " +
                        "Lundail at the Mage Arena every day to receive 40 free runes and you " +
                        "can teleport to either Edgeville or Ardougne from the Wilderness lever.",
                )
                chatPlayer(happy, "Thanks!")
            }
            2 -> {
                chatPlayer(quiz, "Tell me more about the Medium rewards please!")
                chatNpc(
                    happy,
                    "In addition to the easy rewards, Mandrith will give you a 20% discount off " +
                        "entry to the Resource Area, you'll cut Ents faster, you can access a " +
                        "shortcut in the deep Wilderness dungeon. You can also own 4 ecumenical",
                )
                chatNpc(
                    happy,
                    "keys at once and receive 80 runes a day from Lundail, and you can find loot " +
                        "in the chests at the Rogues' Castle. Finally, you can access the lairs " +
                        "of Callisto, Venenatis and Vet'ion, three fearsome Wilderness",
                )
                chatNpc(happy, "bosses.")
            }
            3 -> {
                chatPlayer(quiz, "Tell me more about the Hard rewards please!")
                chatNpc(
                    happy,
                    "In addition to the easy and medium benefits, the sword can teleport you to " +
                        "the Fountain of Rune once per day, you'll get 50% more lava shards when " +
                        "grinding scales and even cheaper access to the Resource Area.",
                )
                chatNpc(
                    happy,
                    "You can own 5 ecumenical keys, Lundail will give you 120 free runes each " +
                        "day, and you'll get increased loot from the chests at the Rogues' " +
                        "Castle. You can also telegrab Wine of Zamorak from spawns in the",
                )
                chatNpc(
                    happy,
                    "Wilderness in banknote form, and control which Obelisk you want to travel " +
                        "to. Also, I'll be willing to buy ecumenical keys off you for 61,500 " +
                        "coins each. Finally, you can access the lairs of Artio, Spindel and " +
                        "Calvar'ion,",
                )
                chatNpc(happy, "three fearsome Wilderness bosses.")
                chatPlayer(happy, "Thanks!")
            }
            4 -> {
                chatPlayer(quiz, "Tell me more about the Elite rewards please!")
                chatNpc(
                    happy,
                    "In addition to the previous tiers of rewards, access to the Resource Area " +
                        "is free and the sword will now give you unlimited teleports to the " +
                        "Fountain of Rune. All dragon bone drops in the Wilderness become noted " +
                        "and",
                )
                chatNpc(
                    happy,
                    "Lundail will give you 200 free runes per day and the rate at which you " +
                        "catch Dark Crabs is increased.",
                )
                chatPlayer(happy, "Thanks!")
            }
        }
    }

    private suspend fun Dialogue.bye() {
        chatPlayer(happy, "Bye!")
        chatNpc(happy, "See you later.")
    }
}
