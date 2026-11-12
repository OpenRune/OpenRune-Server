package org.rsmod.content.travel.teleports

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.game.inv.Inventory

/**
 * Charge handling for teleport items. Charge chains swap the obj itself; varbit items keep the
 * count on the player, so every book a player owns draws on the same pool, as in OSRS.
 */
object TeleportCharges {
    fun ProtectedAccess.hasCharge(item: TeleportItem): Boolean {
        val varbit = item.chargeVarbit ?: return true
        if (vars[varbit] > 0) {
            return true
        }
        mes("Your ${item.label} has no charges left.")
        return false
    }

    /**
     * Spends one charge of the item in [slot]: steps a charge chain down one obj (or crumbles it),
     * or takes one from the item's charge varbit. Returns `false`, spending nothing, when the item
     * is no longer in [slot] or its charges ran out meanwhile.
     */
    fun ProtectedAccess.spendCharge(item: TeleportItem, inv: Inventory, slot: Int): Boolean {
        val obj = inv[slot] ?: return false
        val name = RSCM.getReverseMapping(RSCMType.OBJ, obj.id)
        if (name !in item.objs) {
            return false
        }
        val varbit = item.chargeVarbit
        if (varbit != null) {
            val left = vars[varbit] - 1
            if (left < 0) {
                return false
            }
            vars[varbit] = left
            mes(chargesMessage(item, left))
            return true
        }
        if (!item.isChain) {
            return true
        }
        val next = item.afterCharge(name)
        val replacement = next?.let { ServerCacheManager.getItem(it.asRSCM(RSCMType.OBJ)) }
        val success =
            when {
                next == null -> invDel(inv, name, count = 1, slot = slot).success
                replacement == null -> false
                else -> invReplaceSlot(inv, slot, count = 1, replacement = replacement).success
            }
        if (!success) {
            return false
        }
        if (inv === worn) {
            rebuildAppearance()
        }
        if (next == null) {
            mes("Your ${item.label} crumbles to dust.")
        } else {
            mes(chargesMessage(item, item.chargesAfter(name) ?: 0))
        }
        return true
    }

    fun ProtectedAccess.checkCharges(item: TeleportItem) {
        val varbit = item.chargeVarbit ?: return
        mes(chargesMessage(item, vars[varbit]))
    }

    private fun chargesMessage(item: TeleportItem, left: Int): String =
        when (left) {
            0 -> "Your ${item.label} has run out of charges."
            1 -> "Your ${item.label} has one charge left."
            else -> "Your ${item.label} has $left charges left."
        }
}
