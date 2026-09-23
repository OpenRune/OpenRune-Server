package org.rsmod.content.interfaces.omnishop

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import dev.openrune.types.ItemServerType
import dev.openrune.types.aconverted.interf.IfButtonOp
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.ui.IfScriptArgs
import org.rsmod.api.script.onIfModalButton
import org.rsmod.api.script.onIfScriptTrigger
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class OmnishopScript : PluginScript() {
    override fun ScriptContext.startup() {
        onIfScriptTrigger<InfoArgs>("component.omnishop_main:trigger_request_info") {
            sendInfo(it.shop, it.index)
        }
        onIfScriptTrigger<BuyArgs>("component.omnishop_main:trigger_buy") {
            buy(it.shop, it.index, it.option)
        }
        onIfScriptTrigger<ExamineArgs>("component.omnishop_main:trigger_examine") {
            val obj = ServerCacheManager.getItem(it.obj) ?: return@onIfScriptTrigger
            mes(obj.examine)
        }
        onIfModalButton("component.omnishop_side:items") { button ->
            val obj = button.obj ?: return@onIfModalButton
            when (button.op) {
                IfButtonOp.Op2 -> sell(obj, 1)
                IfButtonOp.Op3 -> sell(obj, 5)
                IfButtonOp.Op4 -> sell(obj, 10)
                IfButtonOp.Op5 -> sell(obj, 50)
                IfButtonOp.Op10 -> mes(obj.examine)
                else -> Unit
            }
        }
    }

    private fun ProtectedAccess.sendInfo(shop: Int, index: Int) {
        val stock = OmnishopStock.find(shop, index) ?: return
        player.omnishopSelectedId = index
        runClientScript(INFO_UPDATE.asRSCM(RSCMType.CLIENTSCRIPT), shop, -1, index, stock.description(), 1, 0, 1)
    }

    private fun ProtectedAccess.buy(shop: Int, index: Int, option: Int) {
        if (shop != player.omnishopLastShop) return
        val stock = OmnishopStock.find(shop, index) ?: return
        if (!stock.buyable) return
        var quantity = BUY_QUANTITIES.getOrNull(option - 1) ?: return
        val costs = stock.buyCosts()
        for ((currency, price) in costs) {
            if (price == 0) continue
            val affordable = currencyCount(currency) / price
            if (affordable == 0) {
                mes("You don't have enough ${currency.pluralName}.")
                return
            }
            quantity = minOf(quantity, affordable)
        }
        if (!stock.obj.stackable) {
            quantity = minOf(quantity, inv.freeSpace() / stock.multiplier)
        } else if (inv.count(stock.obj.internalName) == 0 && inv.isFull()) {
            quantity = 0
        }
        if (quantity <= 0) {
            mes("You don't have enough inventory space.")
            return
        }
        for ((currency, price) in costs) {
            removeCurrency(currency, price * quantity)
        }
        invAdd(inv, stock.obj.internalName, quantity * stock.multiplier)
    }

    private fun ProtectedAccess.sell(obj: ItemServerType, requested: Int) {
        val shop = player.omnishopLastShop
        val stock = OmnishopStock.all(shop).firstOrNull { it.obj.id == obj.id }
        if (stock == null || !stock.sellable) {
            mes("You can't sell this item to this shop.")
            return
        }
        val quantity = minOf(requested, inv.count(obj.internalName))
        if (quantity == 0) return
        if (invDel(inv, obj.internalName, quantity).failure) return
        for ((currency, price) in stock.sellCosts()) {
            val currencyObj = currency.objs.firstOrNull() ?: continue
            if (price > 0) invAdd(inv, currencyObj.internalName, price * quantity)
        }
    }

    private fun ProtectedAccess.currencyCount(currency: OmnishopCurrency): Int =
        currency.objs.sumOf { inv.count(it.internalName) }

    private fun ProtectedAccess.removeCurrency(currency: OmnishopCurrency, amount: Int) {
        var remaining = amount
        for (obj in currency.objs) {
            if (remaining == 0) return
            val take = minOf(remaining, inv.count(obj.internalName))
            if (take > 0 && invDel(inv, obj.internalName, take).success) remaining -= take
        }
    }

    internal data class InfoArgs(val shop: Int, val index: Int) : IfScriptArgs

    internal data class BuyArgs(val shop: Int, val index: Int, val option: Int) : IfScriptArgs

    internal data class ExamineArgs(val obj: Int) : IfScriptArgs

    private companion object {
        const val INFO_UPDATE = "clientscript.omnishop_info_update"
        val BUY_QUANTITIES = listOf(1, 5, 10, 50)
    }
}
