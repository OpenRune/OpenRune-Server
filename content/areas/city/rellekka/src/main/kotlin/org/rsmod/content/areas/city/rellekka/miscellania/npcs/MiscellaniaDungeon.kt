package org.rsmod.content.areas.city.rellekka.miscellania.npcs

import org.rsmod.api.config.Constants
import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.script.onOpNpc1
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

// TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
// TODO(content): [quest:royal_trouble] the dungeon opens with Royal Trouble, so the after-quest
// dialogue is the default; in-quest lines are missing
// TODO(content): [system:treasure_trails] Runolf's master clue puzzle box
class MiscellaniaDungeon : PluginScript() {
    override fun ScriptContext.startup() {
        for (citizen in ROYAL_CITIZENS) {
            onOpNpc1(citizen) { startDialogue(it.npc) { royalCitizen() } }
        }
        onOpNpc1("npc.royal_dwarf_citizen1") { startDialogue(it.npc) { alviss() } }
        onOpNpc1("npc.royal_dwarf_citizen2") { startDialogue(it.npc) { fullangr() } }
        onOpNpc1("npc.royal_dwarf_citizen3") { startDialogue(it.npc) { jari() } }
        onOpNpc1("npc.royal_dwarf_drunk") { startDialogue(it.npc) { donal() } }
        onOpNpc1("npc.royal_dwarf_miner1") { startDialogue(it.npc) { thorodin() } }
        onOpNpc1("npc.royal_dwarf_miner2") { startDialogue(it.npc) { ferd() } }
        onOpNpc1("npc.royal_fremennik_teen1") {
            startDialogue(it.npc) { chatNpc(sad, "We didn't mean it to go wrong, you know...") }
        }
        onOpNpc1("npc.royal_fremennik_teen2") {
            startDialogue(it.npc) { chatNpc(sad, "I want to go home.") }
        }
        onOpNpc1("npc.royal_fremennik_teen3") {
            startDialogue(it.npc) { chatNpc(sad, "And we'd planned it so well, too.") }
        }
        onOpNpc1("npc.royal_fremennik_teen4") {
            startDialogue(it.npc) {
                chatNpc(sad, "If only that gigantic snake hadn't moved in.")
            }
        }
        onOpNpc1("npc.royal_fremennik_teen5") {
            startDialogue(it.npc) {
                chatNpc(angry, "That idiot Armod should never have burned his diary!")
            }
        }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Runolf
    // https://oldschool.runescape.wiki/w/Transcript:Tjorvi
    // https://oldschool.runescape.wiki/w/Transcript:Ingrid
    // https://oldschool.runescape.wiki/w/Transcript:Thora
    private suspend fun Dialogue.royalCitizen() {
        chatPlayer(happy, "Hello.")
        chatNpc(happy, "Good day, your Royal Highness.")
    }

    private suspend fun Dialogue.dwarfGreeting(): Boolean {
        val greeting = if (player.appearance.bodyType == Constants.bodytype_a) "sir" else "madam"
        chatNpc(happy, "Good day, $greeting.")
        val option = choice2("What are you doing down here?", true, "Good day.", false)
        if (!option) {
            chatPlayer(happy, "Good day.")
            return false
        }
        chatPlayer(quiz, "What are you doing down here?")
        return true
    }

    // https://oldschool.runescape.wiki/w/Transcript:Alviss
    private suspend fun Dialogue.alviss() {
        if (!dwarfGreeting()) {
            return
        }
        chatNpc(
            neutral,
            "I'm waiting for my shift, of course. We can't dig all the time, you know.",
        )
        chatNpc(neutral, "I'm also researching the links between the Fremenniks and the Dwarves.")
        chatNpc(happy, "I've found that we have some mythology in common.")
    }

    // https://oldschool.runescape.wiki/w/Transcript:Fullangr
    private suspend fun Dialogue.fullangr() {
        if (!dwarfGreeting()) {
            return
        }
        chatNpc(neutral, "I'm working on the digging, of course.")
        chatNpc(neutral, "It's a small excavation, so only two of us can work on it at a time.")
    }

    // https://oldschool.runescape.wiki/w/Transcript:Jari
    private suspend fun Dialogue.jari() {
        if (!dwarfGreeting()) {
            return
        }
        chatNpc(neutral, "I'm waiting to work on the digging.")
        chatNpc(
            happy,
            "It's the first excavation I've worked on, and I'm looking forward to it.",
        )
    }

    // https://oldschool.runescape.wiki/w/Transcript:Donal
    private suspend fun Dialogue.donal() {
        chatNpc(angry, "What do you want?")
        chatPlayer(neutral, "Just wondering if you were still here.")
        chatNpc(angry, "Of course I'm still here.")
        chatNpc(worried, "I'm not going near that crack in the wall again.")
        chatNpc(worried, "Rock falls and so on are fine, but sea monsters in caves - never!")
    }

    // https://oldschool.runescape.wiki/w/Transcript:Ferd
    private suspend fun Dialogue.ferd() {
        chatNpc(neutral, "Good day, sir.")
        val option = choice2("What are you doing down here?", true, "Good day.", false)
        if (!option) {
            chatPlayer(happy, "Good day.")
            return
        }
        chatPlayer(quiz, "What are you doing down here?")
        chatNpc(neutral, "Shoring up the walls.")
        chatPlayer(quiz, "What does that do?")
        chatNpc(neutral, "Stops them falling down.")
        chatPlayer(neutral, "Oh, I see.")
        chatNpc(neutral, "Aye.")
        chatNpc(
            angry,
            "If you want to chatter, you'd better talk to Thorodin over there. I'm working.",
        )
        chatPlayer(neutral, "Okay then.")
    }

    // https://oldschool.runescape.wiki/w/Transcript:Thorodin
    // TODO(content): [diary:fremennik] "Can I have some more boots?", reward claims, the Fremennik
    // Exiles greeting and the Dagannoth Kings noted bones toggle
    private suspend fun Dialogue.thorodin() {
        chatNpc(happy, "Hello!")
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
                chatNpc(neutral, "Thorodin, taskmaster for the Fremennik Achievement Diary.")
                chatPlayer(quiz, "What is the Achievement Diary?")
                diaryExplanation()
            }
            2 -> {
                chatPlayer(quiz, "I have a question about my Achievement diary.")
                diaryQuestions()
            }
            3 -> bye()
        }
    }

    private suspend fun Dialogue.diaryExplanation() {
        chatNpc(
            neutral,
            "It's a diary that helps you keep track of particular achievements. In the Fremennik " +
                "lands it can help you discover some quite useful things. Eventually, with " +
                "enough exploration, the inhabitants will reward you.",
        )
        chatNpc(neutral, "You can see the list of tasks on the side-panel.")
    }

    private suspend fun Dialogue.diaryQuestions() {
        while (true) {
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
                1 -> {
                    chatPlayer(quiz, "What is the Achievement Diary?")
                    diaryExplanation()
                }
                2 -> {
                    diaryRewards()
                    return
                }
                3 ->
                    chatNpc(
                        neutral,
                        "Just complete the tasks in the Fremennik Province so they're ticked " +
                            "off, then come and speak to me for your rewards.",
                    )
                4 -> {
                    bye()
                    return
                }
            }
        }
    }

    private suspend fun Dialogue.diaryRewards() {
        chatNpc(
            neutral,
            "Well, there are four different pairs of Fremennik sea boots, which match up with " +
                "the four levels of difficulty. Each has the same rewards as the previous level and " +
                "some additional benefits too... which tier of rewards",
        )
        chatNpc(neutral, "would you like to know more about?")
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
                chatPlayer(happy, "Tell me more about the Easy rewards please!")
                chatNpc(
                    neutral,
                    "If you complete all of the easy tasks in the Fremennik Province, Peer the " +
                        "Seer can bank items for you at all times and your enchanted lyre can " +
                        "hold an extra charge.",
                )
            }
            2 -> {
                chatPlayer(happy, "Tell me more about the Medium rewards please!")
                chatNpc(
                    neutral,
                    "In addition to the easy rewards, your boots can teleport you to Rellekka " +
                        "once per day as well as an increased rate of gaining approval with your " +
                        "subjects.",
                )
            }
            3 -> {
                chatPlayer(happy, "Tell me more about the Hard rewards please!")
                chatNpc(
                    neutral,
                    "In addition to the easy and medium benefits, you'll be able to change your " +
                        "lyre teleport location to Waterbirth Island, access two new Lunar " +
                        "spells and you'll receive extra Agility experience from the Rellekka " +
                        "rooftop",
                )
                chatNpc(neutral, "course.")
            }
            4 -> {
                chatPlayer(happy, "Tell me more about the Elite rewards please!")
                chatNpc(
                    neutral,
                    "In addition to the previous tiers of rewards, your boots now have unlimited " +
                        "teleports to Rellekka, your lyre can be attuned to teleport you to " +
                        "Jatiszo or Neitiznot, you'll no longer need a seal of passage on Lunar " +
                        "Isle,",
                )
                chatNpc(
                    neutral,
                    "Dagannoth bones will be dropped as notes and your subjects will approve of " +
                        "your kingdom management even faster than before.",
                )
            }
        }
        chatPlayer(happy, "Thanks!")
    }

    private suspend fun Dialogue.bye() {
        chatPlayer(happy, "Bye!")
        chatNpc(happy, "See you later.")
    }

    private companion object {
        val ROYAL_CITIZENS =
            listOf(
                "npc.royal_citizen_man1",
                "npc.royal_citizen_man2",
                "npc.royal_citizen_woman1",
                "npc.royal_citizen_woman2",
            )
    }
}
