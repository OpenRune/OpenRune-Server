package org.rsmod.api.invtx

import org.rsmod.api.inv.storage.VirtualItemConsumePolicy
import org.rsmod.game.entity.Player
import org.rsmod.game.inv.InvObj
import org.rsmod.game.inv.Inventory
import org.rsmod.objtx.Transaction
import org.rsmod.objtx.TransactionInventory
import org.rsmod.objtx.TransactionResult
import org.rsmod.objtx.TransactionResultList

/**
 * [count] of [obj] taken as the cost of a crafted product. With [internal] set, virtual storage
 * holding the item (a coal bag, essence pouches) can pay some or all of the cost.
 */
public data class InvCost(val obj: Int, val count: Int, val internal: String? = null)

/** [count] of [obj] made alongside a crafted product. */
public data class InvGain(val obj: Int, val count: Int)

/**
 * Takes every [consume] cost from [inv] and adds [count] of [obj] and any [byproducts] to [into] as
 * one action. If any part fails (a missing input, not enough of one, no room for an output) nothing
 * at all changes, so inputs are never lost without the outputs and outputs are never created without
 * their inputs.
 *
 * Virtual storage cannot join an inventory transaction, so its share is taken only after the
 * transaction has succeeded and before it commits. Outputs always go to [into], never to virtual
 * storage.
 */
public fun Player.invConsumeAndAdd(
    inv: Inventory,
    consume: List<InvCost>,
    obj: Int,
    count: Int,
    byproducts: List<InvGain> = emptyList(),
    into: Inventory = inv,
): TransactionResultList<InvObj> =
    consumeWith(inv, consume, into) { output ->
        add(output, obj, count)
        for (byproduct in byproducts) {
            add(output, byproduct.obj, byproduct.count)
        }
    }

/** Takes every [consume] cost with nothing made in return, all or nothing. */
public fun Player.invConsume(
    inv: Inventory,
    consume: List<InvCost>,
): TransactionResultList<InvObj> = consumeWith(inv, consume, inv) {}

private fun Player.consumeWith(
    inv: Inventory,
    consume: List<InvCost>,
    into: Inventory,
    alongside: Transaction<InvObj>.(TransactionInventory<InvObj>) -> Unit,
): TransactionResultList<InvObj> {
    val stored = consume.map { storedShare(inv, it) }
    val separateOutput = into !== inv
    val result =
        invTransaction(inv, if (separateOutput) into else null, autoCommit = false) {
            val target = select(inv)
            val output = if (separateOutput) select(into) else target
            consume.forEachIndexed { index, cost ->
                val fromStorage = stored[index]
                val fromInv = cost.count - fromStorage
                if (fromInv > 0) {
                    delete(target, cost.obj, fromInv)
                }
                if (fromStorage > 0) {
                    execute { TransactionResult.Ok(fromStorage, fromStorage) }
                }
            }
            alongside(output)
        }
    if (result.failure) {
        return result
    }
    consume.forEachIndexed { index, cost -> takeFromStorage(inv, cost, stored[index]) }
    result.commitAll()
    return result
}

private fun Player.storedShare(inv: Inventory, cost: InvCost): Int {
    val internal = cost.internal ?: return 0
    val storage = cachedPlayerItemStorage
    if (!storage.isManaged(this, inv, internal)) {
        return 0
    }
    val stored = storage.additionalCount(this, inv, internal)
    return when (storage.consumePolicy(this, inv, internal)) {
        VirtualItemConsumePolicy.StorageFirst -> minOf(cost.count, stored)
        VirtualItemConsumePolicy.InventoryFirst -> {
            val physical = inv.objs.sumOf { if (it?.id == cost.obj) it.count.toLong() else 0L }
            (cost.count - physical).coerceIn(0L, stored.toLong()).toInt()
        }
    }
}

private fun Player.takeFromStorage(inv: Inventory, cost: InvCost, amount: Int) {
    val internal = cost.internal
    if (internal == null || amount <= 0) {
        return
    }
    val removed = cachedPlayerItemStorage.removeFromStorage(this, inv, internal, amount)
    check(removed == amount) { "Storage gave $removed of $internal, $amount were counted." }
}
