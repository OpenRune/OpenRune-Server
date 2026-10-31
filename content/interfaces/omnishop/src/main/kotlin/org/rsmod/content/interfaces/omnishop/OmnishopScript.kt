package org.rsmod.content.interfaces.omnishop

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import dev.openrune.types.ItemServerType
import dev.openrune.types.aconverted.interf.IfButtonOp
import jakarta.inject.Inject
import org.rsmod.api.invtx.add
import org.rsmod.api.invtx.delete
import org.rsmod.api.invtx.invTransaction
import org.rsmod.api.invtx.select
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.ui.IfScriptArgs
import org.rsmod.api.player.vars.VarPlayerIntMapSetter
import org.rsmod.api.script.onIfClose
import org.rsmod.api.script.onIfModalButton
import org.rsmod.api.script.onIfScriptTrigger
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class OmnishopScript @Inject constructor() : PluginScript() {
    override fun ScriptContext.startup() {
        onIfClose("interface.omnishop_main") { player.omnishopLastShop = -1 }
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
        if (shop != player.omnishopLastShop) return
        val stock = OmnishopStock.find(shop, index) ?: return
        player.omnishopSelectedId = index
        runClientScript(INFO_UPDATE.asRSCM(RSCMType.CLIENTSCRIPT), shop, -1, index, stock.description(), 1, 0, 1)
    }

    internal fun ProtectedAccess.buy(shop: Int, index: Int, option: Int) {
        if (shop != player.omnishopLastShop) return
        val stock = OmnishopStock.find(shop, index) ?: return
        if (!stock.buyable) return
        val requested = stock.buyQuantity(option) ?: return
        val check = OmnishopPurchaseCheck(player, shop, stock.row, stock.obj)
        publish(check)
        check.failure?.let { mes(it); return }
        val maximum = minOf(requested, check.maximum)
        if (maximum <= 0) return
        val costs = stock.buyCosts()
        val payments = costs.map { (currency, price) ->
            OmnishopPayment(currency.objs, currency.points, price)
        }
        for ((index, payment) in payments.withIndex()) {
            if (payment.balance(player) < payment.price) {
                mes("You don't have enough ${costs[index].first.pluralName}.")
                return
            }
        }
        val output = if (stock.multiplier == 0) null else check.output
        val quantity = buyOmnishopStock(player, payments, output,
            stock.multiplier, maximum, check.outputVars, cert = stock.soldNoted)
        if (quantity == 0) {
            mes("You don't have enough inventory space.")
            return
        }
        publish(OmnishopPurchaseCompleted(player, shop, stock.row, stock.obj, quantity))
        redraw()
    }

    internal fun ProtectedAccess.sell(obj: ItemServerType, requested: Int) {
        val shop = player.omnishopLastShop
        val stock = OmnishopStock.all(shop).firstOrNull { it.obj.id == obj.id }
        if (stock == null || !stock.sellable) {
            mes("You can't sell this item to this shop.")
            return
        }
        val quantity = minOf(requested, inv.count(obj.internalName))
        if (quantity == 0) return
        val costs = stock.sellCosts()
        if (costs.any { (currency, price) -> currency.objs.isEmpty() || price * quantity > Int.MAX_VALUE }) return
        val result = player.invTransaction(inv, autoCommit = false) {
            val inventory = select(inv)
            delete(inventory, obj.id, quantity)
            for ((currency, price) in costs) {
                if (price > 0) add(inventory, currency.objs.first().id, (price * quantity).toInt())
            }
        }
        if (result.failure) {
            mes("You don't have enough inventory space.")
            return
        }
        result.commitAll()
        redraw()
    }

    private fun ProtectedAccess.redraw() {
        val redraw = "varp.omnishop_redraw"
        VarPlayerIntMapSetter.set(player, redraw, player.vars[redraw] xor 1)
    }

    internal data class InfoArgs(val shop: Int, val index: Int) : IfScriptArgs

    internal data class BuyArgs(val shop: Int, val index: Int, val option: Int) : IfScriptArgs

    internal data class ExamineArgs(val obj: Int) : IfScriptArgs

    private companion object {
        const val INFO_UPDATE = "clientscript.omnishop_info_update"
    }
}
