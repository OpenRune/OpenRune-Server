package org.rsmod.content.areas.zeah.molch.npcs

import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.player.stat.statBase
import org.rsmod.api.script.onOpNpc1
import org.rsmod.content.quest.manager.QuestRequirements
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

// TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
class MolchTownsfolk : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1("npc.kebos_farmer_1") { startDialogue(it.npc) { keith() } }
        onOpNpc1("npc.ga_gordon") { startDialogue(it.npc) { gordon() } }
        onOpNpc1("npc.molch_temple_shaman") { startDialogue(it.npc) { kahtBalam() } }
        onOpNpc1("npc.molch_lizardman_guard") { startDialogue(it.npc) { infiltrator() } }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Keith
    private suspend fun Dialogue.keith() {
        chatPlayer(quiz, "Hello. Who are you?")
        chatNpc(happy, "Hello there. The name's Keith. I'm the farmer here.")
        chatPlayer(quiz, "You're pretty far out of the way here. How come?")
        chatNpc(
            neutral,
            "It's a peaceful life. Out here I can avoid the drama and politics of the big " +
                "settlements.",
        )
        chatPlayer(worried, "Aren't you worried about how close the lizardmen are though?")
        chatNpc(
            neutral,
            "The Shayzien troops keep them busy. They've not tried coming up this way.",
        )
        chatPlayer(neutral, "Fair enough. I'll leave you to it.")
    }

    // https://oldschool.runescape.wiki/w/Transcript:Gordon
    // https://oldschool.runescape.wiki/w/Transcript:Getting_Ahead
    // TODO(content): [system:met_flags] "Hello again. How are things?" on later visits
    // TODO(content): [quest:getting_ahead] start the quest after the offer, and the in-progress
    //  dialogue
    private suspend fun Dialogue.gordon() {
        if (QuestRequirements.hasCompleted(player, GETTING_AHEAD)) {
            // TODO(content): [quest:getting_ahead] "As promised, here's your reward." and the
            //  3,000 coins for players who didn't receive them from the quest
            chatNpc(happy, "Hello again. Thank you for dealing with that beast.")
            chatPlayer(happy, "It's my pleasure.")
            return
        }
        chatPlayer(quiz, "Hello. Who are you?")
        chatNpc(happy, "The name's Gordon. Good day to you.")
        chatPlayer(happy, "It's a nice farm you've got here.")
        chatNpc(happy, "Thank you. We built it ourselves.")
        chatPlayer(quiz, "Really? So you haven't been here long?")
        chatNpc(
            neutral,
            "Nope. Not too long ago you couldn't safely travel around these parts. Too many " +
                "lizardmen. But then some Hosidius explorers discovered how fertile this land was.",
        )
        chatNpc(
            neutral,
            "Lord Hosidius made a deal with Lord Shayzien and the lizardmen were cleared out to " +
                "make room for the Farming Guild.",
        )
        chatNpc(
            happy,
            "When we heard, we decided to come up here and start up our own farm.",
        )
        val help =
            choice2(
                "Need a hand with anything?",
                true,
                "Very nice. Well I hope it goes well for you.",
                false,
            )
        if (!help) {
            chatPlayer(happy, "Very nice. Well I hope it goes well for you.")
            chatNpc(happy, "Thank you friend. See you later.")
            return
        }
        chatPlayer(quiz, "Need a hand with anything?")
        val canStart =
            player.statBase("stat.crafting") >= GETTING_AHEAD_CRAFTING &&
                player.statBase("stat.construction") >= GETTING_AHEAD_CONSTRUCTION
        if (!canStart) {
            chatNpc(neutral, "I think we're good. Thank you for the offer though.")
            return
        }
        chatNpc(neutral, "Well actually, there is something we've been having a problem with.")
        chatNpc(
            worried,
            "Recently, some strange beast has been stealing our livestock at night. The soldiers " +
                "are too busy dealing with the lizardmen to help, so we haven't been able to " +
                "stop it.",
        )
        chatNpc(
            neutral,
            "You look like you know your way around a weapon. If you can kill the beast and " +
                "bring me its head as proof, I'll ensure you are compensated for your trouble.",
        )
    }

    // https://oldschool.runescape.wiki/w/Transcript:Kaht_B'alam
    private suspend fun Dialogue.kahtBalam() {
        chatNpc(quiz, "Yesss?")
        val option =
            choice3(
                "You can speak!?",
                1,
                "Why are you down here?",
                2,
                "Nevermind, bye.",
                3,
            )
        when (option) {
            1 -> {
                chatPlayer(shocked, "You can speak!?")
                chatNpc(
                    neutral,
                    "Of courssse, sssome of usss remember the high ssspeech out old massster " +
                        "ssspoke in.",
                )
                chatPlayer(
                    neutral,
                    "Hmm, I suppose I've never had a chance to speak with most Lizardmen, " +
                        "they're generally quite aggressive.",
                )
                chatNpc(
                    neutral,
                    "Yesss, we ssspeak the language of war far better. It isss the only way we " +
                        "know of.",
                )
                kahtMaster()
            }
            2 -> {
                chatPlayer(quiz, "Why are you down here?")
                chatNpc(
                    neutral,
                    "Thisss isss our temple, we don't usssually get many visssitorsss... We make " +
                        "sssacrifice, offeringsss to our massster.",
                )
                chatPlayer(worried, "What kind of... sacrifices?")
                chatNpc(neutral, "Thisss and that. Whatever out massster desssiresss.")
                kahtMaster()
            }
            else -> {
                chatPlayer(neutral, "Nevermind, bye.")
                chatNpc(neutral, "Ye niauh, human.")
            }
        }
    }

    private suspend fun Dialogue.kahtMaster() {
        chatPlayer(quiz, "This master you speak of...")
        chatNpc(neutral, "I know only what my eldersss passssed down. Very little.")
        chatPlayer(quiz, "How long ago since this master left you?")
        chatNpc(
            neutral,
            "Many generationsss have come and gone sssince he walked amongssst usss.",
        )
        chatPlayer(quiz, "Do you remember his name?")
        chatNpc(
            neutral,
            "K'uhul ajaw isss the name we ssspeak. You manfolk ssspeak a different name.",
        )
        chatPlayer(confused, "Hmmm...")
    }

    // https://oldschool.runescape.wiki/w/Transcript:Shayzien_Infiltrator
    // TODO(content): [system:kill_counts] "The Shayzien records say you've killed [#] lizardmen
    //  since they began tracking your kills. [#] were lizardman shamans."
    private suspend fun Dialogue.infiltrator() {
        val option =
            choice2(
                "Who are you?",
                true,
                "Do you know how many lizardmen I've killed?",
                false,
            )
        if (option) {
            chatPlayer(quiz, "Who are you?")
            chatNpc(
                angry,
                "Quiet! I'm here observing the lizardmen shamans, try not to blow my cover.",
            )
            chatPlayer(neutral, "You don't seem to be hiding very well.")
            chatNpc(angry, "As if you could do better!")
        } else {
            chatPlayer(quiz, "Do you know how many lizardmen I've killed?")
        }
    }

    private companion object {
        const val GETTING_AHEAD = "quest_gettingahead"
        const val GETTING_AHEAD_CRAFTING = 30
        const val GETTING_AHEAD_CONSTRUCTION = 26
    }
}
