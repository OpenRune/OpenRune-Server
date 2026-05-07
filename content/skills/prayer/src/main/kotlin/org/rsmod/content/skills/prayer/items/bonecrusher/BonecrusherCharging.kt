package org.rsmod.content.skills.prayer.items.bonecrusher

import org.rsmod.api.invtx.invAdd
import org.rsmod.api.invtx.invDel
import org.rsmod.api.obj.charges.ObjChargeManager
import org.rsmod.api.obj.charges.ObjChargeManager.Companion.isFailure
import org.rsmod.game.entity.Player
import org.rsmod.game.inv.Inventory
import org.rsmod.game.inv.isType

internal sealed class BonecrusherUnchargeResult {
    data object WrongItem : BonecrusherUnchargeResult()
    data object NoCharges : BonecrusherUnchargeResult()
    data object CannotRedeemEcto : BonecrusherUnchargeResult()
    data object NoInvSpace : BonecrusherUnchargeResult()
    data class Success(
        val ectoTokens: Int,
        val chargesRemoved: Int,
        val remainingCharges: Int,
    ) : BonecrusherUnchargeResult()
}

internal fun Player.chargeCrusherItemWithEcto(
    chargeManager: ObjChargeManager,
    crusherSlot: Int,
    tokenSlot: Int,
): Int? {
    val tokenCount = inv.count("obj.ectotoken")
    val removed = invDel(inv, "obj.ectotoken", count = tokenCount, slot = tokenSlot)
    if (removed.failure) {
        return null
    }

    val added = chargeManager.addChargesSameItem(
        inventory = inv,
        slot = crusherSlot,
        add = tokenCount * 25,
        internal = "varbit.charges_bonecrusher_quantity",
        max = 60000
    )

    if (added.isFailure()) {
        invAdd(inv, "obj.ectotoken", count = tokenCount)
        return null
    }

    return chargeManager.getCharges(inv[crusherSlot], "varbit.charges_bonecrusher_quantity")
}

internal fun Player.tryUnchargeBonecrusher(
    chargeManager: ObjChargeManager,
    inv: Inventory,
    slot: Int,
    crusherInternal: String,
): BonecrusherUnchargeResult {
    val crusher = inv[slot]?.takeIf { it.isType(crusherInternal) } ?: return BonecrusherUnchargeResult.WrongItem

    val storedCharges = chargeManager.getCharges(crusher, "varbit.charges_bonecrusher_quantity")
    if (storedCharges == 0) {
        return BonecrusherUnchargeResult.NoCharges
    }
    if (storedCharges < 25) {
        return BonecrusherUnchargeResult.CannotRedeemEcto
    }

    val redeemableTokens = storedCharges / 25
    val chargesToRemove = redeemableTokens * 25

    val added = invAdd(inv, "obj.ectotoken", count = redeemableTokens)
    if (added.failure) {
        return BonecrusherUnchargeResult.NoInvSpace
    }

    val reduced = chargeManager.reduceChargesSameItem(
        inventory = inv,
        slot = slot,
        remove = chargesToRemove,
        internal = "varbit.charges_bonecrusher_quantity"
    )

    if (reduced.isFailure()) {
        invDel(inv, "obj.ectotoken", count = redeemableTokens, strict = false)
        return BonecrusherUnchargeResult.WrongItem
    }

    val remaining = storedCharges - chargesToRemove
    return BonecrusherUnchargeResult.Success(redeemableTokens, chargesToRemove, remaining)
}
