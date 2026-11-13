package org.rsmod.content.skills.thieving

import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import dtx.core.ArgMap
import dtx.core.RollResult
import dtx.core.flatten
import dtx.rs.RSDropTable
import org.rsmod.api.droptable.DropRollItem
import org.rsmod.api.invtx.invTransaction
import org.rsmod.api.invtx.select
import org.rsmod.game.entity.Player
import org.rsmod.game.inv.Inventory

private const val COINS = "obj.coins"

internal typealias ThievingDropTable = RSDropTable<Player, DropRollItem>

internal data class LootDrop(val obj: String, val count: Int)

internal fun Player.rollDrops(table: ThievingDropTable): List<Pair<DropRollItem, String>> {
    val drops =
        when (val result = table.roll(this, ArgMap()).flatten()) {
            is RollResult.Nothing -> emptyList()
            is RollResult.Single -> listOf(result.result)
            is RollResult.ListOf -> result.results
        }
    return drops
        .flatMap { it.withBonusDrops() }
        .filter { !it.isNothing && it.condition(this) }
        .map { it to (it.transformObj(this) ?: it.obj) }
}

private fun DropRollItem.withBonusDrops(): List<DropRollItem> =
    listOf(this) + bonusDrops.flatMap { it.withBonusDrops() }

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
