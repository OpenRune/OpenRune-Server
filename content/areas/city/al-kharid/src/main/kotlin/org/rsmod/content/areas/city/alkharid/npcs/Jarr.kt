package org.rsmod.content.areas.city.alkharid.npcs

import org.rsmod.api.config.constants
import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.player.output.ClientScripts.toplevelSidebuttonSwitch
import org.rsmod.api.script.onOpNpc1
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

// https://oldschool.runescape.wiki/w/Transcript:Jarr
// TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
// TODO(content): [diary:desert] tier reward claims and "Can I have another amulet?"
class Jarr : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1("npc.jarr_desert_diary") {
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
                "I have a question about my Achievement Diary",
                2,
                "Bye!",
                3,
            )
        when (option) {
            1 -> {
                chatPlayer(quiz, "Who are you?")
                chatNpc(
                    happy,
                    "I'm Jarr, the taskmaster for the Kharidian Desert Achievement Diary.",
                )
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
                "Kharidian Desert it can help you discover some quite useful things. " +
                "Eventually, with enough exploration, the inhabitants will reward you.",
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
                    "Just complete the tasks in the Desert so they're ticked off, then come and " +
                        "speak to me for your rewards.",
                )
                mainMenu()
            }
            4 -> bye()
        }
    }

    private suspend fun Dialogue.rewards() {
        chatNpc(
            happy,
            "Well, there are four different Desert amulets, which match up with the four levels " +
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
                    "If you complete all of the easy tasks in the Desert, you can charge a " +
                        "Pharaoh's sceptre with 10 charges, Simon Templeton will purchase " +
                        "artefacts in banknote form and Desert goat horns are dropped as notes.",
                )
            }
            2 -> {
                chatPlayer(quiz, "Tell me more about the Medium rewards please!")
                chatNpc(
                    happy,
                    "In addition to the easy rewards, you can charge a Pharaoh's sceptre with 25 " +
                        "charges and the amulet will teleport you to Nardah once per day.",
                )
            }
            3 -> {
                chatPlayer(quiz, "Tell me more about the Hard rewards please!")
                chatNpc(
                    happy,
                    "In addition to the easy and medium benefits, you can charge a Pharaoh's " +
                        "sceptre with 50 charges, you can adjust your Camulet to teleport you " +
                        "above the temple and the Nardah herbalist will create unfinished potions",
                )
                chatNpc(
                    happy,
                    "for you, as well as cleaning noted herbs for the usual herb cleaning fee.",
                )
                chatNpc(
                    happy,
                    "All your rides on a magic carpet are free and you'll receive extra Agility " +
                        "experience from the Pollnivneach rooftop course.",
                )
            }
            4 -> {
                chatPlayer(quiz, "Tell me more about the Elite rewards please!")
                chatNpc(
                    happy,
                    "In addition to the previous tiers of rewards, you can charge a Pharaoh's " +
                        "sceptre with 100 charges, the amulet will protect you from desert " +
                        "heat at all times, a shortcut opens up in the Kalphite hive and the " +
                        "amulet will",
                )
                chatNpc(
                    happy,
                    "teleport you to Nardah and the kalphite hive to the East of the Shantay " +
                        "Pass as much as you desire. You'll also get completely free passage " +
                        "through the Shantay passes here and at the Ruins of Unkah.",
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
