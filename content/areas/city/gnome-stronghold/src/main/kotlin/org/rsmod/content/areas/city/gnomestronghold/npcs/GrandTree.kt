package org.rsmod.content.areas.city.gnomestronghold.npcs

import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.script.onOpNpc1
import org.rsmod.api.script.onOpNpc3
import org.rsmod.content.quest.manager.QuestRequirements
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

// TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
// TODO(content): [system:gnome_restaurant] delivery hand-in dialogue for Narnode and Errdo
class GrandTree : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1("npc.grandtree_narnode") { startDialogue(it.npc) { kingNarnode() } }
        onOpNpc3("npc.pilot_grand_tree_base") { startDialogue(it.npc) { captainErrdo() } }
    }

    private suspend fun Dialogue.kingNarnode() {
        when {
            QuestRequirements.hasCompleted(player, MONKEY_MADNESS_II) -> narnodeAfterMm2()
            QuestRequirements.hasCompleted(player, MONKEY_MADNESS_I) -> narnodeAfterMm1()
            QuestRequirements.hasCompleted(player, GRAND_TREE) -> narnodeBeforeMm1()
            else -> narnodeBeforeGrandTree()
        }
    }

    // https://oldschool.runescape.wiki/w/Transcript:The_Grand_Tree (The Dying Tree)
    // TODO(content): [quest:the_grand_tree] quest start: "You seem worried, what's up?" leads
    //  Narnode down the trapdoor to the start offer; quest stages
    private suspend fun Dialogue.narnodeBeforeGrandTree() {
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

    // https://oldschool.runescape.wiki/w/Transcript:King_Narnode_Shareen
    // TODO(content): [quest:monkey_madness_i] quest stages
    private suspend fun Dialogue.narnodeBeforeMm1() {
        chatNpc(happy, "Adventurer! It is good to see you again.")
        chatPlayer(quiz, "And you too, King. How fares the tree?")
        chatNpc(
            happy,
            "The tree? It is fine, as it has always been since we foiled Glough's plans.",
        )
        chatPlayer(quiz, "Good. What ever did happen to Glough?")
        chatNpc(
            neutral,
            "Oh, I forced him to resign. I have now appointed a new Head Tree Guardian, Daero. " +
                "He is learning quickly and serves me well.",
        )
        chatPlayer(quiz, "King, you look worried. Is anything the matter?")
        chatNpc(worried, "Nothing in particular ... Well actually, yes - there is.")
        chatPlayer(quiz, "What is it?")
        chatNpc(worried, "Well, do you remember Glough's ship building facilities in Karamja?")
        chatPlayer(quiz, "Yes, they were on the eastern coast. What of them?")
        chatNpc(
            worried,
            "After you defeated Glough's demon I sent an envoy of my Royal Guard, the 10th " +
                "squad, to oversee the decommission of the shipyard. They were ordered to use " +
                "force if necessary.",
        )
        chatPlayer(quiz, "I see ... Were they successful?")
        chatNpc(
            worried,
            "I ... I don't know. I have heard nothing from them. I do not even know if they " +
                "reached the shipyard!",
        )
        chatPlayer(neutral, "It is a long way...")
        chatNpc(
            worried,
            "But I need to know what happened. These are elite soldiers - their disappearance " +
                "cannot simply be ignored. I cannot wait any longer.",
        )
        chatNpc(
            quiz,
            "And so I ask you: would you visit Glough's old shipyard in Karamja and find out if " +
                "the 10th squad ever managed to reach?",
        )
        if (choice2("Yes.", true, "No.", false, title = "Start the Monkey Madness I quest?")) {
            chatPlayer(happy, "Ok, I'll do it.")
            // TODO(content): [quest:monkey_madness_i] quest start: the Royal Seal and the start
            //  of the quest
            return
        }
        chatPlayer(neutral, "I'm afraid I am too busy right now.")
        chatNpc(neutral, "As you wish. Return to me when you have the time.")
    }

    // https://oldschool.runescape.wiki/w/Transcript:King_Narnode_Shareen
    // TODO(content): [quest:monkey_madness_ii] quest start for a player with the requirements,
    //  and quest stages
    private suspend fun Dialogue.narnodeAfterMm1() {
        chatPlayer(happy, "Hello there, King Narnode.")
        chatNpc(happy, "Good of you to stop by, human.")
        chatPlayer(quiz, "How fares the kingdom today?")
        chatNpc(
            neutral,
            "Everything seems fine, as far as I can tell... I will let you know if there is any " +
                "trouble.",
        )
        mesbox("You do not meet all of the requirements to start the Monkey Madness II quest.")
    }

    // https://oldschool.runescape.wiki/w/Transcript:King_Narnode_Shareen
    // TODO(content): [system:royal_seed_pods] King Narnode's Royal Seed Pods shop; the cache has
    //  no shop inv for it
    private suspend fun Dialogue.narnodeAfterMm2() {
        chatPlayer(happy, "Hello, King Narnode.")
        chatNpc(happy, "Good to see you again, ${player.displayName}.")
        val option =
            choice2(
                "Have you any more Royal Seed Pods?",
                1,
                "How fares the kingdom today?",
                2,
                title = "Select an Option",
            )
        if (option == 1) {
            chatPlayer(quiz, "Have you any more Royal Seed Pods?")
            return
        }
        chatPlayer(quiz, "How fares the kingdom today?")
        chatNpc(happy, "All is well in the Stronghold thanks to you, ${player.displayName}.")
    }

    // https://oldschool.runescape.wiki/w/Transcript:Captain_Errdo
    // TODO(content): [system:gnome_glider] the Glider op and the glider transport interface
    private suspend fun Dialogue.captainErrdo() {
        if (QuestRequirements.hasCompleted(player, GRAND_TREE)) {
            captainErrdoAfterQuest()
            return
        }
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

    private suspend fun Dialogue.captainErrdoAfterQuest() {
        val option =
            choice3(
                "Can you take me on the glider?",
                1,
                "Why are gliders better than other transport?",
                2,
                "Sorry, I don't want anything now.",
                3,
                title = "Select an option",
            )
        when (option) {
            1 -> {
                chatPlayer(quiz, "Can you take me on the glider?")
                chatNpc(happy, "Of course!")
            }
            2 -> {
                chatPlayer(quiz, "Why are gliders better than other transport?")
                chatNpc(
                    happy,
                    "Oh we have a whole network! It's wonderful for getting to hard to reach " +
                        "places.",
                )
                chatNpc(happy, "There are so many places where your teleports cannot reach!")
                chatPlayer(quiz, "How did you all manage to build such an established network?")
                chatNpc(laugh, "I think you'll find that is a gnome trade secret!")
            }
            3 -> chatPlayer(neutral, "Sorry, I don't want anything now.")
        }
    }

    private companion object {
        const val GRAND_TREE = "quest_grandtree"
        const val MONKEY_MADNESS_I = "quest_monkeymadness1"
        const val MONKEY_MADNESS_II = "quest_monkeymadness2"
    }
}
