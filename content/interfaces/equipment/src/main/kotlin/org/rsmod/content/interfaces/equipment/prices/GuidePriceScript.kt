package org.rsmod.content.interfaces.equipment.prices

import dev.openrune.ServerCacheManager
import dev.openrune.definition.type.widget.IfEvent
import dev.openrune.rscm.RSCM
import dev.openrune.rscm.RSCMType
import dev.openrune.types.ItemServerType
import dev.openrune.types.aconverted.interf.IfButtonOp
import jakarta.inject.Inject
import org.rsmod.api.invtx.invCompress
import org.rsmod.api.invtx.invMoveAll
import org.rsmod.api.market.MarketPrices
import org.rsmod.api.player.output.ClientScripts.ifSetTextAlign
import org.rsmod.api.player.output.objExamine
import org.rsmod.api.player.output.runClientScript
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.protect.ProtectedAccessLauncher
import org.rsmod.api.player.ui.ifClose
import org.rsmod.api.player.ui.ifSetText
import org.rsmod.api.script.onIfClose
import org.rsmod.api.script.onIfModalButton
import org.rsmod.api.script.onIfOverlayButton
import org.rsmod.api.utils.format.formatAmount
import org.rsmod.events.EventBus
import org.rsmod.game.entity.Player
import org.rsmod.game.inv.InvObj
import org.rsmod.game.type.getInvObj
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class GuidePriceScript
@Inject
constructor(
    private val eventBus: EventBus,
    private val protectedAccess: ProtectedAccessLauncher,
    private val marketPrices: MarketPrices,
) : PluginScript() {
    private val ItemServerType.price: Int
        get() = marketPrices[this] ?: 1

    override fun ScriptContext.startup() {
        onIfOverlayButton("component.wornitems:pricechecker") { player.selectGuidePrices() }
        onIfModalButton("component.ge_pricechecker:all") { addAllFromInv() }
        onIfModalButton("component.ge_pricechecker_side:items") { addFromSlot(it.comsub, it.op) }
        onIfModalButton("component.ge_pricechecker:items") { takeFromSlot(it.comsub, it.op) }
        onIfModalButton("component.ge_pricechecker:other") { searchObj() }
        onIfClose("interface.ge_pricechecker") { player.closeGuide() }
    }

    private fun Player.selectGuidePrices() {
        ifClose(eventBus)
        protectedAccess.launch(this) { openGuide() }
    }

    private fun ProtectedAccess.openGuide() {
        invClear(tempInv)
        invTransmit(tempInv)
        invTransmit(inv)
        ifOpenMainSidePair(
            main = "interface.ge_pricechecker",
            side = "interface.ge_pricechecker_side",
        )
        player.updateGuidePrices()
        ifSetObj("component.ge_pricechecker:otheritem", "obj.blankobject", zoom = 1)
        ifSetEvents(
            target = "component.ge_pricechecker:items",
            range = tempInv.indices,
            IfEvent.Op1,
            IfEvent.Op2,
            IfEvent.Op3,
            IfEvent.Op4,
            IfEvent.Op5,
            IfEvent.Op10,
        )
        interfaceInvInit(
            inv = inv,
            target = "component.ge_pricechecker_side:items",
            objRowCount = 4,
            objColCount = 7,
            op1 = "Add<col=ff9040>",
            op2 = "Add-5<col=ff9040>",
            op3 = "Add-10<col=ff9040>",
            op4 = "Add-All<col=ff9040>",
            op5 = "Add-X<col=ff9040>",
        )
        ifSetEvents(
            target = "component.ge_pricechecker_side:items",
            range = inv.indices,
            IfEvent.Op1,
            IfEvent.Op2,
            IfEvent.Op3,
            IfEvent.Op4,
            IfEvent.Op5,
            IfEvent.Op10,
        )
    }

    private fun Player.updateGuidePrices() {
        val tempInv =
            checkNotNull(invMap["inv.tradeoffer"]) {
                "`tempInv` must be transmitted. (`startInvTransmit`)"
            }
        val priceList = tempInv.toPriceList()
        updatePrices(priceList.prices)
        updateTotalPrice(priceList.totalPrice)
    }

    private fun Player.updateTotalPrice(totalPrice: Long) {
        val total = totalPrice.formatAmount
        ifSetTextAlign(
            player = this,
            target = "component.ge_pricechecker:output",
            alignH = 1,
            alignV = 1,
            lineHeight = 15,
        )
        ifSetText(
            internal = "component.ge_pricechecker:output",
            text = "Total guide price:<br><col=ffffff>$total</col>",
        )
    }

    private fun Player.updateSearchPrice(type: ItemServerType) {
        ifSetTextAlign(
            player = this,
            target = "component.ge_pricechecker:output",
            alignH = 1,
            alignV = 1,
            lineHeight = 15,
        )
        ifSetText(
            internal = "component.ge_pricechecker:output",
            text = "${type.name}:<br><col=ffffff>${type.price.formatAmount} coins</col>",
        )
    }

    private fun Player.updatePrices(priceList: List<Int>) {
        check(priceList.size == 28) { "ClientScript takes 28 exact prices." }
        runClientScript(785, *priceList.toTypedArray())
    }

    private fun Iterable<InvObj?>.toPriceList(): PriceList {
        var total = 0L
        val prices = mutableListOf<Int>()
        for (obj in this) {
            if (obj == null) {
                prices += 0
                continue
            }
            val type = getInvObj(obj)
            val price = type.price
            total += price * obj.count.toLong()
            prices += price
        }
        return PriceList(prices, total)
    }

    private suspend fun ProtectedAccess.searchObj() {
        val search = objDialog("Select an item to ask about its price:")

        val searchInternalName = RSCM.getReverseMapping(RSCMType.OBJ,search.id)

        ifSetObj("component.ge_pricechecker:otheritem", searchInternalName, zoom = 1)
        player.updateSearchPrice(search)
    }

    private fun ProtectedAccess.addAllFromInv() {
        if (inv.isEmpty()) {
            mes("You have no items that can be checked.")
            soundSynth("synth.pillory_wrong")
            return
        }
        val untradableSlots = inv.mapSlots { slot, obj -> obj != null && !ocTradable(obj) }
        val transaction = invMoveInv(inv, tempInv, keepSlots = untradableSlots)

        if (transaction.noneCompleted()) {
            mes("You have items that cannot be traded.")
            soundSynth("synth.pillory_wrong")
            return
        }

        player.updateGuidePrices()
    }

    private suspend fun ProtectedAccess.addFromSlot(fromSlot: Int, op: IfButtonOp) {
        val obj = inv[fromSlot] ?: return
        if (op == IfButtonOp.Op10) {
            player.examine(obj)
            return
        }

        if (!ocTradable(obj)) {
            mes("You cannot trade that item.")
            return
        }

        val count = resolveCount(op) ?: error("Invalid op: $op (slot=$fromSlot)")
        invMoveFromSlot(from = inv, into = tempInv, fromSlot = fromSlot, count = count)
        player.updateGuidePrices()
    }

    private suspend fun ProtectedAccess.takeFromSlot(fromSlot: Int, op: IfButtonOp) {
        if (op == IfButtonOp.Op10) {
            val obj = tempInv[fromSlot] ?: return
            player.examine(obj)
            return
        }
        val count = resolveCount(op) ?: error("Invalid op: $op (slot=$fromSlot)")
        invMoveFromSlot(from = tempInv, into = inv, fromSlot = fromSlot, count = count)
        player.invCompress(tempInv)
        player.updateGuidePrices()
    }

    private suspend fun ProtectedAccess.resolveCount(op: IfButtonOp): Int? =
        when (op) {
            IfButtonOp.Op1 -> 1
            IfButtonOp.Op2 -> 5
            IfButtonOp.Op3 -> 10
            IfButtonOp.Op4 -> Int.MAX_VALUE
            IfButtonOp.Op5 -> countDialog()
            else -> null
        }

    private fun Player.closeGuide() {
        val tempInv = invMap["inv.tradeoffer"] ?: return
        val result = invMoveAll(from = tempInv, into = inv)
        check(result.success) { "Could not move `tempInv` into `inv`: $tempInv" }
    }

    private fun Player.examine(obj: InvObj) {
        val type = getInvObj(obj)
        objExamine(type, obj.count, type.price)
    }

    private data class PriceList(val prices: List<Int>, val totalPrice: Long)
}
