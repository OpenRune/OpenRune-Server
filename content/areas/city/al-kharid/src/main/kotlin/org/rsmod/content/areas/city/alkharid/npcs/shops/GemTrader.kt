package org.rsmod.content.areas.city.alkharid.npcs.shops

import jakarta.inject.Inject
import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.script.onOpNpc1
import org.rsmod.api.script.onOpNpc3
import org.rsmod.api.shops.Shops
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

// https://oldschool.runescape.wiki/w/Transcript:Gem_trader
// TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
class GemTrader @Inject constructor(private val shops: Shops) : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1("npc.gem_trader") { shopDialogue(it.npc) }
        onOpNpc3("npc.gem_trader") { player.openGemShop(it.npc) }
    }

    private fun Player.openGemShop(npc: Npc) {
        shops.open(this, npc, "Gem Trader.", "inv.gemshop")
    }

    private suspend fun ProtectedAccess.shopDialogue(npc: Npc) =
        startDialogue(npc) { shopKeeper(npc) }

    private suspend fun Dialogue.shopKeeper(npc: Npc) {
        chatNpc(
            happy,
            "Good day to you traveller. Would you be interested in buying some gems?",
        )
        when (choice2("Yes please.", 1, "No thank you.", 2)) {
            1 -> player.openGemShop(npc)
            2 -> {
                chatPlayer(neutral, "No thank you.")
                chatNpc(neutral, "Eh, suit yourself.")
            }
        }
    }
}
