package org.rsmod.content.areas.city.varrock.npcs

import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import org.rsmod.api.invtx.invTransaction
import org.rsmod.api.invtx.select
import org.rsmod.game.entity.Player

internal object BaraekFurTrade {
    const val BEAR_FUR = "obj.fur"
    private const val COINS = "obj.coins"

    /**
     * Sells one bear fur for [price] coins. With no room for the fur it is dropped at the player's
     * feet through [dropFur], as Baraek does in OSRS.
     */
    fun buy(player: Player, price: Int, dropFur: (Player) -> Unit = {}): Boolean {
        if (player.inv.count(COINS) < price) {
            return false
        }
        if (exchange(player, take = COINS to price, give = BEAR_FUR to 1)) {
            return true
        }
        if (!exchange(player, take = COINS to price, give = null)) {
            return false
        }
        dropFur(player)
        return true
    }

    /** Sells furs one at a time until none of [furs] are left or the coins no longer fit. */
    fun sellAll(player: Player, furs: List<String>, price: Int): Int {
        var sold = 0
        for (fur in furs) {
            while (fur in player.inv && exchange(player, take = fur to 1, give = COINS to price)) {
                sold++
            }
        }
        return sold
    }

    private fun exchange(player: Player, take: Pair<String, Int>, give: Pair<String, Int>?): Boolean {
        val result =
            player.invTransaction(player.inv) {
                val inv = select(player.inv)
                delete {
                    from = inv
                    obj = take.first.asRSCM(RSCMType.OBJ)
                    strictCount = take.second
                }
                if (give != null) {
                    insert {
                        into = inv
                        obj = give.first.asRSCM(RSCMType.OBJ)
                        strictCount = give.second
                    }
                }
            }
        return result.success
    }
}
