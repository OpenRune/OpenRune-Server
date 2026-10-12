package org.rsmod.content.areas.city.varlamore.sunsetcoast.npcs

import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.script.onOpNpc1
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

// TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
// TODO(content): [quest:the_fremennik_trials] the Fremennik-name greetings and Reinn's
//  after-quest conversation about the Colosseum
class SunsetCoastTownsfolk : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1("npc.sunset_coast_fremennik_1") { startDialogue(it.npc) { reinn() } }
        onOpNpc1("npc.sunset_coast_fremennik_2") { startDialogue(it.npc) { inga() } }
        onOpNpc1("npc.sunset_coast_fremennik_3") { startDialogue(it.npc) { arnkel() } }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Reinn_(Sunset_Coast)
    private suspend fun Dialogue.reinn() {
        chatNpc(happy, "Good day, traveller.")
        chatPlayer(happy, "And to you.")
    }

    // https://oldschool.runescape.wiki/w/Transcript:Inga_(Sunset_Coast)
    private suspend fun Dialogue.inga() {
        chatNpc(
            happy,
            "I think the locals are taking a liking to us! One of them hasn't stopped wearing " +
                "those clothes Arnkel sold her. Much better than those piddly little things she " +
                "was wearing before!",
        )
        chatPlayer(neutral, "I think they're piddly because of how warm it is here...")
        chatNpc(laugh, "Bah! A bit of heat never hurt anyone.")
        chatPlayer(neutral, "I don't think that's true.")
        chatNpc(angry, "Oh stop your whining, traveller! Now, I think it's time for a drink.")
    }

    // https://oldschool.runescape.wiki/w/Transcript:Arnkel
    private suspend fun Dialogue.arnkel() {
        chatNpc(
            happy,
            "Today is a fine day! First, I intend to drink. Next, I intend to fight. Finally, I " +
                "intend to contemplate our existence in this confusing world.",
        )
        chatPlayer(neutral, "That's quite the day.")
        chatNpc(happy, "And a fine day it will be!")
    }
}
