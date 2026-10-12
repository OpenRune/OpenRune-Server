package org.rsmod.content.areas.city.rellekka.rellekka.npcs.shops

import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.script.onOpNpc1
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

// https://oldschool.runescape.wiki/w/Transcript:Sigmund_The_Merchant (before The Fremennik Trials)
// TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
// TODO(content): [quest:the_fremennik_trials] his trading task during the Trials, and greeting
// and opening "Sigmund the Merchant" (inv.viking_general_store) for Fremenniks
// TODO(content): [verify:in_game] what the Trade option does before the Trials
class SigmundTheMerchant : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1("npc.viking_sigmund") { startDialogue(it.npc) { outerlander() } }
    }

    private suspend fun Dialogue.outerlander() {
        chatPlayer(happy, "Hiya.")
        chatNpc(
            neutral,
            "Hello outerlander. By the laws of our tribe, I am afraid I may not speak to you " +
                "without the express permission of the chieftain.",
        )
    }
}
