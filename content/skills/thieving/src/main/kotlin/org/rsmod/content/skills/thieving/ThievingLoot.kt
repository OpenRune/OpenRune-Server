package org.rsmod.content.skills.thieving

import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import org.rsmod.api.invtx.invTransaction
import org.rsmod.api.invtx.select
import org.rsmod.game.entity.Player
import org.rsmod.game.inv.Inventory

private const val COINS = "obj.coins"

internal data class LootDrop(val obj: String, val count: Int)

internal fun LootDrop.doubled(pouch: String?, pouchCoins: () -> Int): List<LootDrop> =
    if (obj == pouch) {
        listOf(this, LootDrop(COINS, (1..count).sumOf { pouchCoins() }))
    } else {
        listOf(copy(count = count * 2))
    }

internal fun Player.addLoot(inv: Inventory, loot: List<LootDrop>, commit: Boolean = true): Boolean =
    invTransaction(inv, autoCommit = commit) {
            val into = select(inv)
            for (drop in loot) {
                insert {
                    this.into = into
                    obj = drop.obj.asRSCM(RSCMType.OBJ)
                    strictCount = drop.count
                }
            }
        }
        .success

internal fun Player.exchangePouches(
    inv: Inventory,
    pouch: String,
    count: Int,
    coins: Long,
): Boolean {
    if (coins > Int.MAX_VALUE) return false
    return invTransaction(inv) {
            val from = select(inv)
            delete {
                this.from = from
                obj = pouch.asRSCM(RSCMType.OBJ)
                strictCount = count
            }
            insert {
                into = from
                obj = COINS.asRSCM(RSCMType.OBJ)
                strictCount = coins.toInt()
            }
        }
        .success
}
