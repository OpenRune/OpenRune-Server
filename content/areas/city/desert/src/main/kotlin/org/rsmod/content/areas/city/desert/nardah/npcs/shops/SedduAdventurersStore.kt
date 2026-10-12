package org.rsmod.content.areas.city.desert.nardah.npcs.shops

import jakarta.inject.Inject
import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.script.onOpNpc1
import org.rsmod.api.script.onOpNpc3
import org.rsmod.api.shops.Shops
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

// https://oldschool.runescape.wiki/w/Transcript:Seddu
// TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
class SedduAdventurersStore @Inject constructor(private val shops: Shops) : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1(SEDDU) { startDialogue(it.npc) { seddu(it.npc) } }
        onOpNpc3(SEDDU) { player.openStore(it.npc) }
    }

    private fun Player.openStore(npc: Npc) {
        shops.open(this, npc, "Seddu's Adventurer's Store.", "inv.armourshopnardah")
    }

    private suspend fun Dialogue.seddu(npc: Npc) {
        chatNpc(happy, "I buy and sell adventurer's equipment, do you want to trade?")
        if (choice2("Yes please.", true, "No thank you.", false)) {
            player.openStore(npc)
        } else {
            chatPlayer(neutral, "No thank you.")
        }
    }

    private companion object {
        const val SEDDU = "npc.elid_shield_seller"
    }
}
