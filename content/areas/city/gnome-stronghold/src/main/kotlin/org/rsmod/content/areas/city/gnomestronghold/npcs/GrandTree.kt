package org.rsmod.content.areas.city.gnomestronghold.npcs

import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.script.onOpNpc1
import org.rsmod.api.script.onOpNpc3
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

// TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
// TODO(content): [system:gnome_restaurant] delivery hand-in dialogue for Narnode and Errdo
class GrandTree : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1("npc.grandtree_narnode") { startDialogue(it.npc) { kingNarnode() } }
        onOpNpc3("npc.pilot_grand_tree_base") { startDialogue(it.npc) { captainErrdo() } }
    }

    // https://oldschool.runescape.wiki/w/Transcript:The_Grand_Tree (The Dying Tree)
    // TODO(content): [quest:the_grand_tree] "You seem worried, what's up?" leads Narnode down the
    //  trapdoor to the start offer; quest stages and the after-quest dialogue
    // TODO(content): [quest:monkey_madness_i] start offer, stages and after-quest dialogue
    // TODO(content): [quest:monkey_madness_ii] after-quest options and the Royal Seed Pods shop
    private suspend fun Dialogue.kingNarnode() {
        chatNpc(
            happy,
            "Welcome Traveller. I am King Narnode. It's nice to see an outsider.",
        )
        chatPlayer(happy, "Hi! It seems to be a busy settlement.")
        chatNpc(worried, "For now.")
        if (choice2("You seem worried, what's up?", true, "I'll be off now.", false)) {
            chatPlayer(quiz, "You seem worried, what's up?")
            chatNpc(worried, "Traveller, can I speak to you in strictest confidence?")
            chatPlayer(neutral, "Of course sire.")
            chatNpc(worried, "Not here, follow me.")
            return
        }
        chatPlayer(neutral, "I'll be off now.")
        chatNpc(
            happy,
            "Enjoy your stay with us. There are many things to see in my kingdom.",
        )
    }

    // https://oldschool.runescape.wiki/w/Transcript:Captain_Errdo (before The Grand Tree)
    // TODO(content): [quest:the_grand_tree] after-quest dialogue with "Can you take me on the
    //  glider?"
    // TODO(content): [system:gnome_glider] the Glider op and the glider transport interface
    private suspend fun Dialogue.captainErrdo() {
        chatNpc(happy, "Welcome to Gnome Air!")
        while (true) {
            val option =
                choice4(
                    "What's Gnome Air?",
                    1,
                    "Where can you take me?",
                    2,
                    "How much for one-way to Varrock?",
                    3,
                    "I'll leave you to it.",
                    4,
                )
            when (option) {
                1 -> {
                    chatPlayer(quiz, "What's Gnome Air?")
                    chatNpc(happy, "Gnome Air is the finest airline in Gielinor!")
                    chatNpc(happy, "Well...it's the only real airline in Gielinor.")
                    chatNpc(laugh, "Ha!")
                    chatPlayer(quiz, "What do you mean by real airline?")
                    chatNpc(
                        laugh,
                        "Well there's a dodgy magic carpet operation in the desert, I doubt it " +
                            "will ever take off... Ha!",
                    )
                }
                2 -> {
                    chatPlayer(quiz, "Where can you take me?")
                    chatNpc(neutral, "Glough has ordered that I only take gnomes on Gnome Air.")
                }
                3 -> {
                    chatPlayer(quiz, "How much for one-way to Varrock?")
                    chatNpc(neutral, "I can't take you anywhere.")
                    chatPlayer(quiz, "How come?")
                    chatNpc(
                        sad,
                        "I would, but Glough has told me not to take humans on Gnome Air.",
                    )
                }
                else -> {
                    chatPlayer(neutral, "I'll leave you to it.")
                    return
                }
            }
        }
    }
}
