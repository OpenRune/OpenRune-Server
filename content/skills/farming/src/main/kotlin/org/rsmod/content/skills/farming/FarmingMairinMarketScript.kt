package org.rsmod.content.skills.farming

import jakarta.inject.Inject
import org.rsmod.api.market.MarketPrices
import org.rsmod.api.player.output.mes
import org.rsmod.api.script.onOpNpc3
import org.rsmod.api.shops.Shops
import org.rsmod.api.shops.currency.ShopCurrencyRows
import org.rsmod.api.shops.operation.ShopOperationMap
import org.rsmod.api.shops.operation.StandardCurrencyShopOperations
import org.rsmod.api.shops.restock.ShopRestockProcess
import org.rsmod.game.entity.Player
import org.rsmod.game.inv.Inventory
import org.rsmod.game.shop.Shop
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class FarmingMairinMarketScript @Inject constructor(
    private val shops: Shops,
    private val shopOps: ShopOperationMap,
    private val restockProcess: ShopRestockProcess,
    private val marketPrices: MarketPrices,
) : PluginScript() {
    private val marketOperations by lazy {
        val row = ShopCurrencyRows.all().single { it.key == CURRENCY }
        object : StandardCurrencyShopOperations(
            ShopCurrencyRows.toShopCurrency(row),
            restockProcess,
            marketPrices,
        ) {
            override fun examineInvValue(player: Player, sideInv: Inventory, shop: Shop, slot: Int) {
                player.mes("You can't sell this item to this shop.")
            }

            override fun invSell(
                player: Player,
                sideInv: Inventory,
                shop: Shop,
                slot: Int,
                request: Int,
            ) {
                player.mes("You can't sell this item to this shop.")
            }
        }
    }

    override fun ScriptContext.startup() {
        onOpNpc3("npc.fossil_mermaid_deepwater") {
            shopOps[CURRENCY] = marketOperations
            shops.open(
                player = player,
                title = "Mairin's Market",
                shopInv = "inv.skill_guide_thieving_other",
                buyPercentage = 100.0,
                sellPercentage = 100.0,
                changePercentage = 0.0,
                currency = CURRENCY,
            )
        }
    }

    private companion object {
        const val CURRENCY = "currency.mermaid_tears"
    }
}
