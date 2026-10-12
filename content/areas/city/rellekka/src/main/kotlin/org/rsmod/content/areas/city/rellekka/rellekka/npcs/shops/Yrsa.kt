package org.rsmod.content.areas.city.rellekka.rellekka.npcs.shops

import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.script.onOpNpc1
import org.rsmod.api.script.onOpNpc3
import org.rsmod.api.script.onOpNpc4
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

// https://oldschool.runescape.wiki/w/Transcript:Yrsa (before The Fremennik Trials)
// TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
// TODO(content): [quest:the_fremennik_trials] "Yrsa's Accoutrements" (inv.viking_clothes_shop,
// which has no shop config in the cache) and "Yrsa's Shoe Store" makeover for Fremenniks
class Yrsa : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1("npc.viking_clothing_shopkeeper") { startDialogue(it.npc) { outerlander() } }
        onOpNpc3("npc.viking_clothing_shopkeeper") { mes("Only Fremenniks may buy clothes here.") }
        onOpNpc4("npc.viking_clothing_shopkeeper") {
            mes("Only Fremenniks may change their shoes here.")
        }
    }

    private suspend fun Dialogue.outerlander() {
        chatPlayer(happy, "Hello!")
        chatNpc(
            sad,
            "I'm sorry outerlander, I cannot sell you any clothes. Our customs forbid it.",
        )
    }
}
