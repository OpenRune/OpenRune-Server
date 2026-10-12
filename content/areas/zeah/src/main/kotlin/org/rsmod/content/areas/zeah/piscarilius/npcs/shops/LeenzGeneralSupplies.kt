package org.rsmod.content.areas.zeah.piscarilius.npcs.shops

import jakarta.inject.Inject
import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.script.onOpNpc1
import org.rsmod.api.script.onOpNpc3
import org.rsmod.api.shops.Shops
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

// https://oldschool.runescape.wiki/w/Transcript:Leenz
// TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
class LeenzGeneralSupplies @Inject constructor(private val shops: Shops) : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1(LEENZ) { startDialogue(it.npc) { leenz(it.npc) } }
        onOpNpc3(LEENZ) { player.openShop(it.npc) }
    }

    private fun Player.openShop(npc: Npc) {
        shops.open(this, npc, "Leenz's General Supplies", "inv.piscarilius_generalstore")
    }

    private suspend fun Dialogue.leenz(npc: Npc) {
        chatNpc(
            happy,
            "A great adventurer, from afar? How may I help with my repertoire?",
        )
        if (choice2("Let's trade.", true, "I don't need anything right now.", false)) {
            chatPlayer(happy, "Let's trade.")
            player.openShop(npc)
        } else {
            chatPlayer(neutral, "I don't need anything right now.")
        }
    }

    private companion object {
        const val LEENZ = "npc.piscarilius_generalstore_keeper"
    }
}
