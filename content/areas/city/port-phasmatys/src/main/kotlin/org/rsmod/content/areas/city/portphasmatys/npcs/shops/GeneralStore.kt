package org.rsmod.content.areas.city.portphasmatys.npcs.shops

import jakarta.inject.Inject
import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.script.onOpNpc1
import org.rsmod.api.script.onOpNpc3
import org.rsmod.api.shops.Shops
import org.rsmod.content.areas.city.portphasmatys.npcs.understandsGhosts
import org.rsmod.content.areas.city.portphasmatys.npcs.wail
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

// https://oldschool.runescape.wiki/w/Transcript:Ghost_shopkeeper
// TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
// TODO(content): [verify:in_game] whether Trade opens the shop without a ghostspeak amulet
class GeneralStore @Inject constructor(private val shops: Shops) : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1(SHOPKEEPER) { startDialogue(it.npc) { shopkeeper(it.npc) } }
        onOpNpc3(SHOPKEEPER) { player.openShop(it.npc) }
    }

    private fun Player.openShop(npc: Npc) {
        shops.open(this, npc, "Port Phasmatys General Store", "inv.ahoy_generalshop")
    }

    private suspend fun Dialogue.shopkeeper(npc: Npc) {
        if (!player.understandsGhosts()) {
            return wail()
        }
        chatNpc(neutral, "Can I help you at all?")
        when (choice2("Yes please. What are you selling?", 1, "No thanks.", 2)) {
            1 -> player.openShop(npc)
            2 -> chatPlayer(neutral, "No thanks.")
        }
    }

    private companion object {
        const val SHOPKEEPER = "npc.ahoy_ghost_shopkeeper"
    }
}
