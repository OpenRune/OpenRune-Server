package org.rsmod.content.areas.zeah.hosidius.npcs

import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.player.stat.statHeal
import org.rsmod.api.script.onOpNpc1
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

// TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
// TODO(content): [verify:in_game] the heal amount is copied from the Edgeville Monastery monks
class HosidiusMonks : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1("npc.hosidius_monk") { startDialogue(it.npc) { monk() } }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Monk (Hosidius, members world)
    private suspend fun Dialogue.monk() {
        chatNpc(happy, "Greetings traveller.")
        val option =
            choice2("Can you heal me? I'm injured.", 1, "What are you doing here?", 2)
        if (option == 1) {
            chatPlayer(sad, "Can you heal me? I'm injured.")
            chatNpc(happy, "Ok.")
            player.statHeal(HITPOINTS, constant = 2, percent = 20)
            return
        }
        chatPlayer(quiz, "What are you doing here?")
        chatNpc(happy, "We offer sanctuary to those seeking the guidance of Saradomin.")
        val followUp = choice2("What if I want guidance from another god?", 1, "Thanks.", 2)
        if (followUp == 2) {
            chatPlayer(happy, "Thanks.")
            return
        }
        chatPlayer(quiz, "What if I want guidance from another god?")
        chatNpc(
            angry,
            "If Saradomin's not good enough for you, those otherworldly freaks from Arceuus are " +
                "probably your kind of people.",
        )
        chatNpc(
            angry,
            "They're always looking for hidden knowledge about souls and spirits, when any " +
                "sensible person would just trust in Saradomin.",
        )
        chatPlayer(neutral, "I see.")
    }

    private companion object {
        const val HITPOINTS = "stat.hitpoints"
    }
}
