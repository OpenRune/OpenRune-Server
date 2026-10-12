package org.rsmod.content.areas.city.rellekka.rellekka.npcs.shops

import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.script.onOpNpc1
import org.rsmod.api.script.onOpNpc3
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

// https://oldschool.runescape.wiki/w/Transcript:Fish_monger (before The Fremennik Trials)
// TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
// TODO(content): [quest:the_fremennik_trials] greeting and opening "Fremennik Fish Monger"
// (inv.viking_fishmonger) for Fremenniks
// TODO(content): [skill:thieving] reactions after stealing from his stall
class FishMonger : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1("npc.viking_fish_monger") { startDialogue(it.npc) { outerlander() } }
        onOpNpc3("npc.viking_fish_monger") { mes(ONLY_FREMENNIKS) }
    }

    private suspend fun Dialogue.outerlander() {
        chatNpc(angry, "I don't sell to outerlanders.")
        access.mes(ONLY_FREMENNIKS)
    }

    private companion object {
        const val ONLY_FREMENNIKS = "Only Fremenniks may purchase fish here."
    }
}
