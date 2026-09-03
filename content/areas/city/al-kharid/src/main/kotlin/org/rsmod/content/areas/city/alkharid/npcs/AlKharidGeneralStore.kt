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
 * AlKharidGeneralStore — Shop keeper & Shop assistant, the two clerks of the Al Kharid General
 * Store (first building east of the palace).
 *
 * Dialogue tracks the upstream generic shopkeeper transcript: both clerks run the identical
 * standard-dialogue script upstream, so one shared dialogue body serves both. Trade opens the
 * upstream `inv.generalshop1` cache inventory (`.data/raw-cache/server/shops/generalshop1.toml`) —
 * a single shared stockroom for both clerks, not two separate shops. Op3 is the cache's real Trade
 * slot: both npc types (`npc.generalshopkeeper3` id 2817, `npc.generalassistant3` id 2818) report
 * `ops=[0=Talk-to, 2=Trade]`.
 */
class AlKharidGeneralStore @Inject constructor(private val shops: Shops) : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1(SHOP_KEEPER) { startDialogue(it.npc) }
        onOpNpc1(SHOP_ASSISTANT) { startDialogue(it.npc) }
        onOpNpc3(SHOP_KEEPER) { player.openShop(it.npc) }
        onOpNpc3(SHOP_ASSISTANT) { player.openShop(it.npc) }
    }

    private fun Player.openShop(npc: Npc) {
        shops.open(this, npc, SHOP_TITLE, SHOP_INV)
    }

    private suspend fun ProtectedAccess.startDialogue(npc: Npc) {
        startDialogue(npc) { clerkDialogue(npc) }
    }

    private suspend fun Dialogue.clerkDialogue(npc: Npc) {
        chatNpc(happy, "Can I help you at all?")
        val choice = choice2("Yes please. What are you selling?", OPT_YES, "No thanks.", OPT_NO)
        when (choice) {
            OPT_YES -> {
                chatPlayer(happy, "Yes please. What are you selling?")
                player.openShop(npc)
            }
            OPT_NO -> chatPlayer(neutral, "No thanks.")
        }
    }

    public companion object {
        public const val SHOP_KEEPER: String = "npc.generalshopkeeper3"
        public const val SHOP_ASSISTANT: String = "npc.generalassistant3"
        public const val SHOP_INV: String = "inv.generalshop1"
        public const val SHOP_TITLE: String = "Al Kharid General Store"

        private const val OPT_YES: Int = 1
        private const val OPT_NO: Int = 2
    }
}
