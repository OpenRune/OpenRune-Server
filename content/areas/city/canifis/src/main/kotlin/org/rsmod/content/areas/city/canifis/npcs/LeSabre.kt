package org.rsmod.content.areas.city.canifis.npcs

import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.script.onOpNpc1
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

// https://oldschool.runescape.wiki/w/Transcript:Le-sabr%C3%A8
// TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
// TODO(content): [diary:morytania] reward claims and "Can I have another set of Morytania legs?"
class LeSabre : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1("npc.lesabre_mort_diary") { startDialogue(it.npc) { leSabre() } }
    }

    private suspend fun Dialogue.leSabre() {
        chatNpc(happy, "Hello, welcome to Morytania!")
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
                        "My name is Le Sabre and I'm the task master for the Morytania " +
                            "Achievement Diary.",
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
            "It's a diary that helps you keep track of particular achievements. Here in " +
                "Morytania it can help you discover some quite useful things. Eventually, with " +
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
                title = "Select an Option",
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
                    "Just complete the tasks in Morytania so they're ticked off, then come and " +
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
            happy,
            "Well, there are four different pairs of Morytania platelegs, which match up with " +
                "the four levels of difficulty. Each has the same rewards as the previous level " +
                "and some additional benefits too... which tier of",
        )
        chatNpc(happy, "rewards would you like to know more about?")
        val tier =
            choice4(
                "Easy Rewards.",
                1,
                "Medium Rewards.",
                2,
                "Hard Rewards.",
                3,
                "Elite Rewards.",
                4,
                title = "Select an Option",
            )
        when (tier) {
            1 -> {
                chatPlayer(quiz, "Tell me more about the Easy rewards please!")
                chatNpc(
                    happy,
                    "If you complete all of the easy tasks in Morytania, your legs can teleport " +
                        "to the Slime pit beneath the Ectofuntus twice a day, there's a chance " +
                        "Ghasts will ignore you in the swamp, and you'll receive a 2.5%",
                )
                chatNpc(happy, "increase in Slayer XP when on assignment in the Slayer Tower too.")
            }
            2 -> {
                chatPlayer(quiz, "Tell me more about the Medium rewards please!")
                chatNpc(
                    happy,
                    "In addition to the easy rewards, you get 5 daily slime pit teleports and a " +
                        "built-in ghostspeak amulet. Robin in Port Phasmatys will exchange some " +
                        "bonemeal for bones for you, and you'll receive a 5% increase in Slayer",
                )
                chatNpc(happy, "XP when on assignment in the Slayer Tower too.")
            }
            3 -> {
                chatPlayer(quiz, "Tell me more about the Hard rewards please!")
                chatNpc(
                    happy,
                    "In addition to the easy and medium benefits, you can pick double Mort Myre " +
                        "Fungi in the swamp, get more experience from burning shades, receive " +
                        "more runes from Barrows, Robin will exchange more bonemeal for",
                )
                chatNpc(
                    happy,
                    "bones each day and you'll receive a 7.5% increase in Slayer XP when on " +
                        "assignment in the Slayer Tower. The disciples at the Ectofuntus will " +
                        "give you a bonecrusher too!",
                )
            }
            4 -> {
                chatPlayer(quiz, "Tell me more about the Elite rewards please!")
                chatNpc(
                    happy,
                    "In addition to the previous tiers of rewards, Robin will exchange even more " +
                        "bonemeal for bones, you'll receive a 10% increase in Slayer XP when on " +
                        "assignment in the Slayer Tower, there's a new herb farming patch on",
                )
                chatNpc(
                    happy,
                    "Harmony Island, you'll receive 50% more experience when cremating shade " +
                        "remains and the experience provided by the Bonecrusher is increased too.",
                )
            }
        }
        chatPlayer(happy, "Thanks!")
    }

    private suspend fun Dialogue.bye() {
        chatPlayer(happy, "Bye!")
        chatNpc(happy, "See you later!")
    }
}
