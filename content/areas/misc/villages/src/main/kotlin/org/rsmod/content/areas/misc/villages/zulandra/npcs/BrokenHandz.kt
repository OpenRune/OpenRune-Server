package org.rsmod.content.areas.misc.villages.zulandra.npcs

import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.script.onOpNpc1
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

// https://oldschool.runescape.wiki/w/Transcript:Broken_Handz
// TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
class BrokenHandz : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1("npc.snakeboss_spectator") { startDialogue(it.npc) { brokenHandz() } }
    }

    private suspend fun Dialogue.brokenHandz() {
        chatNpc(
            happy,
            "If you'd like to see someone fighting Zulrah, feel free to borrow my telescope.",
        )
        if (choice2("What do you mean?", true, "Thanks.", false)) {
            chatPlayer(quiz, "What do you mean?")
            chatNpc(
                neutral,
                "Zulrah, the giant snake. Its lair is deep in the swamp, so I've set up a " +
                    "telescope so I can watch people fighting it.",
            )
        }
        chatPlayer(happy, "Thanks.")
    }
}
