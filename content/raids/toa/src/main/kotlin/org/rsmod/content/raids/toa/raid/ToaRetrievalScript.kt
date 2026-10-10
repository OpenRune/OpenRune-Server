package org.rsmod.content.raids.toa.raid

import dev.openrune.definition.type.widget.IfEvent
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import dev.openrune.types.aconverted.interf.IfButtonOp
import jakarta.inject.Inject
import kotlin.math.min
import org.rsmod.api.market.MarketPrices
import org.rsmod.api.player.output.mes
import org.rsmod.api.player.output.runClientScript
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.stopInvTransmit
import org.rsmod.api.player.ui.IfModalButton
import org.rsmod.api.player.vars.intVarp
import org.rsmod.api.script.onIfClose
import org.rsmod.api.script.onIfModalButton
import org.rsmod.api.script.onOpLoc1
import org.rsmod.content.raids.toa.raid.ToaRetrieval.retrievalChest
import org.rsmod.content.raids.toa.raid.ToaRetrieval.retrievalLocked
import org.rsmod.game.entity.Player
import org.rsmod.game.type.getInvObj
import org.rsmod.objtx.isOk
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class ToaRetrievalScript @Inject constructor(private val prices: MarketPrices) : PluginScript() {
    private var Player.retrievalType by intVarp("varp.if1")

    override fun ScriptContext.startup() {
        onOpLoc1(CHEST) { openChest() }
        onIfClose(INTERFACE) { player.stopInvTransmit(player.retrievalChest) }
        onIfModalButton(BUTTON) { unlockOrTakeAll() }
        onIfModalButton(DISCARD) { discardAll() }
        onIfModalButton(ITEMS) { itemOp(it) }
    }

    private suspend fun ProtectedAccess.openChest() {
        val chest = player.retrievalChest
        if (chest.isEmpty()) {
            mesbox("There is nothing to collect.")
            return
        }

        val fee = if (player.retrievalLocked == 1) ToaRetrieval.fee(player, prices) else 0
        if (fee == 0) player.retrievalLocked = 0

        player.runClientScript(SCRIPT_TRANSMIT_DATA.asRSCM(RSCMType.CLIENTSCRIPT), fee, COFFER)
        player.retrievalType = if (fee > 0) TYPE_LOCKED else TYPE_UNLOCKED
        invTransmit(chest)
        ifOpenMainModal(INTERFACE)
        ifSetEvents(ITEMS, chest.indices, IfEvent.Op2, IfEvent.Op9, IfEvent.Op10)
    }

    private fun ProtectedAccess.unlockOrTakeAll() {
        if (player.retrievalLocked == 1) unlock() else takeAll()
    }

    private fun ProtectedAccess.unlock() {
        val fee = ToaRetrieval.fee(player, prices)
        val carried = invTotal(inv, COINS)
        val banked = invTotal(bank, COINS)
        if (fee > carried.toLong() + banked) {
            mes("You need at least ${"%,d".format(fee)} coins to unlock your retrieval service.")
            return
        }
        val fromInv = min(fee, carried)
        if (fromInv > 0) invDel(inv, COINS, fromInv)
        if (fee > fromInv) invDel(bank, COINS, fee - fromInv)
        player.retrievalLocked = 0
        player.runClientScript(SCRIPT_TRANSMIT_DATA.asRSCM(RSCMType.CLIENTSCRIPT), 0, COFFER)
        player.retrievalType = TYPE_UNLOCKED
        mes("You've unlocked the contents of the retrieval service.")
    }

    private fun ProtectedAccess.takeAll() {
        val chest = player.retrievalChest
        for (slot in chest.indices) {
            val obj = chest[slot] ?: continue
            invMoveFromSlot(
                from = chest,
                into = inv,
                fromSlot = slot,
                count = obj.count,
                strict = false,
            )
        }
        invCompress(chest)
        if (chest.isNotEmpty()) {
            mes("Not enough space in your inventory to reclaim all of the items.")
            return
        }
        ifClose()
        mes("You reclaim all of your items from the retrieval service.")
    }

    private suspend fun ProtectedAccess.discardAll() {
        ifClose()
        val discard =
            choice2(
                "Yes, discard them all.",
                true,
                "No, keep them.",
                false,
                title = "Are you sure you wish to discard all the items?",
            )
        if (!discard) {
            openChest()
            return
        }
        invClear(player.retrievalChest)
        player.retrievalLocked = 0
        mes("All the contents of the retrieval service have been discarded.")
    }

    private fun ProtectedAccess.itemOp(event: IfModalButton) {
        val chest = player.retrievalChest
        val obj = chest[event.comsub] ?: return
        when (event.op) {
            IfButtonOp.Op2 -> {
                if (player.retrievalLocked == 1) {
                    mes("You need to unlock your retrieval service first.")
                    return
                }
                for (slot in chest.indices) {
                    val other = chest[slot] ?: continue
                    if (other.id != obj.id) continue
                    val moved =
                        invMoveFromSlot(
                            from = chest,
                            into = inv,
                            fromSlot = slot,
                            count = other.count,
                            strict = false,
                        )
                    if (!moved[0].isOk()) break
                }
                invCompress(chest)
                if (chest.isEmpty()) {
                    ifClose()
                    mes("You reclaim all of your items from the retrieval service.")
                }
            }
            IfButtonOp.Op9 -> {
                val type = getInvObj(obj)
                if (!type.tradeable) {
                    mes("This item is untradeable.")
                    return
                }
                val value = (prices[type] ?: type.cost).toLong() * obj.count
                mes("The value of ${type.name} x ${obj.count} is ${"%,d".format(value)} coins.")
            }
            IfButtonOp.Op10 -> objExamine(chest, event.comsub)
            else -> Unit
        }
    }

    private companion object {
        const val CHEST = "loc.toa_lobby_gravestone_chest"
        const val INTERFACE = "interface.gravestone_retrieval"
        const val ITEMS = "component.gravestone_retrieval:items"
        const val BUTTON = "component.gravestone_retrieval:button"
        const val DISCARD = "component.gravestone_retrieval:discard"
        const val COINS = "obj.coins"
        const val TYPE_LOCKED = 39
        const val TYPE_UNLOCKED = 38
        const val SCRIPT_TRANSMIT_DATA = "clientscript.[clientscript,gravestone_transmit_data]"
        const val COFFER = 0
    }
}
