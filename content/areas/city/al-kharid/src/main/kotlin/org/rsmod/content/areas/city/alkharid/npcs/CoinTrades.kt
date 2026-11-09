package org.rsmod.content.areas.city.alkharid.npcs

import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import org.rsmod.api.invtx.add
import org.rsmod.api.invtx.delete
import org.rsmod.api.invtx.invTransaction
import org.rsmod.api.invtx.select
import org.rsmod.api.player.protect.ProtectedAccess

private const val COINS = "obj.coins"

/** Pays [price] coins for one [obj], or changes nothing if either side doesn't fit. */
public fun ProtectedAccess.buyForCoins(obj: String, price: Int): Boolean =
    tradeForCoins(price, take = null, give = obj)

/** Pays [price] coins to swap one [take] for one [give], or changes nothing. */
public fun ProtectedAccess.swapForCoins(take: String, give: String, price: Int): Boolean =
    tradeForCoins(price, take, give)

private fun ProtectedAccess.tradeForCoins(price: Int, take: String?, give: String): Boolean =
    player
        .invTransaction(inv) {
            val target = select(inv)
            delete(target, COINS.asRSCM(RSCMType.OBJ), price)
            if (take != null) {
                delete(target, take.asRSCM(RSCMType.OBJ), count = 1)
            }
            add(target, give.asRSCM(RSCMType.OBJ), count = 1)
        }
        .success
