package org.rsmod.content.areas.city.seersvillage.npcs

import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.script.onOpNpc1
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

// https://oldschool.runescape.wiki/w/Transcript:Murder_Mystery (before starting)
// TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
// TODO(content): [quest:murder_mystery] the in-quest dialogue, and after the quest the family
//  says "Apparently you aren't as stupid as you look." while the staff say "Thank you for all
//  your help in solving the murder."
class SinclairMansion : PluginScript() {
    override fun ScriptContext.startup() {
        for (resident in MEN) {
            onOpNpc1(resident) { startDialogue(it.npc) { mesbox("He is ignoring you.") } }
        }
        for (resident in WOMEN) {
            onOpNpc1(resident) { startDialogue(it.npc) { mesbox("She is ignoring you.") } }
        }
        onOpNpc1("npc.hobbes_the_butler") { startDialogue(it.npc) { hobbes() } }
        onOpNpc1("npc.louisa_the_cook") { startDialogue(it.npc) { louisa() } }
        onOpNpc1("npc.donovan_the_family_handyman") { startDialogue(it.npc) { donovan() } }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Hobbes
    private suspend fun Dialogue.hobbes() {
        chatNpc(angry, "This is private property! Please leave!")
    }

    // https://oldschool.runescape.wiki/w/Transcript:Louisa
    private suspend fun Dialogue.louisa() {
        chatNpc(sad, "I'm far too upset to talk to random people right now.")
    }

    // https://oldschool.runescape.wiki/w/Transcript:Donovan_the_Family_Handyman
    private suspend fun Dialogue.donovan() {
        chatNpc(angry, "I have no interest in talking to gawkers.")
    }

    private companion object {
        val MEN = listOf("npc.frank_sinclair", "npc.david_sinclair")
        val WOMEN =
            listOf(
                "npc.anna_sinclair",
                "npc.carol_sinclair",
                "npc.elizabeth_sinclair",
                "npc.mary_the_maid",
            )
    }
}
