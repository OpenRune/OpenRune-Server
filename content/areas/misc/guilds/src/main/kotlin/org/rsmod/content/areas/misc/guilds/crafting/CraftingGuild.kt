package org.rsmod.content.areas.misc.guilds.crafting

import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.script.onOpNpc1
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

// TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
class CraftingGuild : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1("npc.zep_assist_craft") { startDialogue(it.npc) { assistantBrock() } }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Assistant_Brock
    // TODO(content): [system:balloon_transport] "Yes." and the Fly option open the balloon map
    private suspend fun Dialogue.assistantBrock() {
        chatNpc(
            neutral,
            "Do you want to use the balloon? Just so you know, some locations require special " +
                "logs and high Firemaking skills.",
        )
        when (choice3("Yes.", 1, "Who are you?", 2, "No.", 3)) {
            1 -> Unit
            2 -> {
                chatPlayer(quiz, "Who are you?")
                chatNpc(
                    neutral,
                    "I am Assistant Brock. I serve under Auguste as his number two assistant.",
                )
                chatNpc(neutral, "Do you want to use the balloon?")
                when (choice2("Yes.", 1, "No.", 2)) {
                    1 -> Unit
                    2 -> chatPlayer(neutral, "No.")
                }
            }
            3 -> chatPlayer(neutral, "No.")
        }
    }
}
