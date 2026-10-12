package org.rsmod.content.areas.city.burthorpe.npcs

import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.player.stat.statBase
import org.rsmod.api.script.onOpNpc1
import org.rsmod.content.quest.manager.QuestRequirements
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

// https://oldschool.runescape.wiki/w/Transcript:Austri
// https://oldschool.runescape.wiki/w/Transcript:Vestri
// TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
class WhiteWolfMountainDwarves : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1("npc.tunnel_dwarf") { startDialogue(it.npc) { tunnelDwarf() } }
        onOpNpc1("npc.tunnel_dwarf1") { startDialogue(it.npc) { tunnelDwarf() } }
    }

    private suspend fun Dialogue.tunnelDwarf() {
        if (QuestRequirements.hasCompleted(player, FISHING_CONTEST)) {
            chatNpc(
                happy,
                "Welcome, oh great Fishing Champion! Feel free to pop by and use our tunnel any " +
                    "time.",
            )
            chatPlayer(happy, "Thank you, I think I'll stop by for a beer one day.")
            return
        }
        chatNpc(angry, "Hmmph. What do you want?")
        while (true) {
            val option =
                choice3(
                    "I was wondering what was down those stairs?",
                    1,
                    "I was just stopping to say hello!",
                    2,
                    "Do you have a brother?",
                    3,
                )
            when (option) {
                1 -> if (!stairs()) return
                2 -> {
                    chatPlayer(happy, "I was just stopping to say hello!")
                    chatNpc(neutral, "Hello then.")
                }
                else -> {
                    chatPlayer(quiz, "Do you have a brother?")
                    chatNpc(angry, "What if I do! It's no business of yours.")
                }
            }
        }
    }

    private suspend fun Dialogue.stairs(): Boolean {
        chatPlayer(quiz, "I was just wondering what was down those stairs?")
        chatNpc(angry, "You can't go down there!")
        val option =
            choice3(
                "I didn't want to anyway.",
                1,
                "Why not?",
                2,
                "I'm bigger than you. Let me by.",
                3,
            )
        when (option) {
            1 -> {
                chatPlayer(neutral, "I didn't want to anyway.")
                chatNpc(neutral, "Good. Because you can't.")
                chatPlayer(neutral, "Because I don't want to.")
                chatNpc(neutral, "Because you can't. So that's fine.")
                chatPlayer(neutral, "Yes, it is.")
                chatNpc(neutral, "Yes. Fine.")
                chatPlayer(neutral, "Absolutely.")
                chatNpc(neutral, "Well then.")
                return true
            }
            2 -> {
                whyNot()
                return false
            }
            else -> {
                chatPlayer(angry, "I'm bigger than you. Let me by!")
                chatPlayer(angry, "...stumpy.")
                chatNpc(angry, "Go away! You're not going to bully your way in HERE!")
                return true
            }
        }
    }

    private suspend fun Dialogue.whyNot() {
        chatPlayer(quiz, "Why not?")
        chatNpc(
            angry,
            "This is the home of the Mountain Dwarves. How would you like it if I wanted to take " +
                "a shortcut through your home?",
        )
        val option =
            choice3(
                "Ooh... is this a shortcut to somewhere?",
                1,
                "Oh, sorry, I hadn't realised it was private.",
                2,
                "If you were my friend I wouldn't mind it.",
                3,
            )
        when (option) {
            1 -> {
                chatPlayer(quiz, "Ooh... is this a shortcut to somewhere?")
                chatNpc(
                    neutral,
                    "Well, it is a lot easier to go this way to get past White Wolf Mountain " +
                        "than through those wolf filled passes.",
                )
            }
            2 -> {
                chatPlayer(sad, "Oh, sorry, I hadn't realised it was private.")
                chatNpc(angry, "Well, it is.")
            }
            3 -> {
                chatPlayer(happy, "If you were my friend I wouldn't mind.")
                chatNpc(neutral, "Yes, but I don't even know you.")
                val friends =
                    choice2(
                        "Well, let's be friends!",
                        1,
                        "You're a grumpy little man aren't you?",
                        2,
                    )
                if (friends == 2) {
                    chatPlayer(neutral, "You're a grumpy little man aren't you?")
                    chatNpc(neutral, "Don't you know it.")
                    return
                }
                chatPlayer(happy, "Well, let's be friends!")
                if (player.statBase("stat.fishing") < FISHING_CONTEST_LEVEL) {
                    chatNpc(neutral, "I'd rather not, thank you.")
                    mesbox(
                        "You do not meet all of the requirements to start the Fishing Contest " +
                            "quest.",
                    )
                    return
                }
                chatNpc(neutral, "I don't make friends easily. People need to earn my trust first.")
                // TODO(content): [quest:fishing_contest] "Start the Fishing Contest quest?" offer
                //  and start
            }
        }
    }

    private companion object {
        const val FISHING_CONTEST = "quest_fishingcontest"

        // https://oldschool.runescape.wiki/w/Fishing_Contest: 10 Fishing
        const val FISHING_CONTEST_LEVEL = 10
    }
}
