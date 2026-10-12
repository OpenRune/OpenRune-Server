package org.rsmod.content.areas.city.seersvillage.npcs

import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.player.stat.basePrayerLvl
import org.rsmod.api.script.onOpNpc1
import org.rsmod.content.quest.manager.QuestRequirements
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

// TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
class SorcerersTowerGapNpcs : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1("npc.thormac") { startDialogue(it.npc) { thormac() } }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Thormac
    // https://oldschool.runescape.wiki/w/Transcript:Scorpion_Catcher
    // TODO(content): [quest:scorpion_catcher] the quest from "Start the Scorpion Catcher quest?" on
    //  and the in-quest dialogue
    // TODO(content): [system:battlestaff_enchanting] the enchanting interface after the cost line
    // TODO(content): [diary:kandarin] 30,000 and 20,000 coin costs with the hard and elite diaries
    private suspend fun Dialogue.thormac() {
        if (QuestRequirements.hasCompleted(player, SCORPION_CATCHER)) {
            chatNpc(happy, "Thank you for rescuing my scorpions.")
            val option =
                choice2("That's ok.", 1, "You said you'd enchant my battlestaff for me.", 2)
            if (option == 1) {
                chatPlayer(neutral, "That's ok.")
                return
            }
            chatPlayer(quiz, "You said you'd enchant my battlestaff for me.")
            chatNpc(
                neutral,
                "Yes, it'll cost you 40,000 coins for the materials needed though. Which sort of " +
                    "staff did you want enchanting?",
            )
            return
        }
        val meetsRequirements =
            player.basePrayerLvl >= PRAYER_REQUIREMENT &&
                QuestRequirements.hasCompleted(player, BARCRAWL)
        if (!meetsRequirements) {
            chatNpc(neutral, "Hello I am Thormac the sorcerer.")
            chatPlayer(quiz, "Do you need anything?")
            chatNpc(neutral, "Not right now, but thank you for offering.")
            mesbox("You do not meet all of the requirements to start the Scorpion Catcher quest.")
            return
        }
        chatNpc(
            neutral,
            "Hello I am Thormac the sorcerer. I don't suppose you could be of assistance to me?",
        )
        val option = choice2("What do you need assistance with?", 1, "I'm a little busy.", 2)
        if (option == 2) {
            chatPlayer(neutral, "I'm a little busy.")
            chatNpc(neutral, "Fair enough.")
            return
        }
        chatPlayer(quiz, "What do you need assistance with?")
        chatNpc(
            sad,
            "I've lost my pet scorpions. They're lesser Kharid scorpions, a very rare breed.",
        )
        chatNpc(sad, "I left their cage door open, now I don't know where they've gone.")
        chatNpc(
            sad,
            "There's three of them, and they're quick little beasties. They're all over Gielinor.",
        )
        chatPlayer(quiz, "How would I go about catching them?")
        chatNpc(neutral, "Well I have a scorpion cage here which you can use to catch them in.")
        chatNpc(
            neutral,
            "If you go up to the village of Seers, to the North of here, one of them will be " +
                "able to tell you where the scorpions are now.",
        )
    }

    private companion object {
        const val SCORPION_CATCHER = "quest_scorpioncatcher"
        const val BARCRAWL = "miniquest_barcrawl"
        const val PRAYER_REQUIREMENT = 31
    }
}
