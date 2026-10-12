package org.rsmod.content.areas.city.rellekka.rellekka.npcs.shops

import org.rsmod.api.script.onOpNpc1
import org.rsmod.api.script.onOpNpc3
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

// https://oldschool.runescape.wiki/w/Transcript:Fur_trader_(Rellekka) (before The Fremennik Trials)
// TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
// TODO(content): [quest:the_fremennik_trials] greeting and opening "Fremennik Fur Trader"
// (inv.viking_furshop, whose cache name is "Ardougne Fur Stall.") for Fremenniks
// TODO(content): [skill:thieving] reactions after stealing from his stall
class FurTrader : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1("npc.viking_fur_monger") {
            startDialogue(it.npc) { chatNpc(angry, "I don't sell to outerlanders.") }
        }
        onOpNpc3("npc.viking_fur_monger") { mes("Only Fremenniks may purchase furs here.") }
    }
}
