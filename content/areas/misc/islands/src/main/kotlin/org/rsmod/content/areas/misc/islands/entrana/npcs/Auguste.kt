package org.rsmod.content.areas.misc.islands.entrana.npcs

import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.player.stat.stat
import org.rsmod.api.script.onOpNpc1
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

// https://oldschool.runescape.wiki/w/Transcript:Auguste (before Enlightened Journey)
// TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
// TODO(content): [quest:enlightened_journey] the quest itself, the balloon after the quest, the
// origami balloon, replacing the bomber jacket and cap and combining the cap with gnome goggles
class Auguste : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1("npc.zep_piccard") { startDialogue(it.npc) { auguste() } }
    }

    private suspend fun Dialogue.auguste() {
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
            return
        }
        chatPlayer(neutral, "I don't think so.")
        chatNpc(sad, "Oh, never mind then.")
    }

    private fun Dialogue.meetsRequirements(): Boolean =
        vars[QUEST_POINTS] >= 20 &&
            player.stat("stat.firemaking") >= 20 &&
            player.stat("stat.farming") >= 30 &&
            player.stat("stat.crafting") >= 36

    private companion object {
        const val QUEST_POINTS = "varp.qp"
        const val START_TITLE = "Start the Enlightened Journey quest?"
    }
}
