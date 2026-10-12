package org.rsmod.content.areas.city.desert.pollnivneach.npcs.shops

import jakarta.inject.Inject
import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.script.onOpNpc1
import org.rsmod.api.script.onOpNpc3
import org.rsmod.api.shops.Shops
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

// https://oldschool.runescape.wiki/w/Transcript:Market_seller
// TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
class PollnivneachGeneralStore @Inject constructor(private val shops: Shops) : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1(MARKET_SELLER) { startDialogue(it.npc) { marketSeller(it.npc) } }
        onOpNpc3(MARKET_SELLER) { player.openStore(it.npc) }
    }

    private fun Player.openStore(npc: Npc) {
        shops.open(this, npc, "Pollnivneach general store.", "inv.pollnivneach_generalstore")
    }

    private suspend fun Dialogue.marketSeller(npc: Npc) {
        chatNpc(happy, "Greetings! Are you in need of provisions?")
        if (choice2("Yes thank you.", true, "No thanks.", false)) {
            player.openStore(npc)
        } else {
            chatPlayer(neutral, "No thanks.")
        }
    }

    private companion object {
        const val MARKET_SELLER = "npc.feud_marketseller"
    }
}
