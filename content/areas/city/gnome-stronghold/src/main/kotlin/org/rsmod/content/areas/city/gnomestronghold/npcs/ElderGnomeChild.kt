package org.rsmod.content.areas.city.gnomestronghold.npcs

import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.script.onOpNpc1
import org.rsmod.content.quest.manager.QuestRequirements
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

// https://oldschool.runescape.wiki/w/Transcript:Elder_Gnome_child
// TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
// TODO(content): [diary:western_provinces] reward claims and "Can I have another banner?"
// TODO(content): [system:achievement_diary] open the Achievement Diaries tab in "Who are you?"
class ElderGnomeChild : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1("npc.western_gnome_child_diary") { startDialogue(it.npc) { elderGnomeChild() } }
    }

    private suspend fun Dialogue.elderGnomeChild() {
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
                    chatNpc(
                        happy,
                        "I'm the eldest of the Gnome children and master of tasks for the " +
                            "Western Provinces Achievement Diary.",
                    )
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
            "It's a diary that helps you keep track of particular achievements. In the Western " +
                "Provinces it can help you discover some quite useful things. Eventually, with " +
                "enough exploration, the inhabitants will reward you.",
        )
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
                    "Just complete the tasks in the Western provinces so they're ticked off, " +
                        "then come and speak to me for your rewards.",
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
            "Well, there are four different Western banners, which match up with the four " +
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
                    "If you complete all of the easy tasks in the Western Diary, you have a 25% " +
                        "chance of 2 Chompy birds appearing when hunting and Rantz will provide " +
                        "you with 25 free ogre arrows every day.",
                )
            }
            2 -> {
                chatPlayer(quiz, "Tell me more about the Medium rewards please!")
                chatNpc(
                    happy,
                    "In addition to the easy rewards, the Crystal saw will last for twice as long " +
                        "before reverting to seed, you have a 50% chance of 2 Chompy birds " +
                        "appearing when hunting and Rantz will provide you with 50 free ogre",
                )
                chatNpc(happy, "arrows every day.")
            }
            3 -> {
                chatPlayer(quiz, "Tell me more about the Hard rewards please!")
                chatNpc(
                    happy,
                    "In addition to the easy and medium benefits, Ilfeen of the Elves can " +
                        "enchant your crystal seed into a Halberd, a locked door under Ape Atoll " +
                        "becomes unlocked, The Void Knights can upgrade your Void armour, the",
                )
                chatNpc(
                    happy,
                    "Hunter master in the Feldip hills will allow you to hunt in his private " +
                        "hunting ground, teleport crystals can now hold 5 charges, Rantz will " +
                        "provide you with 100 ogre arrows every day and the banner will teleport " +
                        "you to",
                )
                chatNpc(happy, "Piscatoris once per day.")
            }
            4 -> {
                chatPlayer(quiz, "Tell me more about the Elite rewards please!")
                val master =
                    if (QuestRequirements.hasCompleted(player, MONKEY_MADNESS_II)) "Steve" else "Nieve"
                chatNpc(
                    happy,
                    "In addition to the previous tiers of rewards, two Chompy birds will always " +
                        "appear and they have a chance of dropping a pet, $master will reward " +
                        "your slayer assignments with more points and Rantz will provide",
                )
                chatNpc(
                    happy,
                    "you with 150 free ogre arrows every day. The banner will teleport you to " +
                        "Piscatoris as much as you desire and you may resurrect once per day " +
                        "while battling Zulrah.",
                )
            }
        }
        chatPlayer(happy, "Thanks!")
    }

    private suspend fun Dialogue.bye() {
        chatPlayer(happy, "Bye!")
        chatNpc(happy, "Goodbye adventurer.")
    }

    private companion object {
        const val MONKEY_MADNESS_II = "quest_monkeymadness2"
    }
}
