package org.rsmod.content.areas.city.falador.npcs

import jakarta.inject.Inject
import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.shops.Shops
import org.rsmod.game.entity.Npc
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
    General("Falador General Store", "inv.generalshop4", 130.0, 40.0, 3.0),
    Garden("Garden Centre", "inv.poh_garden_centre", 100.0, 40.0, 3.0),
}

class FaladorShopkeepersScript @Inject constructor(private val shops: Shops) : PluginScript() {
    override fun ScriptContext.startup() {
        onCounterTalk("npc.cassie") { npc -> startDialogue(npc) { cassie(npc) } }
        onCounterTrade("npc.cassie") { player.openShop(it, FaladorShop.Shields) }

        onCounterTalk("npc.flynn") { npc -> startDialogue(npc) { flynn(npc) } }
        onCounterTrade("npc.flynn") { player.openShop(it, FaladorShop.Maces) }

        onCounterTalk("npc.wayne") { npc -> startDialogue(npc) { wayne(npc) } }
        onCounterTrade("npc.wayne") { player.openShop(it, FaladorShop.Chainmail) }

        onCounterTalk("npc.herquin") { npc -> startDialogue(npc) { herquin(npc) } }
        onCounterTrade("npc.herquin") { player.openShop(it, FaladorShop.Gems) }

        for (keeper in GENERAL_STORE_STAFF) {
            onCounterTalk(keeper) { npc -> startDialogue(npc) { generalStore(npc) } }
            onCounterTrade(keeper) { player.openShop(it, FaladorShop.General) }
        }

        onCounterTalk("npc.poh_garden_supplier") { npc -> startDialogue(npc) { gardenSupplier(npc) } }
        onCounterTrade("npc.poh_garden_supplier") { player.openShop(it, FaladorShop.Garden) }
    }

    private suspend fun Dialogue.cassie(npc: Npc) {
        chatNpc(happy, "I buy and sell shields, do you want to trade?")
        if (choice2("Yes please.", true, "No thank you.", false)) {
            player.openShop(npc, FaladorShop.Shields)
            return
        }
        chatPlayer(neutral, "No thank you.")
    }

    private suspend fun Dialogue.flynn(npc: Npc) {
        chatNpc(happy, "Hello. Do you want to buy or sell any maces?")
        if (choice2("No thanks.", false, "Well, I'll have a look, at least.", true)) {
            player.openShop(npc, FaladorShop.Maces)
            return
        }
        chatPlayer(neutral, "No thanks.")
    }

    private suspend fun Dialogue.wayne(npc: Npc) {
        chatNpc(happy, "Welcome to Wayne's Chains. Do you wanna buy or sell some chain mail?")
        if (choice2("Yes please.", true, "No thanks.", false)) {
            player.openShop(npc, FaladorShop.Chainmail)
            return
        }
        chatPlayer(neutral, "No thanks.")
    }

    private suspend fun Dialogue.herquin(npc: Npc) {
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
            player.openShop(npc, FaladorShop.Gems)
            return
        }
        chatPlayer(neutral, "Sorry I don't want to talk to you actually.")
        chatNpc(angry, "Huh! Charming!")
    }

    private suspend fun Dialogue.generalStore(npc: Npc) {
        chatNpc(happy, "Can I help you at all?")
        if (choice2("Yes please. What are you selling?", true, "No thanks.", false)) {
            player.openShop(npc, FaladorShop.General)
            return
        }
        chatPlayer(neutral, "No thanks.")
    }

    private suspend fun Dialogue.gardenSupplier(
        npc: Npc,
        greeting: String = "Do you want to buy some garden plants?"
    ) {
        chatNpc(quiz, greeting)
        when (choice3("Yes please!", 1, "What are these plants for?", 2, "No thanks", 3)) {
            1 -> {
                chatPlayer(happy, "Yes please!")
                player.openShop(npc, FaladorShop.Garden)
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
                gardenSupplier(npc, "So do you want to look at my stock?")
            }
            else -> chatPlayer(neutral, "No thanks.")
        }
    }

    private fun Player.openShop(npc: Npc, shop: FaladorShop) {
        shops.open(player = this, activeNpc = npc, title = shop.title, shopInv = shop.inv)
    }

    private companion object {
        val GENERAL_STORE_STAFF = listOf("npc.generalshopkeeper4", "npc.generalassistant4")
    }
}
