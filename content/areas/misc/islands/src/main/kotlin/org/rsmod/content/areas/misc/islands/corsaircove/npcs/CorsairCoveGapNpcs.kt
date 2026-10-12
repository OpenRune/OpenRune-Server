package org.rsmod.content.areas.misc.islands.corsaircove.npcs

import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.script.onOpNpc1
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

// TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
class CorsairCoveGapNpcs : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1("npc.corscurs_thief") { startDialogue(it.npc) { arsen() } }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Arsen_the_Thief (before The Corsair Curse)
    // TODO(content): [quest:the_corsair_curse] thanks after the quest
    private suspend fun Dialogue.arsen() {
        chatNpc(sad, "Urgh...")
        mesbox("He looks too sick to chat.")
    }
}
