package org.rsmod.content.areas.city.burthorpe.npcs

import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.script.onOpNpc1
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

// TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
class DeathPlateauSoldier : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1("npc.death_ig_solider_wounded") { startDialogue(it.npc) { soldier() } }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Soldier_(Death_Plateau)
    private suspend fun Dialogue.soldier() {
        chatPlayer(worried, "Are you OK?")
        chatNpc(sad, "Urrrgggh.....")
        chatNpc(
            sad,
            "I'll be OK, the trolls only leave the plateau at nightfall. The guys are bringing a " +
                "stretcher shortly.",
        )
        chatPlayer(worried, "As long as you're sure.")
        chatNpc(
            sad,
            "It's my own fault really, I was having a walk and wandered past the danger sign. " +
                "The trolls throw rocks down at any one who goes up the path!",
        )
        chatNpc(worried, "Don't go up there!")
        chatPlayer(neutral, "OK, thanks for the warning.")
    }
}
