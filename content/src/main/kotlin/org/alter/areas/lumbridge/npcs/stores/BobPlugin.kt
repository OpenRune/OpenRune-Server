package org.alter.areas.lumbridge.npcs.stores

import org.alter.api.ext.*
import org.alter.game.model.Direction
import org.alter.game.model.entity.Player
import org.alter.game.model.queue.QueueTask
import org.alter.game.model.shop.PurchasePolicy
import org.alter.game.model.shop.ShopItem
import org.alter.game.pluginnew.PluginEvent
import org.alter.game.pluginnew.event.impl.onNpcOption
import org.alter.mechanics.shops.CoinCurrency
import org.alter.rscm.RSCM.getRSCM


class BobPlugin() : PluginEvent() {

    private val dialogOptions: List<String> = listOf(
        "Give me a quest!",
        "Have you anything to sell?",
        "Can you repair my items for me?",
    )

    private val storeItems = listOf(
        ShopItem(getRSCM("items.bronze_pickaxe"), 5, 1, 0),
        ShopItem(getRSCM("items.bronze_axe"), 10, 16, 9),
        ShopItem(getRSCM("items.iron_axe"), 5, 56, 33),
        ShopItem(getRSCM("items.steel_axe"), 3, 200, 120),
        ShopItem(getRSCM("items.iron_battleaxe"), 5, 182, 109),
        ShopItem(getRSCM("items.steel_battleaxe"), 2, 650, 390),
        ShopItem(getRSCM("items.mithril_battleaxe"), 1, 1690, 1014),
    )

    override fun init() {
        spawnNpc("npcs.bob", 3230, 3203, 0, 2, Direction.EAST)

        createShop("Bob's Brilliant Axes.", CoinCurrency(), purchasePolicy = PurchasePolicy.BUY_STOCK) {
            storeItems.forEachIndexed { index, item ->
                items[index] = item
            }
        }

        onNpcOption("npcs.bob", "talk-to") {
            player.queue { dialog(player) }
        }

        onNpcOption("npcs.bob", "trade") {
            player.shop()
        }
    }

    fun Player.shop() = this.openShop("Bob's Brilliant Axes.")

    suspend fun QueueTask.dialog(player: Player) {
        when (options(player, *dialogOptions.toTypedArray())) {
            1 -> {
                chatPlayer(player, "Give me a quest!")
                chatNpc(player, "Get yer own!")
            }

            2 -> player.shop()

            3 -> {
                chatPlayer(player, "Can you repair my items for me?")
                chatNpc(player, "Of course I'll repair it, though the materials may cost you. Just hand me the item and I'll take a look.")
            }
        }
    }
}
