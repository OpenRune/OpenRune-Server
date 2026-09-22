package org.rsmod.content.areas.city.rimmington.npcs

import jakarta.inject.Inject
import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.script.onOpNpc1
import org.rsmod.api.script.onOpNpc3
import org.rsmod.api.shops.Shops
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

/** Prices mirror each shop's inv config, in tenths of a percent. */
private enum class RimmingtonShop(
    val title: String,
    val inv: String,
    val sell: Double,
    val buy: Double,
    val change: Double,
) {
    Crafting("Rommik's Crafty Supplies.", "inv.craftingshop", 100.0, 65.0, 2.0),
    Archery("Brian's Archery Supplies.", "inv.salesman_ranging", 100.0, 65.0, 2.0),
}

class RimmingtonShopkeepersScript @Inject constructor(private val shops: Shops) : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1("npc.rommik") { startDialogue(it.npc) { rommik() } }
        onOpNpc3("npc.rommik") { player.openShop(RimmingtonShop.Crafting) }

        onOpNpc1("npc.salesman_ranging") { startDialogue(it.npc) { brian() } }
        onOpNpc3("npc.salesman_ranging") { player.openShop(RimmingtonShop.Archery) }

        for (staff in GENERAL_STORE_STAFF) {
            onOpNpc1(staff) { startDialogue(it.npc) { generalStore(it.npc) } }
            onOpNpc3(staff) { player.openGeneralStore(it.npc) }
        }
    }

    private suspend fun Dialogue.rommik() {
        chatNpc(happy, "Would you like to buy some Crafting equipment?")
        shopOffer(
            RimmingtonShop.Crafting,
            decline = "No thanks, I've got all the Crafting equipment I need.",
            declineSpoken = "No thanks; I've got all the Crafting equipment I need.",
        )
    }

    private suspend fun Dialogue.brian() {
        chatNpc(happy, "Would you like to buy some archery equipment?")
        shopOffer(
            RimmingtonShop.Archery,
            decline = "No thanks, I've got all the archery equipment I need.",
            declineSpoken = "No thanks, I've got all the archery equipment I need.",
        )
    }

    private suspend fun Dialogue.shopOffer(
        shop: RimmingtonShop,
        decline: String,
        declineSpoken: String,
    ) {
        if (choice2(decline, false, "Let's see what you've got, then.", true)) {
            player.openShop(shop)
            return
        }
        chatPlayer(neutral, declineSpoken)
        chatNpc(neutral, "Okay. Fare well on your travels.")
    }

    private suspend fun Dialogue.generalStore(npc: Npc) {
        chatNpc(happy, "Can I help you at all?")
        if (choice2("Yes please. What are you selling?", true, "No thanks.", false)) {
            player.openGeneralStore(npc)
            return
        }
        chatPlayer(neutral, "No thanks.")
    }

    private fun Player.openGeneralStore(npc: Npc) {
        shops.open(this, npc, "Rimmington General Store", "inv.generalshop6")
    }

    private fun Player.openShop(shop: RimmingtonShop) {
        shops.open(
            player = this,
            title = shop.title,
            shopInv = shop.inv,
            buyPercentage = shop.buy,
            sellPercentage = shop.sell,
            changePercentage = shop.change,
        )
    }

    private companion object {
        val GENERAL_STORE_STAFF = listOf("npc.generalshopkeeper6", "npc.generalassistant6")
    }
}
