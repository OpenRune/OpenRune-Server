package org.rsmod.content.areas.misc.islands.fossilisland.npcs

import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.script.onOpNpc1
import org.rsmod.api.script.onOpNpc3
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

// https://oldschool.runescape.wiki/w/Transcript:Peter
// TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
// TODO(content): [system:fossil_island_tasks] the notice board task set rewards (5 unidentified
// small fossils for the Museum Camp set; the other sets' dialogue is missing from the wiki)
// TODO(content): [system:fossil_drops] fossil drops that respect the two toggles
class Peter : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1(PETER) { startDialogue(it.npc) { peter() } }
        onOpNpc3(PETER) { startDialogue(it.npc) { toggleFossils() } }
    }

    private suspend fun Dialogue.peter() {
        val option =
            choice2(
                "Talk about the island camp.",
                1,
                "Talk about fossils.",
                2,
                title = "Select an option",
            )
        when (option) {
            1 -> {
                chatPlayer(quiz, "What can I do around here?")
                chatNpc(
                    happy,
                    "There's a list pinned up on the camp notice board, near the toilet. Have a " +
                        "look there for useful things you could do. I've got some rewards if you " +
                        "do them all!",
                )
                chatPlayer(happy, "I see. Thank you.")
            }
            2 -> talkAboutFossils()
        }
    }

    private suspend fun Dialogue.talkAboutFossils() {
        chatPlayer(
            quiz,
            "Can I talk to you for a moment about the fossils I find on this island?",
        )
        chatNpc(happy, "Sure, would you like to talk about? Normal or rare fossils?")
        val rare =
            choice2(
                "Let's talk about rare ones.",
                true,
                "Let's talk about normal ones.",
                false,
                title = "Ask about rare or normal fossils?",
            )
        if (rare) {
            chatPlayer(quiz, "Let's talk about rare fossils, if that's okay.")
            if (vars[RARE_BLOCKED] == 0) blockRareFossils() else allowRareFossils()
        } else {
            chatPlayer(quiz, "Let's talk about normal fossils, if that's okay.")
            if (vars[NORMAL_BLOCKED] == 0) blockNormalFossils() else allowNormalFossils()
        }
    }

    private suspend fun Dialogue.blockRareFossils() {
        chatNpc(happy, "Oh definitely! Did you find a new type of fossil?!")
        chatPlayer(
            neutral,
            "Not quite... I'd like you to stop rare fossils showing up on the island.",
        )
        chatNpc(sad, "Oh..., I can do that for you, if you really want me to.")
        if (sureOrChangedMind()) {
            chatPlayer(neutral, "Yes I'm sure.")
            vars[RARE_BLOCKED] = 1
            chatNpc(
                sad,
                "So be it, I'll spread the word, rare fossils won't drop for you anymore. Come " +
                    "back to me if you change your mind.",
            )
        } else {
            chatPlayer(happy, "Actually, I've changed my mind, I'll keep collecting them.")
            chatNpc(
                happy,
                "Oh wonderful! Rare fossils are so hard to find, I'd hate for you to miss out on " +
                    "one.",
            )
        }
    }

    private suspend fun Dialogue.allowRareFossils() {
        chatNpc(
            happy,
            "So you've changed your mind then? I knew you would, who can resists the hidden " +
                "knowledge found in rare fossils?",
        )
        chatPlayer(neutral, "Yes I know, can you just do it for me.")
        chatNpc(neutral, "If that's what you really want.")
        if (sureOrNevermind()) {
            chatPlayer(neutral, "Yes I'm sure.")
            vars[RARE_BLOCKED] = 0
            chatNpc(
                happy,
                "Of course you are! I'll get the word out and you'll start to see rare fossils " +
                    "drop again.",
            )
        } else {
            chatPlayer(neutral, "Eh, nevermind.")
            chatNpc(neutral, "Fine, just let me know if you actually change your mind.")
        }
    }

    private suspend fun Dialogue.blockNormalFossils() {
        chatNpc(happy, "Of course, I'm always up for an exciting discussion about fossils.")
        chatPlayer(
            neutral,
            "Is it possible that you could stop normal fossils being given to me on the island.",
        )
        chatNpc(confused, "... Why would you want that?")
        chatPlayer(
            neutral,
            "I've found every fossil I want and well, I'm just not interested in them anymore.",
        )
        chatNpc(sad, "Oh... I guess can see what I can do, Only if you're sure of course.")
        if (sureOrChangedMind()) {
            chatPlayer(neutral, "Yes I'm sure.")
            vars[NORMAL_BLOCKED] = 1
            chatNpc(
                sad,
                "Fine... I'll spread the word, you won't see fossils dropped on the island " +
                    "anymore... Come back to me if you change your mind.",
            )
        } else {
            chatPlayer(happy, "Actually, I've changed my mind, I'll keep collecting them.")
            chatNpc(
                happy,
                "Really?! That's fantastic! Fossils are so much fun, you'd be missing out!",
            )
        }
    }

    private suspend fun Dialogue.allowNormalFossils() {
        chatNpc(
            neutral,
            "Oh yes... I remember, you asked me to stop fossils dropping for you on the island.",
        )
        chatPlayer(neutral, "Yes, well, I need you to let them drop again.")
        chatNpc(quiz, "Really? Are you sure?")
        if (sureOrNevermind()) {
            chatPlayer(neutral, "Yes I'm sure.")
            vars[NORMAL_BLOCKED] = 0
            chatNpc(
                happy,
                "Of course you are! I'll get the word out and you'll start to see fossils drop " +
                    "again.",
            )
        } else {
            chatPlayer(neutral, "Eh, nevermind.")
            chatNpc(neutral, "Okay... let me know if you actually change your mind.")
        }
    }

    private suspend fun Dialogue.sureOrChangedMind(): Boolean =
        choice2("Yes I'm sure.", true, "Actually, I've changed my mind.", false)

    private suspend fun Dialogue.sureOrNevermind(): Boolean =
        choice2("Yes I'm sure.", true, "Nevermind", false)

    private suspend fun Dialogue.toggleFossils() {
        val rare =
            choice2("Rare.", true, "Normal.", false, title = "Toggle rare or normal fossils?")
        if (rare) {
            if (vars[RARE_BLOCKED] == 0) {
                vars[RARE_BLOCKED] = 1
                chatNpc(
                    neutral,
                    "That has been done, rare fossils won't drop for you anymore. Come back to " +
                        "me if you change your mind.",
                )
            } else {
                vars[RARE_BLOCKED] = 0
                chatNpc(
                    happy,
                    "Excellent, you'll start to see rare fossils showing up on the island again.",
                )
            }
            return
        }
        if (vars[NORMAL_BLOCKED] == 0) {
            vars[NORMAL_BLOCKED] = 1
            chatNpc(
                neutral,
                "Fine, regular fossils won't drop for you anymore. Come back to me if you change " +
                    "your mind",
            )
        } else {
            vars[NORMAL_BLOCKED] = 0
            chatNpc(happy, "Great! You'll start to see fossils showing up on the island again.")
        }
    }

    private companion object {
        const val PETER = "npc.fossil_panner"
        const val NORMAL_BLOCKED = "varbit.fossil_drop_blocked"
        const val RARE_BLOCKED = "varbit.fossil_drop_rare_blocked"
    }
}
