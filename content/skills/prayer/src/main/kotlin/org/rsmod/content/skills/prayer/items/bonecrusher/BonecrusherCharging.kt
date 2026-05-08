package org.rsmod.content.skills.prayer.items.bonecrusher

import org.rsmod.api.invtx.invAdd
import org.rsmod.api.invtx.invDel
import org.rsmod.api.player.vars.intVarBit
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
    crusherSlot: Int,
    tokenSlot: Int,
): Int? {
    val tokenCount = inv.count("obj.ectotoken")
    val removed = invDel(inv, "obj.ectotoken", count = tokenCount, slot = tokenSlot)
    if (removed.failure) {
        return null
    }

    val add = tokenCount * 25
    val next = (bonecrusherCharges + add).coerceAtMost(60_000)
    if (next == bonecrusherCharges) {
        invAdd(inv, "obj.ectotoken", count = tokenCount)
        return null
    }
    bonecrusherCharges = next

    return bonecrusherCharges
}

internal fun Player.tryUnchargeBonecrusher(
    inv: Inventory,
    slot: Int,
    crusherInternal: String,
): BonecrusherUnchargeResult {
    inv[slot]?.takeIf { it.isType(crusherInternal) } ?: return BonecrusherUnchargeResult.WrongItem

    val storedCharges = bonecrusherCharges
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

    bonecrusherCharges = (storedCharges - chargesToRemove).coerceAtLeast(0)

    val remaining = storedCharges - chargesToRemove
    return BonecrusherUnchargeResult.Success(redeemableTokens, chargesToRemove, remaining)
}

private var Player.bonecrusherCharges by intVarBit("varbit.charges_bonecrusher_quantity")
