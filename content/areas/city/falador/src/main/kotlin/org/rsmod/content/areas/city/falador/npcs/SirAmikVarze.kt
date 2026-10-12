package org.rsmod.content.areas.city.falador.npcs

import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.script.onOpNpc1
import org.rsmod.content.quest.manager.QuestRequirements
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

// https://oldschool.runescape.wiki/w/Transcript:Black_Knights%27_Fortress (before the quest)
// TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
// TODO(content): [quest:black_knights_fortress] in-progress stages, and the dialogue after the
//  quest but before Wanted! (the transcript has none, so that falls back to the pre-quest lines)
class SirAmikVarze : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1("npc.sir_amik_varze") { startDialogue(it.npc) { sirAmik() } }
    }

    private suspend fun Dialogue.sirAmik() {
        if (QuestRequirements.hasCompleted(player, WANTED)) {
            sirAmikAfterWanted()
            return
        }
        chatNpc(
            neutral,
            "I am the leader of the White Knights of Falador. Why do you seek my audience?",
        )
        val quest =
            choice2("I seek a quest!", true, "I don't, I'm just looking around.", false)
        if (!quest) {
            chatPlayer(neutral, "I don't, I'm just looking around.")
            chatNpc(neutral, "Ok. Please don't break anything.")
            return
        }
        chatPlayer(happy, "I seek a quest.")
        chatNpc(
            neutral,
            "Well, I need some spy work doing but it's quite dangerous. It will involve going " +
                "into the Black Knights' fortress.",
        )
        val brave =
            choice2(
                "I laugh in the face of danger!",
                true,
                "I go and cower in a corner at the first sign of danger!",
                false,
            )
        if (brave) {
            chatPlayer(happy, "I laugh in the face of danger!")
            chatNpc(neutral, "Well that's good. Don't get too overconfident though.")
            chatNpc(
                neutral,
                "You've come along at just the right time actually. All of my knights are " +
                    "already known to the Black Knights.",
            )
            mission()
            return
        }
        chatPlayer(worried, "I go and cower in a corner at the first sign of danger!")
        chatNpc(confused, "Err....")
        chatNpc(confused, "Well.")
        chatNpc(neutral, "I... suppose spy work DOES involve a little hiding in corners.")
        val giveItAGo =
            choice2(
                "Oh. I suppose I'll give it a go then.",
                true,
                "No, I'm not ready to do that.",
                false,
            )
        if (!giveItAGo) {
            notReady()
            return
        }
        chatPlayer(neutral, "Oh. I suppose I'll give it a go then.")
        chatNpc(
            neutral,
            "You've come along at just the right time actually. All of my knights are already " +
                "known to the Black Knights.",
        )
        mission()
    }

    private suspend fun Dialogue.mission() {
        chatNpc(neutral, "Subtlety isn't exactly our strong point.")
        chatPlayer(
            quiz,
            "Can't you just take your White Knights' armour off? They wouldn't recognise you then!",
        )
        chatNpc(
            neutral,
            "I am afraid our charter prevents us using espionage in any form, that is the domain " +
                "of the Temple Knights.",
        )
        chatPlayer(quiz, "Temple Knights? Who are they?")
        chatNpc(
            neutral,
            "That information is classified. I am forbidden to share it with outsiders.",
        )
        chatPlayer(quiz, "So... what do you need doing?")
        chatNpc(
            neutral,
            "Well, the Black Knights have started making strange threats to us; demanding " +
                "large amounts of money and land, and threatening to invade Falador if we don't " +
                "pay them.",
        )
        chatNpc(neutral, "Now, NORMALLY this wouldn't be a problem...")
        chatNpc(worried, "But they claim to have a powerful new secret weapon.")
        chatNpc(
            neutral,
            "Your mission, should you decide to accept it, is to infiltrate their fortress, find " +
                "out what their secret weapon is, and then sabotage it.",
        )
        // TODO(content): [quest:black_knights_fortress] "Start the Black Knights' Fortress quest?"
    }

    private suspend fun Dialogue.notReady() {
        chatPlayer(neutral, "No, I'm not ready to do that.")
        chatNpc(neutral, "Come see me again if you change your mind.")
    }

    // https://oldschool.runescape.wiki/w/Transcript:Sir_Amik_Varze
    private suspend fun Dialogue.sirAmikAfterWanted() {
        chatPlayer(happy, "Hello Sir Amik.")
        chatNpc(happy, "Hello, friend!")
        chatNpc(
            neutral,
            "Well ${player.displayName}, now that you are a White Knight, I expect you should be " +
                "out there hunting Black Knights for us!",
        )
        val explain =
            choice2(
                "Can you explain the White Knight honour system again?",
                true,
                "Okay, bye!",
                false,
            )
        if (!explain) {
            chatPlayer(happy, "Okay, 'bye then Amik!")
            return
        }
        chatNpc(
            neutral,
            "Sadly we are not as rich as we once were, and there are many White Knights who " +
                "foolishly lose their combat equipment.",
        )
        chatNpc(
            neutral,
            "We do not think it fair to make a profit from our brethren, so we will sell you " +
                "equipment at cost, and rebuy it at the same cost, but we will only sell equipment " +
                "to those we consider responsible enough to",
        )
        chatNpc(neutral, "wield it correctly.")
        chatNpc(
            neutral,
            "By killing Black Knights, you will increase your reputation with us, by killing " +
                "White Knights we will obviously think less of you.",
        )
        chatNpc(
            neutral,
            "You can check your White Knight reputation level by looking at your quest journal " +
                "for the Wanted! Quest, or Sir Vyvin will let you know what level you are at when " +
                "you go to purchase equipment.",
        )
        chatNpc(
            neutral,
            "Sir Vyvin can be found in Falador Castle, and he will sell you any equipment " +
                "appropriate to your reputation level.",
        )
        chatNpc(happy, "Have fun, and go kill some Black Knights for me!")
        chatPlayer(happy, "Okay Amik, thanks for explaining!")
    }

    private companion object {
        const val WANTED = "quest_wanted"
    }
}
