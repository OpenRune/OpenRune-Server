package org.rsmod.content.areas.city.rellekka.rellekka.npcs.shops

import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.script.onOpNpc1
import org.rsmod.api.script.onOpNpc3
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

// https://oldschool.runescape.wiki/w/Transcript:Skulgrimen (before The Fremennik Trials)
// TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
// TODO(content): [quest:the_fremennik_trials] greeting and opening "Skulgrimen's Battle Gear"
// (inv.viking_weapons_shop) for Fremenniks, and rock-shell armour crafting
class Skulgrimen : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1("npc.viking_weapons_salesman") { startDialogue(it.npc) { outerlander() } }
        onOpNpc3("npc.viking_weapons_salesman") {
            mes("Only Fremenniks may purchase weapons and armour here.")
        }
    }

    private suspend fun Dialogue.outerlander() {
        chatPlayer(happy, "Hello!")
        chatNpc(
            neutral,
            "Sorry. I can't sell weapons to outerlanders. Wouldn't be right. Against our beliefs.",
        )
    }
}
