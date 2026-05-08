package org.rsmod.content.skills.prayer.items.ashsanctifier

import org.rsmod.api.invtx.invAdd
import org.rsmod.api.invtx.invDel
import org.rsmod.api.player.vars.intVarBit
import org.rsmod.game.entity.Player
import org.rsmod.game.inv.Inventory
import org.rsmod.game.inv.isType
import kotlin.math.min


internal sealed class AshSanctifierUnchargeResult {
    data object WrongItem : AshSanctifierUnchargeResult()

    data object NoCharges : AshSanctifierUnchargeResult()

    data object CannotRedeemDeathRunes : AshSanctifierUnchargeResult()

    data object NoInvSpace : AshSanctifierUnchargeResult()

    data class Success(
        val deathRunes: Int,
        val chargesRemoved: Int,
        val remainingCharges: Int,
    ) : AshSanctifierUnchargeResult()
}

internal fun Player.chargeAshSanctifierWithDeathRunes(
    inv: Inventory,
    sanctifierSlot: Int,
    runeSlot: Int,
): Int? {
    val sanctifier = inv[sanctifierSlot]?.takeIf { it.isType("obj.ash_sanctifier") } ?: return null
    val runeStack = inv[runeSlot]?.takeIf { it.isType("obj.deathrune") } ?: return null

    val runeCount = runeStack.count
    if (runeCount <= 0) {
        return null
    }

    val curr = ashSanctifierCharges
    val space = (Int.MAX_VALUE - curr).coerceAtLeast(0)
    val maxRunesBySpace = space / 10
    val toConsume = min(runeCount, maxRunesBySpace)
    if (toConsume <= 0) {
        return null
    }

    val removed = invDel(inv, "obj.deathrune", count = toConsume, slot = runeSlot)
    if (removed.failure) {
        return null
    }

    val addCharges = toConsume * 10
    val next = (curr + addCharges).coerceAtMost(Int.MAX_VALUE)
    if (next == curr) {
        invAdd(inv, "obj.deathrune", count = toConsume)
        return null
    }
    ashSanctifierCharges = next

    return ashSanctifierCharges
}

internal fun Player.tryUnchargeAshSanctifier(
    inv: Inventory,
    slot: Int,
): AshSanctifierUnchargeResult {
    val sanctifier = inv[slot]?.takeIf { it.isType("obj.ash_sanctifier") } ?: return AshSanctifierUnchargeResult.WrongItem

    val storedCharges = ashSanctifierCharges
    if (storedCharges == 0) {
        return AshSanctifierUnchargeResult.NoCharges
    }
    if (storedCharges < 10) {
        return AshSanctifierUnchargeResult.CannotRedeemDeathRunes
    }

    val redeemableRunes = storedCharges / 10
    val chargesToRemove = redeemableRunes * 10

    val added = invAdd(inv, "obj.deathrune", count = redeemableRunes)
    if (added.failure) {
        return AshSanctifierUnchargeResult.NoInvSpace
    }

    ashSanctifierCharges = (storedCharges - chargesToRemove).coerceAtLeast(0)

    val remaining = storedCharges - chargesToRemove
    return AshSanctifierUnchargeResult.Success(redeemableRunes, chargesToRemove, remaining)
}

private var Player.ashSanctifierCharges by intVarBit("varbit.charges_ash_sanctifier_quantity")
