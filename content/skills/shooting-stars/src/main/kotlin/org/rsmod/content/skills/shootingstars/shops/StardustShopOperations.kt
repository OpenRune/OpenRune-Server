package org.rsmod.content.skills.shootingstars.shops

import dev.openrune.rscm.RSCM
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import dev.openrune.types.ItemServerType
import jakarta.inject.Inject
import kotlin.math.max
import kotlin.math.min
import org.rsmod.api.invtx.invTransaction
import org.rsmod.api.invtx.select
import org.rsmod.api.market.MarketPrices
import org.rsmod.api.player.output.mes
import org.rsmod.api.player.output.objExamine
import org.rsmod.api.shops.config.ShopParams
import org.rsmod.api.shops.cost.StandardGpCostCalculations
import org.rsmod.api.shops.operation.StandardShopOperations
import org.rsmod.api.shops.restock.ShopRestockProcess
import org.rsmod.api.utils.format.formatAmount
import org.rsmod.game.entity.Player
import org.rsmod.game.inv.InvObj
import org.rsmod.game.inv.Inventory
import org.rsmod.game.shop.Shop
import org.rsmod.game.type.getInvObj
import org.rsmod.game.type.uncert
import org.rsmod.objtx.TransactionResult

private typealias CostCalculation = StandardGpCostCalculations

class StardustShopOperations
@Inject
constructor(
    private val restockProcess: ShopRestockProcess,
    private val marketPrices: MarketPrices,
) : StandardShopOperations {
    private val currencyObj: ItemServerType by lazy {
        ItemServerType("obj.star_dust".asRSCM(RSCMType.OBJ))
    }

    override fun examineShopValue(player: Player, shop: Shop, slot: Int) {
        val obj = shop.inv[slot] ?: return
        val objType = getInvObj(obj)
        val shopInitialObjCount = shop.inv.initialStockCount(obj)
        val value =
            CostCalculation.calculateShopSellSingleValue(
                initialStock = shopInitialObjCount,
                currentStock = obj.count,
                baseCost = stardustCost(objType),
                sellPercentage = shop.sellPercentage,
                changePercentage = shop.changePercentage,
            )
        if (value == 1) {
            player.mes("${objType.name}: currently costs 1 stardust.")
        } else {
            player.mes("${objType.name}: currently costs ${value.formatAmount} stardust.")
        }
    }

    override fun shopBuy(player: Player, sideInv: Inventory, shop: Shop, slot: Int, request: Int) {
        val shopInv = shop.inv
        val obj = shopInv[slot] ?: return
        val objType = getInvObj(obj)

        val initialPurchaseRequest = min(shopInv.count(obj, objType), request)
        if (initialPurchaseRequest == 0) {
            player.mes("That item is currently out of stock.")
            return
        }

        val internalName = RSCM.getReverseMapping(RSCMType.OBJ, objType.id)
        val currencyInternalName = RSCM.getReverseMapping(RSCMType.OBJ, currencyObj.id)

        val shopInitialObjCount = shopInv.initialStockCount(obj)
        val availableCurrencyCount = sideInv.count(currencyInternalName)
        val cappedRequest =
            if (objType.isStackable) {
                min(Int.MAX_VALUE - sideInv.count(internalName), initialPurchaseRequest)
            } else {
                min(sideInv.freeSpace(), initialPurchaseRequest)
            }

        val (finalPurchaseCount, totalCost, firstObjPrice) =
            CostCalculation.calculateShopSellBulkParameters(
                initialStock = shopInitialObjCount,
                currentStock = obj.count,
                baseCost = stardustCost(objType),
                requestedCount = max(1, cappedRequest),
                availableCurrency = availableCurrencyCount,
                sellPercentage = shop.sellPercentage,
                changePercentage = shop.changePercentage,
            )
        val finalPurchaseCost = max(totalCost, firstObjPrice)

        val transaction =
            player.invTransaction(sideInv) {
                val inv = select(sideInv)
                val shopSelect = select(shopInv)
                delete {
                    this.from = inv
                    this.obj = currencyObj.id
                    this.strictCount = finalPurchaseCost
                }
                delete {
                    this.from = shopSelect
                    this.obj = obj.id
                    this.strictCount = finalPurchaseCount
                }
                insert {
                    this.into = inv
                    this.obj = obj.id
                    this.strictCount = finalPurchaseCount
                }
            }

        val success = transaction.success
        val currencyDel = transaction.results[0]
        val stockObjAdd = transaction.results.getOrNull(2)

        val message =
            when {
                currencyDel == TransactionResult.ObjNotFound -> NOT_ENOUGH_STARDUST
                currencyDel == TransactionResult.NotEnoughObjCount -> NOT_ENOUGH_STARDUST
                stockObjAdd == TransactionResult.NotEnoughSpace -> NOT_ENOUGH_INV_SPACE
                success && finalPurchaseCount < initialPurchaseRequest -> {
                    if (availableCurrencyCount <= finalPurchaseCost) {
                        NOT_ENOUGH_STARDUST
                    } else {
                        NOT_ENOUGH_INV_SPACE
                    }
                }
                else -> null
            }
        message?.let(player::mes)

        if (success) {
            restockProcess += shopInv
        }

        if (shopInv[slot] == null) {
            shopInv.resetDefaultStockItem(slot, objType)
        }
    }

    private fun Inventory.resetDefaultStockItem(slot: Int, objType: ItemServerType) {
        val defaultStockIndices = type.stock?.indices ?: return
        if (slot in defaultStockIndices) {
            this[slot] = InvObj(objType, count = 0)
        }
    }

    override fun examineInvValue(player: Player, sideInv: Inventory, shop: Shop, slot: Int) {
        val obj = sideInv[slot] ?: return
        val uncert = uncert(obj)
        val objType = getInvObj(uncert)

        val tradeable = objType.tradeable || objType.stockmarket
        if (!tradeable) {
            player.mes("You can't sell this item.")
            return
        }

        val saleRestricted = objType.param(ShopParams.shop_sale_restricted)
        if (saleRestricted) {
            player.mes("You can't sell this item to a shop.")
            return
        }

        val shopInv = shop.inv
        if (!shopInv.type.allStock && objType !in shopInv) {
            player.mes("You can't sell this item to this shop.")
            return
        }

        val internalName = RSCM.getReverseMapping(RSCMType.OBJ, objType.id)
        val shopCurrentObjCount = shopInv.count(internalName)
        val shopInitialObjCount = shopInv.initialStockCount(objType)
        val value =
            CostCalculation.calculateShopBuySingleValue(
                initialStock = shopInitialObjCount,
                currentStock = shopCurrentObjCount,
                baseCost = stardustCost(objType),
                buyPercentage = shop.buyPercentage,
                changePercentage = shop.changePercentage,
            )

        if (value == 1) {
            player.mes("${objType.name}: shop will buy for 1 stardust.")
        } else {
            player.mes("${objType.name}: shop will buy for ${value.formatAmount} stardust.")
        }
    }

    override fun invSell(player: Player, sideInv: Inventory, shop: Shop, slot: Int, request: Int) {
        val obj = sideInv[slot] ?: return
        val objType = getInvObj(obj)
        val uncertType = uncert(objType)

        val tradeable = uncertType.tradeable || uncertType.stockmarket
        if (!tradeable) {
            player.mes("You can't sell this item.")
            return
        }

        val saleRestricted = uncertType.param(ShopParams.shop_sale_restricted)
        if (saleRestricted) {
            player.mes("You can't sell this item to a shop.")
            return
        }

        val shopInv = shop.inv
        if (!shopInv.type.allStock && uncertType !in shopInv) {
            player.mes("You can't sell this item to this shop.")
            return
        }

        val uncertTypeInternalName = RSCM.getReverseMapping(RSCMType.OBJ, uncertType.id)
        val shopCurrentObjCount = shopInv.count(uncertTypeInternalName)
        val shopInitialObjCount = shopInv.initialStockCount(uncertType)
        val currencyCount = sideInv.count(RSCM.getReverseMapping(RSCMType.OBJ, currencyObj.id))

        val count = min(obj.count, request)
        val (finalCount, payment, _) =
            CostCalculation.calculateShopBuyBulkParameters(
                initialStock = shopInitialObjCount,
                currentStock = shopCurrentObjCount,
                baseCost = stardustCost(uncertType),
                requestedCount = count,
                currencyCap = Int.MAX_VALUE - currencyCount,
                buyPercentage = shop.buyPercentage,
                changePercentage = shop.changePercentage,
            )
        if (finalCount <= 0) {
            player.mes("The shop will not buy that item.")
            return
        }

        val transaction =
            player.invTransaction(sideInv) {
                val inv = select(sideInv)
                val shopSelect = select(shop.inv)
                delete {
                    this.from = inv
                    this.obj = obj.id
                    this.strictCount = finalCount
                }
                insert {
                    this.into = shopSelect
                    this.obj = uncertType.id
                    this.strictCount = finalCount
                }
                if (payment > 0) {
                    insert {
                        this.into = inv
                        this.obj = currencyObj.id
                        this.strictCount = payment
                    }
                }
            }

        if (transaction.success) {
            restockProcess += shopInv
        }
    }

    override fun examineDesc(player: Player, inv: Inventory, shop: Shop, slot: Int) {
        val obj = inv[slot] ?: return
        val type = getInvObj(obj)
        val marketPrice = marketPrices[type] ?: 0
        player.objExamine(type, obj.count, marketPrice)
    }

    private fun stardustCost(type: ItemServerType): Int {
        val name = RSCM.getReverseMapping(RSCMType.OBJ, type.id)
        return FIXED_PRICES[name] ?: type.cost.coerceAtLeast(1)
    }

    private fun Inventory.initialStockCount(obj: InvObj): Int = initialStockCount(obj.id)

    private fun Inventory.initialStockCount(type: ItemServerType): Int = initialStockCount(type.id)

    private fun Inventory.initialStockCount(obj: Int): Int {
        val startStock = type.stock ?: return 0
        for (i in startStock.indices) {
            val stock = startStock[i] ?: continue
            if (stock.obj == obj) {
                return stock.count
            }
        }
        return 0
    }

    companion object {
        const val NOT_ENOUGH_STARDUST = "You don't have enough stardust."
        const val NOT_ENOUGH_INV_SPACE = "You don't have enough inventory space."

        private val FIXED_PRICES =
            mapOf(
                "obj.celestial_ring" to 2000,
                "obj.star_fragment" to 3000,
                "obj.star_reward_gem_bag" to 300,
                "obj.star_pack_softclay" to 150,
                "obj.poh_tablet_shootingstar" to 50,
            )
    }
}
