package org.rsmod.content.areas.city.barbarianvillage.npcs

import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.player.stat.baseWoodcuttingLvl
import org.rsmod.api.script.onOpNpc1
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

// https://oldschool.runescape.wiki/w/Transcript:Sigurd
// TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
// TODO(content): [system:canoe_axe_storage] Store-axe op and its dialogue
class Sigurd : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1("npc.canoeing_sigurd") { startDialogue(it.npc) { sigurd() } }
    }

    private suspend fun Dialogue.sigurd() {
        chatPlayer(happy, "Hello there.")
        chatNpc(laugh, "Ha Ha! Hello!")
        val option =
            choice2("Who are you?", 1, "Can you teach me about Canoeing?", 2)
        when (option) {
            1 -> whoAreYou()
            2 -> canoes()
        }
    }

    private suspend fun Dialogue.whoAreYou() {
        chatPlayer(quiz, "Who are you?")
        chatNpc(happy, "I'm Sigurd the Great and Brainy.")
        chatPlayer(quiz, "Why do they call you the Great and Brainy?")
        chatNpc(happy, "Because I invented the Log Canoe!")
        chatPlayer(confused, "Log Canoe?")
        chatNpc(
            drunk,
            "Yeash! Me and my cousins were having a great party by the river when we decided to " +
                "have a game of 'Smack The Tree'",
        )
        chatPlayer(confused, "Smack the Tree?")
        chatNpc(
            drunk,
            "It's a game were you take it in turnsh shmacking a tree. First one to uproot the " +
                "tree winsh!",
        )
        chatNpc(
            drunk,
            "Anyway, I won the game with a flying tackle. The tree came loose and down the river " +
                "bank I went, still holding the tree.",
        )
        chatNpc(
            drunk,
            "I woke up a few hours later and found myself several miles down river. And thatsh " +
                "how I invented the log canoe!",
        )
        chatPlayer(
            confused,
            "So you invented the 'Log Canoe' by falling into a river hugging a tree?",
        )
        chatNpc(angry, "Well I refined the design from the original you know!")
        chatNpc(
            happy,
            "I cut all the branches off to make it more comfortable. I could tell you how to if " +
                "you like?",
        )
        if (choice2("Yes", true, "No", false)) {
            canoes()
        } else {
            chatPlayer(neutral, "No thanks, not right now.")
        }
    }

    private suspend fun Dialogue.canoes() {
        chatPlayer(quiz, "Can you teach me about canoes?")
        val level = player.baseWoodcuttingLvl
        if (level < LOG_CANOE) {
            chatNpc(neutral, "Well, you don't look like you have the skill to make a canoe.")
            chatNpc(neutral, "You need to have at least level 12 woodcutting.")
            chatNpc(
                happy,
                "Once you are able to make a canoe it makes travel along the river much quicker!",
            )
            return
        }
        chatNpc(
            happy,
            "It's really quite simple. Just walk down to that tree on the bank and chop it down.",
        )
        chatNpc(happy, "Then take your axe to it and shape it how you like!")
        when {
            level < DUGOUT -> {
                chatNpc(
                    happy,
                    "You can make a log canoe like mine! It'll get you 1 stop down the river.",
                )
                chatNpc(
                    angry,
                    "There's some snooty fella down near the Champions Guild who reckons his " +
                        "canoes are better than mine. He's never said it to my face though.",
                )
            }
            level < STABLE_DUGOUT ->
                chatNpc(
                    neutral,
                    "You could make a Dugout canoe with your woodcutting skill, but I don't see " +
                        "why you would want to.",
                )
            level < WAKA -> {
                chatNpc(happy, "Well, you're pretty handy with an axe!")
                chatNpc(
                    neutral,
                    "You could make Stable Dugout canoes, like that snooty fella Tarquin.",
                )
                chatNpc(
                    angry,
                    "He reckons his canoes are better than mine. He's never said it to my face " +
                        "though.",
                )
            }
            else -> {
                chatNpc(
                    happy,
                    "You look like you know your way around a tree, you can make a Waka canoe.",
                )
                chatPlayer(quiz, "What's a Waka?")
                chatNpc(
                    neutral,
                    "I've only ever seen Hari using them. People say he's found a way to canoe " +
                        "the river underground and into the Wilderness",
                )
                chatNpc(neutral, "Hari hangs around up near Edgeville")
                chatNpc(happy, "He's a nice bloke.")
            }
        }
    }

    private companion object {
        const val LOG_CANOE = 12
        const val DUGOUT = 27
        const val STABLE_DUGOUT = 42
        const val WAKA = 57
    }
}
