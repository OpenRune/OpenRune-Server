package org.rsmod.api.shops.operation

import dev.openrune.rscm.RSCM
import dev.openrune.rscm.RSCMType
import dev.openrune.types.ItemServerType
import kotlin.math.max
import kotlin.math.min
import org.rsmod.api.invtx.invTransaction
import org.rsmod.api.invtx.select
import org.rsmod.api.market.MarketPrices
import org.rsmod.api.player.output.mes
import org.rsmod.api.player.output.objExamine
import org.rsmod.api.shops.config.ShopParams
import org.rsmod.api.shops.cost.StandardGpCostCalculations
import org.rsmod.api.shops.currency.ShopCurrency
import org.rsmod.api.shops.restock.ShopRestockProcess
import org.rsmod.game.entity.Player
import org.rsmod.game.inv.InvObj
import org.rsmod.game.inv.Inventory
import org.rsmod.game.shop.Shop
import org.rsmod.game.type.getInvObj
import org.rsmod.game.type.uncert
import org.rsmod.objtx.TransactionResult

private typealias CostCalculation = StandardGpCostCalculations

public open class StandardCurrencyShopOperations(
    protected val currency: ShopCurrency,
    private val restockProcess: ShopRestockProcess,
    private val marketPrices: MarketPrices,
    private val costOf: (ItemServerType) -> Int = { it.cost },
) : StandardShopOperations {
    override fun examineShopValue(player: Player, shop: Shop, slot: Int) {
        val obj = shop.inv[slot] ?: return
        val objType = getInvObj(obj)

        val shopInitialObjCount = shop.inv.initialStockCount(obj)
        val value =
            CostCalculation.calculateShopSellSingleValue(
                initialStock = shopInitialObjCount,
                currentStock = obj.count,
                baseCost = costOf(objType),
                sellPercentage = shop.sellPercentage,
                changePercentage = shop.changePercentage,
            )
        player.mes(currency.shopCostMessage(objType.name, value))
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
        val shopInitialObjCount = shopInv.initialStockCount(obj)
        val availableCurrencyCount = currency.balance(player, sideInv)
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
                baseCost = costOf(objType),
                requestedCount = max(1, cappedRequest),
                availableCurrency = availableCurrencyCount,
                sellPercentage = shop.sellPercentage,
                changePercentage = shop.changePercentage,
            )
        val finalPurchaseCost = max(totalCost, firstObjPrice)
        val currencyObj = currency.invObj

        if (currencyObj == null && finalPurchaseCount == 0) {
            player.mes(currency.notEnoughMessage())
            return
        }

        val transaction =
            player.invTransaction(sideInv) {
                val inv = select(sideInv)
                val shopSelect = select(shopInv)
                if (currencyObj != null) {
                    delete {
                        this.from = inv
                        this.obj = currencyObj.id
                        this.strictCount = finalPurchaseCost
                    }
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
        val currencyDelIndex = if (currencyObj != null) 0 else -1
        val stockObjAddIndex = if (currencyObj != null) 2 else 1
        val currencyDel =
            if (currencyDelIndex >= 0) transaction.results.getOrNull(currencyDelIndex) else null
        val stockObjAdd = transaction.results.getOrNull(stockObjAddIndex)

        val message =
            when {
                currencyDel == TransactionResult.ObjNotFound -> currency.notEnoughMessage()
                currencyDel == TransactionResult.NotEnoughObjCount -> currency.notEnoughMessage()
                stockObjAdd == TransactionResult.NotEnoughSpace -> NOT_ENOUGH_INV_SPACE
                success && finalPurchaseCount < initialPurchaseRequest -> {
                    if (availableCurrencyCount <= finalPurchaseCost) {
                        currency.notEnoughMessage()
                    } else {
                        NOT_ENOUGH_INV_SPACE
                    }
                }
                else -> null
            }
        message?.let(player::mes)

        if (success) {
            if (currencyObj == null) {
                currency.deduct(player, finalPurchaseCost)
            }
            restockProcess += shopInv
        }

        if (shopInv[slot] == null) {
            shopInv.resetDefaultStockItem(slot, objType)
        }
    }

    /**
     * Resets the count of a default stock obj to zero when it's sold out, instead of removing it
     * from the inventory. Applies only if [slot] is within the original stock indices
     * ([dev.openrune.types.inv.UnpackedInvType.stock]).
     */
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
                baseCost = costOf(objType),
                buyPercentage = shop.buyPercentage,
                changePercentage = shop.changePercentage,
            )
        player.mes(currency.shopBuyMessage(objType.name, value))
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

        val invCappedRequest = min(sideInv.count(obj, objType), request)
        if (invCappedRequest == 0) {
            return
        }

        val uncertTypeInternalName = RSCM.getReverseMapping(RSCMType.OBJ, uncertType.id)
        val shopCurrentObjCount = shopInv.count(uncertTypeInternalName)
        val shopInitialObjCount = shopInv.initialStockCount(uncertType)
        val cappedRequest = min(Int.MAX_VALUE - shopCurrentObjCount, invCappedRequest)

        val (count, payment) =
            CostCalculation.calculateShopBuyBulkParameters(
                initialStock = shopInitialObjCount,
                currentStock = shopCurrentObjCount,
                baseCost = costOf(uncertType),
                requestedCount = max(1, cappedRequest),
                currencyCap = currency.receiveCap(player, sideInv),
                buyPercentage = shop.buyPercentage,
                changePercentage = shop.changePercentage,
            )

        val currencyObj = currency.invObj
        val transaction =
            player.invTransaction(sideInv) {
                val inv = select(sideInv)
                val shopSelect = select(shop.inv)
                delete {
                    this.from = inv
                    this.obj = obj.id
                    this.strictCount = count
                }
                insert {
                    this.into = shopSelect
                    this.obj = uncertType.id
                    this.strictCount = count
                }
                if (currencyObj != null && payment > 0) {
                    insert {
                        this.into = inv
                        this.obj = currencyObj.id
                        this.strictCount = payment
                    }
                }
            }

        if (transaction.success) {
            if (currencyObj == null) {
                currency.credit(player, payment)
            }
            restockProcess += shopInv
        }
    }

    override fun examineDesc(player: Player, inv: Inventory, shop: Shop, slot: Int) {
        val obj = inv[slot] ?: return
        val type = getInvObj(obj)
        val marketPrice = marketPrices[type] ?: 0
        player.objExamine(type, obj.count, marketPrice)
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

    public companion object {
        public const val NOT_ENOUGH_INV_SPACE: String = "You don't have enough inventory space."
    }
}
