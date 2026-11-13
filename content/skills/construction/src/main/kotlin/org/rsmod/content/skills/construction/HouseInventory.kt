package org.rsmod.content.skills.construction

import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import org.rsmod.api.invtx.add
import org.rsmod.api.invtx.delete
import org.rsmod.api.invtx.invTransaction
import org.rsmod.api.invtx.select
import org.rsmod.game.entity.Player
import org.rsmod.game.inv.Inventory

/** How many of [material] [inv] can pay for, counting every obj that stands in for it. */
fun materialCount(inv: Inventory, material: String): Int = materialOptions(material).sumOf(inv::count)

/**
 * The objs that pay for [costs] out of [inv], or null when it falls short of any of them. A cost
 * that any tier of nails can pay draws on the cheapest tiers first, and an obj two costs share is
 * only counted once.
 */
fun spendPlan(inv: Inventory, costs: List<Pair<String, Int>>): Map<String, Int>? {
    val plan = LinkedHashMap<String, Int>()
    for ((material, count) in costs) {
        var remaining = count
        for (option in materialOptions(material)) {
            if (remaining <= 0) {
                break
            }
            val spare = inv.count(option) - (plan[option] ?: 0)
            val take = minOf(spare, remaining)
            if (take > 0) {
                plan.merge(option, take, Int::plus)
                remaining -= take
            }
        }
        if (remaining > 0) {
            return null
        }
    }
    return plan
}

/** Takes every one of [costs] from [inv] in one transaction, or nothing at all. */
fun Player.spend(costs: List<Pair<String, Int>>, inv: Inventory = this.inv): Boolean {
    val plan = spendPlan(inv, costs) ?: return false
    if (plan.isEmpty()) {
        return true
    }
    val result =
        invTransaction(inv) {
            val from = select(inv)
            for ((obj, count) in plan) {
                delete(from, obj.asRSCM(RSCMType.OBJ), count)
            }
        }
    return result.success
}

/** Gives one [obj] from a dispenser, refusing rather than dropping it when there is no room. */
fun Player.dispense(obj: String, inv: Inventory = this.inv): Boolean {
    if (inv.isFull()) {
        return false
    }
    val result =
        invTransaction(inv) {
            add(select(inv), obj.asRSCM(RSCMType.OBJ), 1)
        }
    return result.success
}

/** Swaps one [from] for one [into], the way a sink or barrel fills what is used on it. */
fun Player.fill(from: String, into: String, inv: Inventory = this.inv): Boolean {
    val result =
        invTransaction(inv) {
            val target = select(inv)
            delete(target, from.asRSCM(RSCMType.OBJ), 1)
            add(target, into.asRSCM(RSCMType.OBJ), 1)
        }
    return result.success
}
