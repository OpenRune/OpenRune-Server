package org.rsmod.content.areas.city.ardougne.npcs

import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.script.onOpNpc1
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

// https://oldschool.runescape.wiki/w/Transcript:Two-pints
// TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
// TODO(content): [diary:ardougne] reward claims and "Can I have another cloak?"
// TODO(content): [system:achievement_diary] open the Achievement Diaries tab in "Who are you?"
class TwoPints : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1("npc.ardy_twopints_diary") { startDialogue(it.npc) { twoPints() } }
    }

    private suspend fun Dialogue.twoPints() {
        chatNpc(drunk, "'elo... *hic*")
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
                1 -> if (!whoAreYou()) return
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

    private suspend fun Dialogue.whoAreYou(): Boolean {
        chatPlayer(quiz, "Who are you?")
        chatNpc(
            drunk,
            "They call me Two pints... I'm the taskmaster for the Ardougne Achievement Diary.",
        )
        val why =
            choice2(
                "Why do they call you Two pints?",
                true,
                "What is the Achievement Diary?",
                false,
            )
        if (!why) {
            whatIsTheDiary()
            return true
        }
        chatPlayer(quiz, "Why do they call you Two pints?")
        chatNpc(drunk, "I'm not too sure really... my memory isn't the greatest... *hic*")
        chatPlayer(
            laugh,
            "Maybe you lost a game of Runelink and had to change your name as a forfeit?",
        )
        chatNpc(drunk, "Perhaps... *hic* I know a great drinking game for that!")
        chatPlayer(neutral, "Suddenly it all makes sense...")
        mesbox("Two pints mutters incoherently and stumbles away.")
        return false
    }

    private suspend fun Dialogue.whatIsTheDiary() {
        chatPlayer(quiz, "What is the Achievement Diary?")
        chatNpc(
            drunk,
            "It's a diary that helps you keep track of particular achievements. In and around " +
                "Ardougne it can help you discover some quite useful things. Eventually, with " +
                "enough exploration, the inhabitants will reward you.",
        )
        chatNpc(drunk, "You can see the list of tasks on the side-panel.")
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
                    drunk,
                    "Just complete the tasks in Ardougne so they're ticked off, then come and " +
                        "speak to me for your rewards.",
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
            drunk,
            "Well, there are four different Ardougne cloaks, which match up with the four levels " +
                "of difficulty. Each has the same rewards as the previous level and some " +
                "additional benefits too... which tier of rewards would you like to",
        )
        chatNpc(drunk, "know more about?")
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
                    drunk,
                    "If you complete all of the easy tasks in Ardougne: The Citizen in West " +
                        "Ardougne will buy cats for 200 death runes each, some drops in the " +
                        "Tower of Life will be noted and the cape can teleport you to the " +
                        "Monastery",
                )
                chatNpc(drunk, "south of Ardougne too.")
            }
            2 -> {
                chatPlayer(quiz, "Tell me more about the Medium rewards please!")
                chatNpc(
                    drunk,
                    "In addition to the easy rewards: Wizard Cromperty will give you 100 noted " +
                        "pure essence each day, even more drops will be noted in the Tower of " +
                        "Life, you've got a 10% better chance at pickpocketing in Ardougne, you",
                )
                chatNpc(
                    drunk,
                    "can toggle the ring of life to teleport to Ardougne, you get three " +
                        "teleports each day to the Ardougne farming patch and you can hold up to " +
                        "56 coin pouches whilst pickpocketing.",
                )
            }
            3 -> {
                chatPlayer(quiz, "Tell me more about the Hard rewards please!")
                chatNpc(
                    drunk,
                    "In addition to the easy and medium benefits: Wizard Cromperty will give you " +
                        "150 pure essence each day, you can toggle the Watchtower teleport to " +
                        "the centre of Yanille, claim further drops as notes in the Tower of",
                )
                chatNpc(
                    drunk,
                    "Life, teleports to the farming patch increased to five per day, have a 10% " +
                        "better chance at pickpocketing everywhere and you can hold up to 84 " +
                        "coin pouches whilst pickpocketing.",
                )
            }
            4 -> {
                chatPlayer(quiz, "Tell me more about the Elite rewards please!")
                chatNpc(
                    drunk,
                    "In addition to the previous tiers of rewards: Wizard Cromperty will give " +
                        "you 250 pure essence every day, you will catch 50% more fish from the " +
                        "Fishing Trawler and receive 25% more marks of grace on the",
                )
                chatNpc(
                    drunk,
                    "Ardougne agility course. Your cape will have unlimited teleports to the " +
                        "Ardougne farming patch, Bert will automatically deliver your daily sand " +
                        "and you can hold up to 140 coin pouches whilst pickpocketing.",
                )
            }
        }
        chatPlayer(happy, "Thanks!")
    }

    private suspend fun Dialogue.bye() {
        chatPlayer(happy, "Bye!")
        chatNpc(drunk, "See you later!")
    }
}
