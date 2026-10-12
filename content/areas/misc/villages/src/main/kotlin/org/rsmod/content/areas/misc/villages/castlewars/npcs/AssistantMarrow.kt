package org.rsmod.content.areas.misc.villages.castlewars.npcs

import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.script.onOpNpc1
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

// TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
class AssistantMarrow : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1("npc.zep_assist_cast") { startDialogue(it.npc) { assistantMarrow() } }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Assistant_Marrow
    // TODO(content): [system:balloon_transport] open the balloon map on "Yes."
    private suspend fun Dialogue.assistantMarrow() {
        chatNpc(
            neutral,
            "Do you want to use the balloon? Just so you know, some locations require special " +
                "logs and high Firemaking skills.",
        )
        when (choice3("Yes.", 1, "No.", 2, "Who are you?", 3)) {
            1 -> Unit
            2 -> chatPlayer(neutral, "No.")
            else -> {
                chatPlayer(quiz, "Who are you?")
                chatNpc(
                    neutral,
                    "I am Assistant Marrow. I'm working here part time while I study to be a " +
                        "doctor.",
                )
                chatNpc(neutral, "Do you want to use the balloon?")
                if (choice2("Yes.", 1, "No.", 2) == 2) {
                    chatPlayer(neutral, "No.")
                }
            }
        }
    }
}
