package org.rsmod.content.areas.city.falador.npcs

import jakarta.inject.Inject
import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.shops.Shops
import org.rsmod.game.entity.Player
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

/**
 * Prices mirror each shop's inv config: `sellMultiplier`, `buyMultiplier` and `delta` are stored in
 * tenths of a percent.
 */
private enum class FaladorShop(
    val title: String,
    val inv: String,
    val sell: Double,
    val buy: Double,
    val change: Double,
) {
    Shields("Cassie's Shield Shop.", "inv.shieldshop", 100.0, 60.0, 2.0),
    Maces("Flynn's Mace Market.", "inv.maceshop", 100.0, 60.0, 1.0),
    Chainmail("Wayne's Chains! - Chainmail specialist.", "inv.chainmailshop", 100.0, 65.0, 1.0),
    Gems("Herquin's Gems.", "inv.gemshop2", 100.0, 70.0, 3.0),
    General("Falador General Store", "inv.generalshop7", 130.0, 40.0, 3.0),
    Garden("Garden Centre", "inv.poh_garden_centre", 100.0, 40.0, 3.0),
}

class FaladorShopkeepersScript @Inject constructor(private val shops: Shops) : PluginScript() {
    override fun ScriptContext.startup() {
        onCounterTalk("npc.cassie") { startDialogue(it) { cassie() } }
        onCounterTrade("npc.cassie") { player.openShop(FaladorShop.Shields) }

        onCounterTalk("npc.flynn") { startDialogue(it) { flynn() } }
        onCounterTrade("npc.flynn") { player.openShop(FaladorShop.Maces) }

        onCounterTalk("npc.wayne") { startDialogue(it) { wayne() } }
        onCounterTrade("npc.wayne") { player.openShop(FaladorShop.Chainmail) }

        onCounterTalk("npc.herquin") { startDialogue(it) { herquin() } }
        onCounterTrade("npc.herquin") { player.openShop(FaladorShop.Gems) }

        for (keeper in GENERAL_STORE_STAFF) {
            onCounterTalk(keeper) { startDialogue(it) { generalStore() } }
            onCounterTrade(keeper) { player.openShop(FaladorShop.General) }
        }

        onCounterTalk("npc.poh_garden_supplier") { startDialogue(it) { gardenSupplier() } }
        onCounterTrade("npc.poh_garden_supplier") { player.openShop(FaladorShop.Garden) }
    }

    private suspend fun Dialogue.cassie() {
        chatNpc(happy, "I buy and sell shields, do you want to trade?")
        if (choice2("Yes please.", true, "No thank you.", false)) {
            player.openShop(FaladorShop.Shields)
            return
        }
        chatPlayer(neutral, "No thank you.")
    }

    private suspend fun Dialogue.flynn() {
        chatNpc(happy, "Hello. Do you want to buy or sell any maces?")
        if (choice2("No thanks.", false, "Well, I'll have a look, at least.", true)) {
            player.openShop(FaladorShop.Maces)
            return
        }
        chatPlayer(neutral, "No thanks.")
    }

    private suspend fun Dialogue.wayne() {
        chatNpc(happy, "Welcome to Wayne's Chains. Do you wanna buy or sell some chain mail?")
        if (choice2("Yes please.", true, "No thanks.", false)) {
            player.openShop(FaladorShop.Chainmail)
            return
        }
        chatPlayer(neutral, "No thanks.")
    }

    private suspend fun Dialogue.herquin() {
        val trade =
            choice2(
                "Do you wish to trade?",
                true,
                "Sorry I don't want to talk to you actually.",
                false,
            )
        if (trade) {
            chatPlayer(quiz, "Do you wish to trade?")
            chatNpc(happy, "Why yes, this a jewel shop after all.")
            player.openShop(FaladorShop.Gems)
            return
        }
        chatPlayer(neutral, "Sorry I don't want to talk to you actually.")
        chatNpc(angry, "Huh! Charming!")
    }

    private suspend fun Dialogue.generalStore() {
        chatNpc(happy, "Can I help you at all?")
        if (choice2("Yes please. What are you selling?", true, "No thanks.", false)) {
            player.openShop(FaladorShop.General)
            return
        }
        chatPlayer(neutral, "No thanks.")
    }

    private suspend fun Dialogue.gardenSupplier(
        greeting: String = "Do you want to buy some garden plants?"
    ) {
        chatNpc(quiz, greeting)
        when (choice3("Yes please!", 1, "What are these plants for?", 2, "No thanks", 3)) {
            1 -> {
                chatPlayer(happy, "Yes please!")
                player.openShop(FaladorShop.Garden)
            }
            2 -> {
                chatPlayer(quiz, "What are these plants for?")
                chatNpc(happy, "For planting in your house's garden, of course!")
                chatPlayer(quiz, "How do I do that?")
                chatNpc(
                    neutral,
                    "The same way you make furniture. You'll find plant hotspots in your garden and " +
                        "you just need to have one of my bagged saplings with you when you build at them.",
                )
                chatPlayer(neutral, "Ah, I see.")
                gardenSupplier("So do you want to look at my stock?")
            }
            else -> chatPlayer(neutral, "No thanks.")
        }
    }

    private fun Player.openShop(shop: FaladorShop) {
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
        val GENERAL_STORE_STAFF = listOf("npc.generalshopkeeper4", "npc.generalassistant4")
    }
}
