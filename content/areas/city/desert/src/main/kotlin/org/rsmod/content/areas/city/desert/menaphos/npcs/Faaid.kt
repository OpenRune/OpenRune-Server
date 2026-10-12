package org.rsmod.content.areas.city.desert.menaphos.npcs

import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.script.onOpNpc1
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

// https://oldschool.runescape.wiki/w/Transcript:Faaid
// TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
class Faaid : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1("npc.menaphos_faaid") { startDialogue(it.npc) { faaid() } }
    }

    private suspend fun Dialogue.faaid() {
        chatNpc(neutral, "Hello there, traveller.")
        chatPlayer(quiz, "What's going on here? Where is everyone?")
        chatNpc(
            sad,
            "Most are in the city. As for the rest, hopefully somewhere nicer than this place.",
        )
        chatPlayer(quiz, "In the city?")
        chatNpc(
            neutral,
            "Yes. When we heard they were going to lock Menaphos down and shut the gates, most " +
                "people here decided they'd be better off behind the walls than outside them.",
        )
        chatPlayer(quiz, "But you stayed?")
        chatNpc(
            sad,
            "We didn't get much warning. People had to rush to gather whatever they could. Some " +
                "things had to be left behind, along with some people...",
        )
        chatPlayer(shocked, "That's horrible!")
        chatNpc(
            sad,
            "It could have been worse. Had we been in Sophanem, we'd have had no warning at all. " +
                "I just hope it won't be long before things are back to normal.",
        )
        chatPlayer(neutral, "I hope so too.")
        chatNpc(neutral, "Thank you, traveller. Stay safe out there. These are troubling times.")
    }
}
