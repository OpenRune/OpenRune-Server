package org.rsmod.api.invtx

import org.rsmod.game.entity.Player
import org.rsmod.game.inv.InvObj
import org.rsmod.game.inv.Inventory
import org.rsmod.objtx.TransactionResultList

/** [count] of [obj] taken as the cost of a crafted product. */
public data class InvCost(val obj: Int, val count: Int)

/**
 * Takes every [consume] cost out of [inv] and adds [count] of [obj] in one transaction. If any part
 * fails (a missing input, not enough of one, no room for the product) nothing at all changes, so
 * inputs are never lost without the product and the product is never created without its inputs.
 *
 * Items are taken from and placed in [inv] only, never from virtual storage such as a coal bag or
 * essence pouch.
 */
public fun Player.invConsumeAndAdd(
    inv: Inventory,
    consume: List<InvCost>,
    obj: Int,
    count: Int,
): TransactionResultList<InvObj> =
    invTransaction(inv) {
        val target = select(inv)
        for (cost in consume) {
            delete(target, cost.obj, cost.count)
        }
        add(target, obj, count)
    }
