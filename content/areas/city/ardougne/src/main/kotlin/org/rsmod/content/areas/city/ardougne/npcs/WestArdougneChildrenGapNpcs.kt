package org.rsmod.content.areas.city.ardougne.npcs

import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.script.onOpNpc1
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

// TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
class WestArdougneChildrenGapNpcs : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1("npc.w_ardoungechild1") { startDialogue(it.npc) { child() } }
        onOpNpc1("npc.dskin_w_ardoungechild1") { startDialogue(it.npc) { child() } }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Child
    private suspend fun Dialogue.child() {
        chatPlayer(happy, "Hello there.")
        if (MOURNER_GEAR.all { player.worn.count(it) > 0 }) {
            chatNpc(worried, "I'm not allowed to speak with Mourners.")
        } else {
            chatNpc(worried, "I'm not allowed to speak with strangers.")
        }
    }

    private companion object {
        val MOURNER_GEAR =
            listOf(
                "obj.gasmask",
                "obj.mourning_mourner_top",
                "obj.mourning_mourner_legs",
                "obj.mourning_mourner_gloves",
                "obj.mourning_mourner_boots",
                "obj.mourning_mourner_cloak",
            )
    }
}
