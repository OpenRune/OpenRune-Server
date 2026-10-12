package org.rsmod.content.other.gravestones

import dev.openrune.definition.type.widget.IfEvent
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import dev.openrune.types.aconverted.interf.IfButtonOp
import jakarta.inject.Inject
import org.rsmod.api.invtx.invDel
import org.rsmod.api.player.output.runClientScript
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.stopInvTransmit
import org.rsmod.api.player.ui.IfOverlayButton
import org.rsmod.api.player.worn.HeldEquipResult
import org.rsmod.api.script.onIfClose
import org.rsmod.api.script.onIfOverlayButton
import org.rsmod.api.script.onOpNpc1
import org.rsmod.api.script.onOpNpc3
import org.rsmod.api.script.onPlayerLogin
import org.rsmod.api.script.onPlayerLogout
import org.rsmod.api.script.onPlayerSoftTimer
import org.rsmod.content.other.gravestones.Gravestones.Companion.INTERFACE
import org.rsmod.content.other.gravestones.Gravestones.Companion.TIMER
import org.rsmod.content.other.gravestones.Gravestones.Companion.TIMER_INTERVAL
import org.rsmod.game.entity.Npc
import org.rsmod.game.type.getInvObj
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class GravestoneScript @Inject constructor(private val graves: Gravestones) : PluginScript() {
    override fun ScriptContext.startup() {
        // TODO: Live opens a grave from up to 7 tiles away with line of sight
        // (https://oldschool.runescape.wiki/w/Grave); this walks up to it.
        for (style in GRAVE_STYLES) {
            onOpNpc1(style) { if (owns(it.npc)) openGrave() }
            // UNVERIFIED: Loot takes what Take-All in the free section would.
            onOpNpc3(style) { if (owns(it.npc)) take(freeSlots()) }
        }
        onPlayerLogin {
            if (player.graveCoords != null) {
                graves.spawn(player)
                if (player.graveDuration > 0) {
                    player.softTimer(TIMER, TIMER_INTERVAL)
                }
            }
        }
        onPlayerLogout { graves.despawn(player) }
        onPlayerSoftTimer(TIMER) { graves.tick(player) }
        onIfClose(INTERFACE) {
            player.stopInvTransmit(player.grave)
            // live capture, rev 241, 20261012T233700-df49f48@1042.
            player.runClientScript(TRANSMIT_DATA.asRSCM(RSCMType.CLIENTSCRIPT), -1, -1)
        }
        onIfOverlayButton(FREE_ITEMS) { takeOrExamine(it) }
        onIfOverlayButton(PAY_ITEMS) { takeOrExamine(it) }
        onIfOverlayButton(FREE_BUTTON) { take(freeSlots()) }
        onIfOverlayButton(PAY_BUTTON) { if (graves.fee(player) > 0) pay() else take(paySlots()) }
        onIfOverlayButton(CLOSE_BUTTON) { graves.close(player) }
    }

    private fun ProtectedAccess.owns(npc: Npc): Boolean =
        player.graveNpc == npc.uid

    /** Live capture, rev 241, 20261012T233700-df49f48@1030. */
    private fun ProtectedAccess.openGrave() {
        graves.sendFeeThresholds(player)
        invTransmit(player.grave)
        ifOpenOverlay(INTERFACE)
        ifSetEvents(FREE_ITEMS, ITEM_SLOTS, IfEvent.Op1, IfEvent.Op10)
        ifSetEvents(PAY_ITEMS, ITEM_SLOTS, IfEvent.Op1, IfEvent.Op10, IfEvent.Depth2)
        sendFee()
    }

    // TODO: Live pays from Death's Coffer before the bank, and items can be discarded into the
    // coffer to lower the fee (https://oldschool.runescape.wiki/w/Items_Kept_on_Death).
    private fun ProtectedAccess.sendFee() {
        runClientScript(TRANSMIT_DATA.asRSCM(RSCMType.CLIENTSCRIPT), graves.fee(player), 0)
    }

    private fun ProtectedAccess.takeOrExamine(button: IfOverlayButton) {
        when (button.op) {
            IfButtonOp.Op1 -> takeSlot(button.comsub)
            IfButtonOp.Op10 -> player.grave[button.comsub]?.let { mes(getInvObj(it).examine) }
            else -> Unit
        }
    }

    private fun ProtectedAccess.takeSlot(slot: Int) {
        if (slot !in ITEM_SLOTS || player.grave[slot] == null) {
            return
        }
        if (graves.isPaySlot(player, slot) && graves.fee(player) > 0) {
            // UNVERIFIED: the message for taking an item before paying.
            mes("You need to pay the fee before you can take that item.")
            return
        }
        take(listOf(slot))
    }

    private fun ProtectedAccess.freeSlots(): List<Int> =
        ITEM_SLOTS.filter { player.grave[it] != null && !graves.isPaySlot(player, it) }

    private fun ProtectedAccess.paySlots(): List<Int> =
        ITEM_SLOTS.filter { graves.isPaySlot(player, it) }

    /**
     * Takes the fee from the coins carried, else from the bank. Death: "Cash is always acceptable.
     * Also, a gravestone can take it directly from your bank, in case you didn't have enough with
     * you." (live capture, rev 240, 20260811T142735-59bcb26@2052). The bank message is from live
     * capture, rev 241, 20261012T233700-df49f48@1037.
     */
    private fun ProtectedAccess.pay() {
        val fee = graves.fee(player)
        if (fee <= 0) {
            return
        }
        // TODO: Death's Coffer pays first (https://oldschool.runescape.wiki/w/Death/Item_Recovery_Fees).
        // UNVERIFIED: whether carried coins and the bank are combined when neither covers the fee
        // alone (Death's wording suggests one or the other).
        val source =
            when {
                inv.count(COINS) >= fee -> inv
                bank.count(COINS) >= fee -> bank
                else -> {
                    // UNVERIFIED: the message when neither can cover the fee.
                    mes("You don't have enough coins with you or in your bank to pay the fee.")
                    return
                }
            }
        if (player.invDel(source, COINS, fee).failure) {
            return
        }
        player.feePaid = true
        if (source === bank) {
            mes("Payment has been taken from your bank: ${"%,d".format(fee)} x Coins")
        } else {
            // UNVERIFIED: the message when carried coins pay (the bank one, reworded).
            mes("Payment has been taken from your inventory: ${"%,d".format(fee)} x Coins")
        }
        sendFee()
    }

    /**
     * Moves the objs in [slots] out of the grave, equipable ones first ("equipable items are now
     * prioritised", https://oldschool.runescape.wiki/w/Grave, 14 February 2019), each back in the
     * inventory slot it held before the death when that slot is free. With "Auto-equip worn items
     * from gravestone" on, whatever was worn goes back on (live capture, rev 241,
     * 20261012T233700-df49f48@1038-1042, which also re-equipped the kept items), first so the kept
     * items free the inventory slots the grave's objs came from.
     */
    private fun ProtectedAccess.take(slots: List<Int>) {
        val grave = player.grave
        val ordered = slots.sortedByDescending { slot -> grave[slot]?.let { getInvObj(it).isEquipable } }
        if (player.autoEquip) {
            reequipWorn()
        }
        var noSpace = false
        for (slot in ordered) {
            if (!moveToInv(slot)) {
                noSpace = true
            }
        }
        if (player.autoEquip) {
            reequipWorn()
        }
        if (noSpace) {
            // UNVERIFIED: the message when the inventory fills up.
            mes("You don't have enough inventory space.")
        }
        if (grave.isEmpty()) {
            // live capture, rev 241, 20261012T233700-df49f48@1042.
            mes("You successfully retrieved everything from your gravestone.")
            graves.close(player)
            graves.remove(player)
            return
        }
        sendFee()
    }

    private fun ProtectedAccess.moveToInv(slot: Int): Boolean {
        val obj = player.grave[slot] ?: return true
        val type = getInvObj(obj)
        val single = obj.count == 1 || (type.isStackable && obj.id !in player.inv.map { it?.id })
        val intoSlot = if (single) GraveLayout.invSlot(player, obj.id) else null
        val result =
            invMoveFromSlot(
                from = player.grave,
                into = player.inv,
                fromSlot = slot,
                count = obj.count,
                intoSlot = intoSlot,
            )
        if (result.failure) {
            return false
        }
        if (intoSlot != null) {
            GraveLayout.consumeInv(player, intoSlot)
        }
        return true
    }

    private fun ProtectedAccess.reequipWorn() {
        for (wornSlot in 0 until GraveLayout.WORN_SLOT_COUNT) {
            val obj = GraveLayout.wornObj(player, wornSlot) ?: continue
            if (player.worn[wornSlot] != null) {
                continue
            }
            val invSlot = player.inv.indexOfFirst { it?.id == obj }
            if (invSlot == -1) {
                continue
            }
            if (invEquip(invSlot) is HeldEquipResult.Success) {
                GraveLayout.consumeWorn(player, wornSlot)
            }
        }
    }

    private companion object {
        val GRAVE_STYLES = listOf("npc.gravestone_default", "npc.gravestone_angel")
        val ITEM_SLOTS = 0 until 120

        const val FREE_ITEMS = "component.gravestone_generic:freeitems"
        const val PAY_ITEMS = "component.gravestone_generic:payitems"
        const val FREE_BUTTON = "component.gravestone_generic:freebutton"
        const val PAY_BUTTON = "component.gravestone_generic:paybutton"
        const val CLOSE_BUTTON = "component.gravestone_generic:close"
        const val TRANSMIT_DATA = "clientscript.[clientscript,gravestone_transmit_data]"
        const val COINS = "obj.coins"
    }
}
