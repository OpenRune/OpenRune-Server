package org.rsmod.content.areas.city.seersvillage.npcs

import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.script.onOpNpc1
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

// https://oldschool.runescape.wiki/w/Transcript:The_%27Wedge%27
// TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
// TODO(content): [diary:kandarin] reward claims and "Can I have some more headgear?"
// TODO(content): [system:achievement_diary] open the Achievement Diaries tab in "Who are you?"
class Wedge : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1("npc.seers_diary_wedge") { startDialogue(it.npc) { wedge() } }
    }

    private suspend fun Dialogue.wedge() {
        chatNpc(happy, "Hi there!")
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
                        "Call me The 'Wedge', I'm the taskmaster for the Achievement Diary here " +
                            "in Kandarin.",
                    )
                    whatIsTheDiary()
                }
                2 -> {
                    chatPlayer(quiz, "I have a question about my Achievement Diary.")
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
            "It's a diary that helps you keep track of particular achievements. In and around " +
                "Kandarin it can help you discover some quite useful things. Eventually, with " +
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
                    "Just complete the tasks in Kandarin so they're ticked off, then come and " +
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
            "Well, there are four different Kandarin headgears, which match up with the four " +
                "levels of difficulty. Each has the same rewards as the previous level and some " +
                "additional benefits too... which tier of rewards would you",
        )
        chatNpc(happy, "like to know more about?")
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
            1 -> easyRewards()
            2 -> mediumRewards()
            3 -> hardRewards()
            4 -> eliteRewards()
        }
        chatPlayer(happy, "Thanks!")
    }

    private suspend fun Dialogue.easyRewards() {
        chatPlayer(quiz, "Tell me more about the Easy rewards please!")
        chatNpc(
            happy,
            "If you complete all of the easy tasks in Kandarin, the headgear functions as a " +
                "light source and you have a chance of receiving an extra log when cutting " +
                "normal trees. You can get the flax keeper to convert 30 flax",
        )
        chatNpc(
            happy,
            "to bowstrings each day, coal trucks hold up to 140 coal and you'll find 5% more " +
                "marks of grace on Seers' rooftop agility course.",
        )
    }

    private suspend fun Dialogue.mediumRewards() {
        chatPlayer(quiz, "Tell me more about the Medium rewards please!")
        chatNpc(
            happy,
            "In addition to the easy rewards, cutting maples in Seers' village provides 10% more " +
                "experience. You can spin materials 33% faster at the spinning wheel, get the " +
                "flax keeper to convert 60 flax to bowstrings each day,",
        )
        chatNpc(
            happy,
            "and the Catherby herb patch provides a 5% higher crop yield. Coal trucks will hold " +
                "up to 280 coal too. You can also use the rope I left at Baxtorian falls, " +
                "instead of bringing your own, as long as you have completed the",
        )
        chatNpc(happy, "Waterfall quest.")
    }

    private suspend fun Dialogue.hardRewards() {
        chatPlayer(quiz, "Tell me more about the Hard rewards please!")
        chatNpc(
            happy,
            "In addition to the easy and medium benefits, Thormac will enchant battlestaves for " +
                "30,000 gold and you can move the Camelot teleport location to outside of " +
                "Seers' bank. You'll find 15% more marks of grace on Seers'",
        )
        chatNpc(
            happy,
            "rooftop, receive 10% higher yield from Catherby farming patch, get the flax keeper " +
                "to convert 120 flax to bowstrings each day, and get 10% more points from " +
                "Barbarian Assault as well one free daily teleport to",
        )
        chatNpc(
            happy,
            "Sherlock. Your chances of having an enchanted bolt use it's special attack has " +
                "increased by 10% too.",
        )
    }

    private suspend fun Dialogue.eliteRewards() {
        chatPlayer(quiz, "Tell me more about the Elite rewards please!")
        chatNpc(
            happy,
            "In addition to the previous tiers of rewards, Thormac now enchants battlestaves for " +
                "20,000 gold, Otto will provide a 50% discount when converting Zamorakian " +
                "spears into hastae. You'll get unlimited teleports to",
        )
        chatNpc(
            happy,
            "Sherlock and 15% increased yield from Catherby herb patch and can get the flax " +
                "keeper to convert 250 flax to bow strings every day. The first 200 coal placed " +
                "in coal trucks every day will be automatically transported",
        )
        chatNpc(happy, "to your bank too.")
    }

    private suspend fun Dialogue.bye() {
        chatPlayer(neutral, "Bye!")
        chatNpc(happy, "See you around.")
    }
}
