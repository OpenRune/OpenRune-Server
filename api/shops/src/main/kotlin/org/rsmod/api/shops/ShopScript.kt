package org.rsmod.api.shops

import com.github.michaelbull.logging.InlineLogger
import dev.openrune.types.ItemServerType
import dev.openrune.types.aconverted.interf.IfButtonOp
import jakarta.inject.Inject
import org.rsmod.api.market.MarketPrices
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.stopInvTransmit
import org.rsmod.api.player.vars.VarPlayerIntMapSetter
import org.rsmod.api.script.onIfClose
import org.rsmod.api.script.onIfModalButton
import org.rsmod.api.shops.operation.ShopOperationMap
import org.rsmod.api.shops.operation.ShopOperations
import org.rsmod.api.shops.restock.ShopRestockProcess
import org.rsmod.game.entity.Player
import org.rsmod.game.inv.InvObj
import org.rsmod.game.shop.Shop
import org.rsmod.game.type.isAssociatedWith
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

public class ShopScript
@Inject
constructor(
    private val operationMap: ShopOperationMap,
    private val restockProcess: ShopRestockProcess,
    private val marketPrices: MarketPrices,
) : PluginScript() {
    private val logger = InlineLogger()

    override fun ScriptContext.startup() {
        val registered = operationMap.registerCurrenciesFromDb(restockProcess, marketPrices)
        if (registered == 0) {
            logger.error {
                "dbtable.shop_currency is empty or missing; no shop currencies registered"
            }
        } else {
            logger.info { "Registered $registered shop currencies from dbtable.shop_currency" }
        }

        onIfModalButton("component.shopmain:value") { selectQuantity(0, it.op, it.comsub) }
        onIfModalButton("component.shopmain:quantity1") { selectQuantity(1, it.op, it.comsub) }
        onIfModalButton("component.shopmain:quantity5") { selectQuantity(2, it.op, it.comsub) }
        onIfModalButton("component.shopmain:quantity10") { selectQuantity(3, it.op, it.comsub) }
        onIfModalButton("component.shopmain:quantity50") { selectQuantity(4, it.op, it.comsub) }
        onIfModalButton("component.shopmain:items") { shopInvButton(it.comsub, it.op, it.obj) }
        onIfModalButton("component.shopside:items") {
            shopSideInvButton(it.comsub, it.op, it.obj)
        }
        onIfClose("interface.shopmain") { player.closeShop() }
    }

    private fun ProtectedAccess.selectQuantity(mode: Int, op: IfButtonOp, sub: Int) {
        if (player.openedShop == null || op != IfButtonOp.Op1 || sub != -1) {
            return
        }
        VarPlayerIntMapSetter.set(player, "varbit.shop_quantity", mode)
        player.initShopSideInv(player.inv)
    }

    private fun ProtectedAccess.shopInvButton(
        sub: Int,
        op: IfButtonOp,
        clientObj: ItemServerType?,
    ) {
        val objSlot = sub - 1
        val shop = player.openedShop ?: return
        val shopObj = shop.inv[objSlot] ?: return
        if (isClientObjInvalid(shopObj, clientObj)) {
            return
        }
        val operations = shop.operations() ?: return
        val selectedOp = player.selectedOperation(op) ?: return
        operations.shopInvOp(player, player.inv, shop, objSlot, selectedOp)
    }

    private fun ProtectedAccess.shopSideInvButton(
        sub: Int,
        op: IfButtonOp,
        clientObj: ItemServerType?,
    ) {
        val invObj = player.inv[sub] ?: return
        if (isClientObjInvalid(invObj, clientObj)) {
            return
        }
        val shop = player.openedShop ?: return
        val operations = shop.operations() ?: return
        val selectedOp = player.selectedOperation(op) ?: return
        operations.sideInvOp(player, player.inv, shop, sub, selectedOp)
    }

    private fun Player.closeShop() {
        val shop = openedShop ?: return
        stopInvTransmit(shop.inv)
        openedShop = null
    }

    private fun Shop.operations(): ShopOperations? {
        val operations = operationMap[currency]
        if (operations != null) {
            return operations
        }
        logger.error { "Currency `${currency}` does not have valid operations. (shop=$this)" }
        return null
    }

    private fun isClientObjInvalid(invObj: InvObj, clientObj: ItemServerType?): Boolean =
        clientObj == null || !clientObj.isAssociatedWith(invObj)
}

internal fun Player.selectedOperation(op: IfButtonOp): IfButtonOp? {
    if (op != IfButtonOp.Op1) {
        return op
    }
    return when (vars["varbit.shop_quantity"]) {
        0 -> IfButtonOp.Op1
        1 -> IfButtonOp.Op2
        2 -> IfButtonOp.Op3
        3 -> IfButtonOp.Op4
        4 -> IfButtonOp.Op5
        else -> null
    }
}
