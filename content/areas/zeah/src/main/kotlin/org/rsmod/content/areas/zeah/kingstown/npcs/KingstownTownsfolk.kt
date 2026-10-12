package org.rsmod.content.areas.zeah.kingstown.npcs

import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.script.onOpNpc1
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

// TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
class KingstownTownsfolk : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1("npc.elise_kourend_kebos_diary") { startDialogue(it.npc) { elise() } }
        onOpNpc1("npc.kourend_statue_architect") { startDialogue(it.npc) { hosa() } }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Hosa
    private suspend fun Dialogue.hosa() {
        chatPlayer(happy, "That statue is truly awesome!")
        chatNpc(happy, "Isn't it just!")
    }

    // https://oldschool.runescape.wiki/w/Transcript:Elise
    // TODO(content): [diary:kourend_and_kebos] "Can I have another Rada's blessing?" and the
    //  "I've completed all of the ... tasks" reward claims (Rada's blessing 1-4)
    private suspend fun Dialogue.elise() {
        chatNpc(happy, "Hello!")
        while (true) {
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
                    chatNpc(
                        happy,
                        "I'm Elise, the taskmaster for the Kourend & Kebos Achievement Diary.",
                    )
                    whatIsTheDiary()
                }
                2 -> {
                    chatPlayer(quiz, "I have a question about my Achievement Diary.")
                    if (!diaryQuestions()) {
                        return
                    }
                }
                else -> return bye()
            }
        }
    }

    private suspend fun Dialogue.diaryQuestions(): Boolean {
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
                    neutral,
                    "Just complete the tasks in Kourend & Kebos area so they're ticked off, then " +
                        "come and speak to me for your rewards.",
                )
            }
            else -> {
                bye()
                return false
            }
        }
        return true
    }

    private suspend fun Dialogue.whatIsTheDiary() {
        chatPlayer(quiz, "What is the Achievement Diary?")
        chatNpc(
            neutral,
            "It's a diary that helps you keep track of particular achievements. In Great Kourend " +
                "and the Kebos Lowlands, it can help you discover some quite useful things.",
        )
        chatNpc(neutral, "Eventually, with enough exploration, you'll be rewarded.")
        chatNpc(neutral, "You can see the list of tasks on the side-panel.")
    }

    private suspend fun Dialogue.bye() {
        chatPlayer(happy, "Bye!")
        chatNpc(happy, "See you later.")
    }

    private suspend fun Dialogue.rewards() {
        chatNpc(
            neutral,
            "Well, there are four different blessings, which match up with the four levels of " +
                "difficulty. Each has the same rewards as the previous level and some additional " +
                "benefits too...",
        )
        chatNpc(neutral, "which tier of rewards would you like to know more about?")
        val tier =
            choice4(
                "Tell me more about the easy rewards please.",
                1,
                "Tell me more about the medium rewards please.",
                2,
                "Tell me more about the hard rewards please.",
                3,
                "Tell me more about the elite rewards please.",
                4,
            )
        when (tier) {
            1 -> easyRewards()
            2 -> mediumRewards()
            3 -> hardRewards()
            else -> eliteRewards()
        }
        chatPlayer(happy, "Thanks!")
    }

    private suspend fun Dialogue.easyRewards() {
        chatPlayer(quiz, "Tell me more about the Easy rewards please.")
        chatNpc(
            neutral,
            "If you complete all of the easy tasks, you'll get a 2% chance of catching two fish " +
                "at once while using the blessing. It will also provide 3 daily teleports to the " +
                "Kourend Woodland. On top of that, you'll be able to",
        )
        chatNpc(
            neutral,
            "access Crabclaw Island for half the price, lizardmen will be more likely to drop a " +
                "Xeric's talisman,, the people of Hosidius will protect your farming patches for " +
                "free, you'll be able to use the Hosidius Kitchens and Eodan",
        )
        chatNpc(
            neutral,
            "in the Forthos Dungeon will now tan hide for you at 8 times the normal price down " +
                "from 10 times.",
        )
    }

    private suspend fun Dialogue.mediumRewards() {
        chatNpc(
            neutral,
            "In addition to the easy rewards, you'll get a 4% chance of catching two fish at " +
                "once while using the blessing. It will also provide 5 daily teleports to the " +
                "Kourend Woodland. On top of that, you'll have a 5% chance to",
        )
        chatNpc(
            neutral,
            "mine two dense essence blocks at once, Thirus will give you 20 free Dynamite per " +
                "day and Eodan in the Forthos Dungeon will now tan hide for you at 6 times the " +
                "normal price down from 8 times.",
        )
    }

    private suspend fun Dialogue.hardRewards() {
        chatPlayer(quiz, "Tell me more about the Hard rewards please.")
        chatNpc(
            neutral,
            "In addition to the easy and medium benefits, you'll get a 6% chance of catching two " +
                "fish at once while using the blessing. It will also provide unlimited teleports " +
                "to the Kourend Woodland and 3 daily teleports to Mount",
        )
        chatNpc(
            neutral,
            "Karuulm. On top of that, Captain Cleive will show you how to use a Slayer helmet in " +
                "place of a Shayzien helm, you'll get a 5% increase in yield from the Hosidius " +
                "and Farming Guild Herb patches, Thirus will give you 40",
        )
        chatNpc(
            neutral,
            "free Dynamite per day and Eodan in the Forthos Dungeon will now tan hide for you at " +
                "4 times the normal price down from 6 times. Tyss at the Dark Altar will give " +
                "you an ash sanctifier too!",
        )
    }

    private suspend fun Dialogue.eliteRewards() {
        chatPlayer(quiz, "Tell me more about the Elite rewards please.")
        chatNpc(
            neutral,
            "In addition to the previous tiers of rewards, you'll get an 8% chance of catching " +
                "two fish at once while using the blessing. It will also provide unlimited " +
                "teleports to the Kourend Woodland and to Mount Karuulm. On top of",
        )
        chatNpc(
            neutral,
            "that, Konar quo Maten will give you 20 Slayer Points per task rather than 18, " +
                "you'll be 10% less likely to burn food at the Hosidius City Kitchens, you'll " +
                "get 10% more Blood runes when Runecrafting, you'll no longer",
        )
        chatNpc(
            neutral,
            "need to wear heat protection inside of the Karuulm Slayer Dungeon, Thirus will give " +
                "you 80 free Dynamite per day, Eodan in the Forthos Dungeon will now tan hide " +
                "for you at 2 times the normal price down",
        )
        chatNpc(
            neutral,
            "from 4 times and the experience provided by the ash sanctifier is increased too.",
        )
    }
}
