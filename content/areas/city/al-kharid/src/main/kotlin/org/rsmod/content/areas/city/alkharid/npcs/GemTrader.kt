package org.rsmod.content.areas.city.alkharid.npcs

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

/**
 * Gem trader — Gem Trader shop owner (south-east of Al-Kharid square). M5.ALKHARID.2.
 *
 * Dialogue tracks the OSRS wiki transcript (Transcript:Gem trader). Trade opens the upstream
 * `inv.gemshop` toml inventory (uncut + cut sapphire, emerald, ruby, and diamond).
 */
class GemTrader @Inject constructor(private val shops: Shops) : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1(NPC) { startDialogue(it.npc) }
        onOpNpc3(NPC) { player.openShop(it.npc) }
    }

    private fun Player.openShop(npc: Npc) {
        shops.open(this, npc, SHOP_TITLE, SHOP_INV)
    }

    private suspend fun ProtectedAccess.startDialogue(npc: Npc) {
        startDialogue(npc) { shopDialogue(npc) }
    }

    private suspend fun Dialogue.shopDialogue(npc: Npc) {
        chatNpc(happy, "Welcome to my gem store. Would you like to look at my wares?")
        val choice = choice2("Yes please.", OPT_YES, "No thanks.", OPT_NO)
        when (choice) {
            OPT_YES -> {
                chatPlayer(happy, "Yes please.")
                player.openShop(npc)
            }
            OPT_NO -> chatPlayer(neutral, "No thanks.")
        }
    }

    public companion object {
        public const val NPC: String = "npc.gem_trader"
        public const val SHOP_INV: String = "inv.gemshop"
        public const val SHOP_TITLE: String = "Gem Trader"

        private const val OPT_YES: Int = 1
        private const val OPT_NO: Int = 2
    }
}
