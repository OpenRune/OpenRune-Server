package org.rsmod.content.areas.city.gnomestronghold.npcs

import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.script.onOpNpc1
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

// TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
class StrongholdGapNpcs : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1("npc.gnomeknight") { startDialogue(it.npc) { gnomeTroop() } }
        onOpNpc1("npc.d_skingnomeknight") { startDialogue(it.npc) { gnomeTroop() } }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Gnome_troop
    private suspend fun Dialogue.gnomeTroop() {
        when (access.random.of(3)) {
            0 -> {
                chatPlayer(happy, "Hi.")
                chatNpc(angry, "Draw your sword warrior, and fight along side us.")
            }
            1 -> {
                chatPlayer(happy, "Hello.")
                chatNpc(angry, "Death to Khazard, and all who serve him.")
            }
            else -> {
                chatPlayer(happy, "Hello.")
                chatNpc(
                    angry,
                    "I can't talk now, can't you see we're trying to win a battle here.",
                )
            }
        }
    }
}
