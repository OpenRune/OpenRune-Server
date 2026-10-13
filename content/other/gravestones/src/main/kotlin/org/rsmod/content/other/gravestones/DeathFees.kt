package org.rsmod.content.other.gravestones

import org.rsmod.api.invtx.invDel
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.vars.intVarp
import org.rsmod.game.entity.Player

/** Death's Coffer: coins credited for items sacrificed to Death, spent only on death fees. */
var Player.deathCoffer by intVarp("varp.death_coffer")

enum class DeathFeeSource {
    Coffer,
    Inventory,
    Bank,
}

/**
 * Takes a death fee of [fee] coins and returns where it came from, or null when nothing covers it.
 * "The fee is deducted from Death's Coffer, if available, or else from the bank", and Death: "Cash
 * is always acceptable. Also, a gravestone can take it directly from your bank, in case you didn't
 * have enough with you."
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
