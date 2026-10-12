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

// https://oldschool.runescape.wiki/w/Transcript:Ali_Morrisane
// TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
// TODO(content): [miniquest:rogue_trader] post-miniquest menu (blackjacks, clothes, rune caskets)
class DiscountWares @Inject constructor(private val shops: Shops) : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1("npc.feud_ali_m") { shopDialogue(it.npc) }
        onOpNpc3("npc.feud_ali_m") { player.openDiscountWares(it.npc) }
    }

    private fun Player.openDiscountWares(npc: Npc) {
        shops.open(this, npc, "Ali's Discount Wares.", "inv.feud_morrisanes")
    }

    private suspend fun ProtectedAccess.shopDialogue(npc: Npc) =
        startDialogue(npc) { shopKeeper(npc) }

    private suspend fun Dialogue.shopKeeper(npc: Npc) {
        chatNpc(happy, "Hello, my friend. Have a look at my wares.")
        when (choice2("No, I'm really too busy.", 1, "Okay.", 2)) {
            1 -> chatPlayer(neutral, "No, I'm really too busy.")
            2 -> player.openDiscountWares(npc)
        }
    }
}
