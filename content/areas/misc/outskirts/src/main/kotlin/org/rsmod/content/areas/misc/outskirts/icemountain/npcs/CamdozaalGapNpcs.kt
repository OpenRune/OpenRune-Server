package org.rsmod.content.areas.misc.outskirts.icemountain.npcs

import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.script.onOpNpc1
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

// TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
class CamdozaalGapNpcs : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1("npc.dt2_camdozaal_archaeologist_1_vis") { startDialogue(it.npc) { helena() } }
        onOpNpc1("npc.dt2_camdozaal_archaeologist_2_vis") { startDialogue(it.npc) { prescott() } }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Helena_(Ruins_of_Camdozaal)
    // TODO(content): [quest:below_ice_mountain] "Did you see that dwarf by the big forge?" before
    // the player has spoken to Ramarno after the quest
    private suspend fun Dialogue.helena() {
        chatPlayer(happy, "Hello there.")
        chatNpc(
            neutral,
            "Hello. Sorry, I'd love to stop and chat, but I have a new find that I need to work " +
                "on. If you need anything, I'm sure Prescott can help.",
        )
        chatPlayer(neutral, "Okay, thanks.")
        chatNpc(happy, "Not a problem.")
    }

    // https://oldschool.runescape.wiki/w/Transcript:Prescott
    // TODO(content): [quest:desert_treasure_ii] Lassar dialogue and the very long rope
    private suspend fun Dialogue.prescott() {
        chatNpc(
            happy,
            "Hello! Glad to see you're okay. I was worried you might have gone and got stuck " +
                "down that hole.",
        )
        chatPlayer(
            happy,
            "No need to worry. I'm mostly quite capable when it comes to these kinds of things!",
        )
        chatNpc(happy, "Good to hear!")
    }
}
