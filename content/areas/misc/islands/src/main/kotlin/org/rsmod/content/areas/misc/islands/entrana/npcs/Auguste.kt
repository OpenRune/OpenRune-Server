package org.rsmod.content.areas.misc.islands.entrana.npcs

import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.player.stat.stat
import org.rsmod.api.script.onOpNpc1
import org.rsmod.content.quest.manager.QuestRequirements
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

// https://oldschool.runescape.wiki/w/Transcript:Auguste
// TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
class Auguste : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1("npc.zep_piccard") { startDialogue(it.npc) { auguste() } }
    }

    private suspend fun Dialogue.auguste() {
        if (QuestRequirements.hasCompleted(player, ENLIGHTENED_JOURNEY)) {
            afterQuest()
            return
        }
        if (!meetsRequirements()) {
            chatNpc(happy, "Greetings!")
            chatPlayer(quiz, "What are you doing?")
            chatNpc(shifty, "Ah, it's a secret I'm afraid.")
            mesbox(
                "You do not meet all of the requirements to start the Enlightened Journey quest.",
            )
            return
        }
        chatNpc(
            happy,
            "Greetings! Would you like to be my number one accomplice? I mean, assistant?",
        )
        chatPlayer(quiz, "Who are you?")
        chatNpc(happy, "I am Auguste. I am going to be the first balloonist in all of Gielinor!")
        chatPlayer(confused, "Balloo-what? I thought only monks lived on Entrana.")
        chatNpc(
            neutral,
            "Well, they do... I was a monk. But I'm so sick of living on this island! I want to " +
                "explore new frontiers!",
        )
        chatPlayer(happy, "And go where no man has gone before?")
        chatNpc(happy, "Maybe! That sounds good. I was thinking just over to Taverley, though.")
        chatPlayer(quiz, "Why not take the boat then?")
        chatNpc(sad, "Ugh! Ocean! Don't talk to me about the ocean...oh dear...I feel ill.")
        chatPlayer(
            worried,
            "Whoa, ok, no ocean. How exactly are you going to get off the island, then?",
        )
        chatNpc(
            happy,
            "I have devised a new way to travel. But I need an assistant balloonist to help me " +
                "build my design.",
        )
        chatNpc(quiz, "Are you an experienced adventurer willing to help me?")
        if (choice2("Yes.", true, "No.", false, title = START_TITLE)) {
            chatPlayer(happy, "Yes! Sign me up.")
            // TODO(content): [quest:enlightened_journey] quest start: "Wonderful! Let's get
            // started." and the balloon construction diagram
            return
        }
        chatPlayer(neutral, "I don't think so.")
        chatNpc(sad, "Oh, never mind then.")
    }

    // TODO(content): [system:balloon_transport] the initial after-quest introduction to the
    // balloon network, and the balloon transport interface on "Yes."
    // TODO(content): [system:item_reclaim] "Could you replace some items for me?" (bomber jacket
    // and cap) and "Could you combine my cap and goggles?"
    private suspend fun Dialogue.afterQuest() {
        chatNpc(
            neutral,
            "Do you want to use the balloon? Just so you know, some locations require special " +
                "logs and high Firemaking skills.",
        )
        val option =
            choice3(
                "Yes.",
                1,
                "No.",
                2,
                "Could you tell me how to make an origami balloon?",
                3,
                title = "Select an Option",
            )
        when (option) {
            2 -> chatPlayer(neutral, "No.")
            3 -> {
                chatPlayer(quiz, "Could you tell me how to make an origami balloon?")
                chatNpc(
                    neutral,
                    "First, use the papyrus on the ball of wool. The papyrus is folded into an " +
                        "origami box and the yarn will support the heat source.",
                )
                chatNpc(
                    neutral,
                    "Next, add the unlit candle to the balloon structure. It will act as the " +
                        "heat source.",
                )
                chatNpc(happy, "Now you have an origami balloon!")
                chatNpc(
                    neutral,
                    "If you use your origami balloon with any dye, you can change the colour.",
                )
                chatNpc(
                    neutral,
                    "You can release the balloon and watch it float in the air, just use a " +
                        "tinderbox on it.",
                )
            }
        }
    }

    private fun Dialogue.meetsRequirements(): Boolean =
        vars[QUEST_POINTS] >= 20 &&
            player.stat("stat.firemaking") >= 20 &&
            player.stat("stat.farming") >= 30 &&
            player.stat("stat.crafting") >= 36

    private companion object {
        const val QUEST_POINTS = "varp.qp"
        const val ENLIGHTENED_JOURNEY = "quest_enlightenedjourney"
        const val START_TITLE = "Start the Enlightened Journey quest?"
    }
}
