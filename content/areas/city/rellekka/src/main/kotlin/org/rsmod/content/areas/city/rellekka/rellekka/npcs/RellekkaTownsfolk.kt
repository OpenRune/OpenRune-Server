package org.rsmod.content.areas.city.rellekka.rellekka.npcs

import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.script.onOpNpc1
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

// TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
class RellekkaTownsfolk : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1("npc.viking_man3") { startDialogue(it.npc) { fremennikCitizen() } }
        onOpNpc1("npc.viking_man2") { startDialogue(it.npc) { fremennikCitizen() } }
        onOpNpc1("npc.viking_man4") { startDialogue(it.npc) { fremennikCitizen() } }
        onOpNpc1("npc.viking_woman") { startDialogue(it.npc) { fremennikCitizen() } }
        onOpNpc1("npc.viking_woman2") { startDialogue(it.npc) { fremennikCitizen() } }
        onOpNpc1("npc.viking_woman3") { startDialogue(it.npc) { fremennikCitizen() } }
        onOpNpc1("npc.viking_woman4") { startDialogue(it.npc) { fremennikCitizen() } }
        onOpNpc1("npc.handsand_reeso") { startDialogue(it.npc) { fremennikCitizen() } }
        onOpNpc1("npc.viking_askelapen") { startDialogue(it.npc) { askeladden() } }
        onOpNpc1("npc.makinghistory_blanin") { startDialogue(it.npc) { blanin() } }
        onOpNpc1("npc.makinghistory_dron") { startDialogue(it.npc) { dron() } }
        onOpNpc1("npc.olaf_ingrid") { startDialogue(it.npc) { ingridHradson() } }
        onOpNpc1("npc.olaf_volf") { startDialogue(it.npc) { volfOlafson() } }
        onOpNpc1("npc.viking_frem_diary") { startDialogue(it.npc) { thorodin() } }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Borrokar
    // https://oldschool.runescape.wiki/w/Transcript:Freidir
    // https://oldschool.runescape.wiki/w/Transcript:Lanzig
    // https://oldschool.runescape.wiki/w/Transcript:Freygerd
    // https://oldschool.runescape.wiki/w/Transcript:Lensa
    // https://oldschool.runescape.wiki/w/Transcript:Jennella
    // https://oldschool.runescape.wiki/w/Transcript:Sassilik
    // https://oldschool.runescape.wiki/w/Transcript:Reeso
    // TODO(content): [quest:the_fremennik_trials] eight random greetings using the Fremennik name
    // TODO(content): [quest:the_fremennik_exiles] Freygerd and Reeso during the quest
    private suspend fun Dialogue.fremennikCitizen() {
        chatNpc(angry, "I cannot speak to you outerlander! Talk to Brundt, the Chieftain!")
    }

    // https://oldschool.runescape.wiki/w/Transcript:Askeladden (before The Fremennik Trials)
    // TODO(content): [quest:the_fremennik_trials] Larravak dialogue and the pet rock
    private suspend fun Dialogue.askeladden() {
        chatPlayer(happy, "Hello there.")
        chatNpc(
            happy,
            "Hey there, buddy. You're an outerlander, huh? I'm not really supposed to talk to you.",
        )
        chatPlayer(quiz, "Why not?")
        chatNpc(
            neutral,
            "I dunno buddy. Some stupid tradition or other. We're always told not to talk to " +
                "outerlanders unless the Chieftain has allowed it.",
        )
        chatPlayer(quiz, "The Chieftain? Who is that?")
        chatNpc(
            neutral,
            "His names Brundt, buddy. He's a real boring guy. You can find him in the longhall " +
                "there.",
        )
        chatPlayer(quiz, "Can you take me to see him?")
        chatNpc(
            neutral,
            "Sorry buddy, I'm not allowed into the Longhall. Don't worry, he's easy to spot. He's " +
                "the one in the fancy helmet. Oh, and he only has one eye.",
        )
        chatPlayer(shocked, "He's a cyclops?")
        chatNpc(laugh, "Nah, nah, nah... He just wears an eyepatch! Well, see ya around buddy!")
    }

    // https://oldschool.runescape.wiki/w/Transcript:Blanin
    // TODO(content): [quest:making_history] quest and after-quest dialogue
    private suspend fun Dialogue.blanin() {
        chatPlayer(happy, "Good day.")
        val title = if (access.isBodyTypeA()) "sir" else "ma'am"
        chatNpc(happy, "Good day to you $title.")
    }

    // https://oldschool.runescape.wiki/w/Transcript:Dron
    // TODO(content): [quest:making_history] quest and after-quest dialogue
    private suspend fun Dialogue.dron() {
        chatPlayer(quiz, "Excuse me..")
        chatNpc(angry, "Leave me or I'll destroy you!")
    }

    // https://oldschool.runescape.wiki/w/Transcript:Ingrid_Hradson
    // TODO(content): [quest:the_fremennik_trials] "Good afternoon! How do you like our village?"
    // TODO(content): [quest:olafs_quest] "Hello again! Have you any word from my husband?"
    private suspend fun Dialogue.ingridHradson() {
        chatNpc(
            angry,
            "Outlander, I have work to be getting on with... Please stop bothering me.",
        )
    }

    // https://oldschool.runescape.wiki/w/Transcript:Volf_Olafson
    // TODO(content): [quest:the_fremennik_trials] "Hello there. Enjoying the view?"
    // TODO(content): [quest:olafs_quest] "Hello again! Does my father send any word...?"
    private suspend fun Dialogue.volfOlafson() {
        chatNpc(neutral, "Sorry, outlander, but I have things to be doing.")
    }

    // https://oldschool.runescape.wiki/w/Transcript:Thorodin
    // TODO(content): [diary:fremennik] "Can I have some more boots?", reward claims and the
    // Dagannoth Kings bone noting option
    // TODO(content): [quest:the_fremennik_exiles] greeting while exiled
    // TODO(content): [verify:in_game] menu shown to a player without diary progress
    private suspend fun Dialogue.thorodin() {
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
                        neutral,
                        "Thorodin, taskmaster for the Fremennik Achievement Diary.",
                    )
                    chatPlayer(quiz, "What is the Achievement Diary?")
                    explainDiary()
                    return
                }
                2 -> {
                    chatPlayer(quiz, "I have a question about my Achievement diary.")
                    if (!thorodinDiaryQuestions()) {
                        return
                    }
                }
                3 -> {
                    chatPlayer(happy, "Bye!")
                    chatNpc(happy, "See you later.")
                    return
                }
            }
        }
    }

    private suspend fun Dialogue.thorodinDiaryQuestions(): Boolean {
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
                explainDiary()
                return true
            }
            2 -> {
                thorodinRewards()
                return false
            }
            3 -> {
                chatNpc(
                    neutral,
                    "Just complete the tasks in the Fremennik Province so they're ticked off, " +
                        "then come and speak to me for your rewards.",
                )
                return true
            }
            else -> {
                chatPlayer(happy, "Bye!")
                chatNpc(happy, "See you later.")
                return false
            }
        }
    }

    private suspend fun Dialogue.explainDiary() {
        chatNpc(
            neutral,
            "It's a diary that helps you keep track of particular achievements. In the Fremennik " +
                "lands it can help you discover some quite useful things. Eventually, with " +
                "enough exploration, the inhabitants will reward you.",
        )
        chatNpc(neutral, "You can see the list of tasks on the side-panel.")
    }

    private suspend fun Dialogue.thorodinRewards() {
        chatNpc(
            neutral,
            "Well, there are four different pairs of Fremennik sea boots, which match up with the " +
                "four levels of difficulty. Each has the same rewards as the previous level and " +
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
}
