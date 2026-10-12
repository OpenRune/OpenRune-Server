package org.rsmod.content.other.gravestones

import org.rsmod.api.invtx.invDel
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.vars.intVarp
import org.rsmod.game.entity.Player

/**
 * Death's Coffer: coins credited for items sacrificed to Death, spent only on death fees
 * (https://oldschool.runescape.wiki/w/Death%27s_Coffer).
 */
var Player.deathCoffer by intVarp("varp.death_coffer")

/** Where a death fee (a grave's or Death's Office's) was paid from. */
enum class DeathFeeSource {
    Coffer,
    Inventory,
    Bank,
}

/**
 * Takes a death fee of [fee] coins and returns where it came from, or null when nothing covers it.
 * "The fee is deducted from Death's Coffer, if available, or else from the bank"
 * (https://oldschool.runescape.wiki/w/Death/Item_Recovery_Fees), and Death: "Cash is always
 * acceptable. Also, a gravestone can take it directly from your bank, in case you didn't have
 * enough with you." (live capture, rev 240, 20260811T142735-59bcb26@2052).
 */
fun ProtectedAccess.payDeathFee(fee: Int): DeathFeeSource? {
    // UNVERIFIED: whether the coffer, carried coins and the bank are combined when none covers the
    // fee alone (the wiki's wording suggests one source).
    if (player.deathCoffer >= fee) {
        player.deathCoffer -= fee
        return DeathFeeSource.Coffer
    }
    val (from, source) =
        when {
            inv.count(COINS) >= fee -> inv to DeathFeeSource.Inventory
            bank.count(COINS) >= fee -> bank to DeathFeeSource.Bank
            else -> return null
        }
    return if (player.invDel(from, COINS, fee).success) source else null
}

/**
 * The message for a fee taken from [source]. The bank one is from live capture, rev 241,
 * 20261012T233700-df49f48@1037.
 */
fun deathFeeMessage(source: DeathFeeSource, fee: Int): String {
    val amount = "%,d".format(fee)
    return when (source) {
        DeathFeeSource.Bank -> "Payment has been taken from your bank: $amount x Coins"
        // UNVERIFIED: the coffer and inventory messages (the bank one, reworded).
        DeathFeeSource.Inventory -> "Payment has been taken from your inventory: $amount x Coins"
        DeathFeeSource.Coffer -> "Payment has been taken from Death's Coffer: $amount x Coins"
    }
}

private const val COINS = "obj.coins"
