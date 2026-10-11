package org.rsmod.api.invtx

import org.rsmod.api.inv.storage.PlayerItemStorage
import org.rsmod.api.inv.storage.VirtualItemConsumePolicy
import org.rsmod.game.entity.Player
import org.rsmod.game.inv.InvObj
import org.rsmod.game.inv.Inventory
import org.rsmod.objtx.TransactionResult
import org.rsmod.objtx.TransactionResultList

internal lateinit var cachedPlayerItemStorage: PlayerItemStorage

internal fun Player.invDelWithVirtualStorage(
    inv: Inventory,
    type: String,
    count: Int,
    slot: Int?,
    strict: Boolean,
    placehold: Boolean,
    autoCommit: Boolean,
    ignoreVirtualStorage: Boolean,
    raw: (
        inv: Inventory,
        type: String,
        count: Int,
        slot: Int?,
        strict: Boolean,
        placehold: Boolean,
        autoCommit: Boolean,
    ) -> TransactionResultList<InvObj>,
): TransactionResultList<InvObj> {
    val storage = cachedPlayerItemStorage
    if (count <= 0 || ignoreVirtualStorage || (inv.type.protect && isAccessProtected) || !storage.isManaged(this, inv, type)) {
        return raw(inv, type, count, slot, strict, placehold, autoCommit)
    }

    val physical = inv.physicalCount(type)
    val virtual = storage.additionalCount(this, inv, type)
    val total = (physical.toLong() + virtual).coerceAtMost(Int.MAX_VALUE.toLong()).toInt()

    if (strict && total < count) {
        return raw(inv, type, count, slot, strict, placehold, autoCommit)
    }

    val effectiveCount = if (strict) count else minOf(count, total)
    val policy = storage.consumePolicy(this, inv, type)
    val (fromInv, fromStorage) =
        when (policy) {
            VirtualItemConsumePolicy.InventoryFirst -> {
                minOf(effectiveCount, physical) to (effectiveCount - minOf(effectiveCount, physical))
            }
            VirtualItemConsumePolicy.StorageFirst -> {
                val fromStore = minOf(effectiveCount, virtual)
                (effectiveCount - fromStore) to fromStore
            }
        }

    val transfer = storage.planTransfer(this, inv, type, fromStorage, incoming = false)
    if (transfer.amount != fromStorage) return raw(inv, type, count, slot, strict, placehold, autoCommit)
    val physicalResult = if (fromInv > 0) raw(inv, type, fromInv, slot, true, placehold, false) else virtualOnlyTransactionResult(effectiveCount, false)
    if (physicalResult.failure) return physicalResult
    val result = physicalResult.withStorageTransfer(effectiveCount, transfer)
    if (autoCommit) result.commitAll()
    return result
}

internal fun Player.invAddWithVirtualStorage(
    inv: Inventory,
    type: String,
    count: Int,
    vars: Int,
    slot: Int?,
    strict: Boolean,
    cert: Boolean,
    uncert: Boolean,
    autoCommit: Boolean,
    ignoreVirtualStorage: Boolean,
    raw: (
        inv: Inventory,
        type: String,
        count: Int,
        vars: Int,
        slot: Int?,
        strict: Boolean,
        cert: Boolean,
        uncert: Boolean,
        autoCommit: Boolean,
    ) -> TransactionResultList<InvObj>,
): TransactionResultList<InvObj> {
    val storage = cachedPlayerItemStorage
    if (count <= 0 || cert || vars != 0 || ignoreVirtualStorage || (inv.type.protect && isAccessProtected) || !storage.isManaged(this, inv, type)) {
        return raw(inv, type, count, vars, slot, strict, cert, uncert, autoCommit)
    }

    val transfer = storage.planTransfer(this, inv, type, count, incoming = true)
    val toInv = count - transfer.amount
    val physicalResult = if (toInv > 0) raw(inv, type, toInv, vars, slot, strict, cert, uncert, false) else virtualOnlyTransactionResult(count, false)
    if (physicalResult.failure) return physicalResult
    val completed = physicalResult.completed() + (if (toInv > 0) transfer.amount else 0)
    val result = physicalResult.withStorageTransfer(completed, transfer)
    if (autoCommit) result.commitAll()
    return result
}

private fun virtualOnlyTransactionResult(
    count: Int,
    autoCommit: Boolean,
): TransactionResultList<InvObj> {
    val result =
        TransactionResultList<InvObj>(
            output = { null },
            inventories = emptyList(),
            results = listOf(TransactionResult.Ok(requested = count, completed = count)),
        )
    if (autoCommit) {
        result.commitAll()
    }
    return result
}

private fun TransactionResultList<InvObj>.withStorageTransfer(completed: Int, transfer: org.rsmod.api.inv.storage.StorageTransfer): TransactionResultList<InvObj> =
    copy(results = listOf(TransactionResult.Ok(requested = completed, completed = completed)),
        validateCommit = { transfer.valid() }, afterCommit = { transfer.commit() })
