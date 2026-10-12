package org.rsmod.content.other.gravestones

import org.rsmod.api.invtx.invDel
import org.rsmod.api.player.protect.ProtectedAccess

/** Where a death fee (a grave's or Death's Office's) was paid from. */
enum class DeathFeeSource {
    Inventory,
    Bank,
}

/**
 * Takes a death fee of [fee] coins from the coins carried, else from the bank, and returns where
 * it came from, or null when neither covers it. Death: "Cash is always acceptable. Also, a
 * gravestone can take it directly from your bank, in case you didn't have enough with you." (live
 * capture, rev 240, 20260811T142735-59bcb26@2052).
 */
fun ProtectedAccess.payDeathFee(fee: Int): DeathFeeSource? {
    // TODO: Death's Coffer pays first (https://oldschool.runescape.wiki/w/Death/Item_Recovery_Fees).
    // UNVERIFIED: whether carried coins and the bank are combined when neither covers the fee
    // alone (Death's wording suggests one or the other).
    val (from, source) =
        when {
            inv.count(COINS) >= fee -> inv to DeathFeeSource.Inventory
            bank.count(COINS) >= fee -> bank to DeathFeeSource.Bank
            else -> return null
        }
    return if (player.invDel(from, COINS, fee).success) source else null
}

private const val COINS = "obj.coins"
