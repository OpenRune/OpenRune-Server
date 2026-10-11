package org.rsmod.api.inv.storage

import jakarta.inject.Inject
import jakarta.inject.Singleton
import org.rsmod.game.entity.Player
import org.rsmod.game.inv.InvVirtualStorage
import org.rsmod.game.inv.Inventory

@Singleton
public class PlayerItemStorage
@Inject
constructor(private val hooks: Set<@JvmSuppressWildcards PlayerItemStorageHook>) : InvVirtualStorage {

    public fun isManaged(player: Player, inventory: Inventory, itemInternal: String, source: ItemStorageSource = ItemStorageSource.Default, incomingCount: Int = 0): Boolean {
        val ctx = PlayerItemStorageContext(player, inventory, itemInternal, source, incomingCount)
        return hooks.any { it.shouldProcess(ctx) }
    }

    override fun additionalCount(player: Player, inventory: Inventory, itemInternal: String): Int {
        val ctx = PlayerItemStorageContext(player, inventory, itemInternal)
        var total = 0L
        for (hook in hooks) {
            if (!hook.shouldProcess(ctx)) {
                continue
            }
            total += hook.contains(ctx)
        }
        return total.coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
    }

    public fun consumePolicy(
        player: Player,
        inventory: Inventory,
        itemInternal: String,
    ): VirtualItemConsumePolicy {
        val ctx = PlayerItemStorageContext(player, inventory, itemInternal)
        for (hook in hooks) {
            if (!hook.shouldProcess(ctx)) {
                continue
            }
            return hook.consumePolicy
        }
        return VirtualItemConsumePolicy.InventoryFirst
    }

    /** Plans are immutable previews. Native vars/attributes change only at commit. */
    public fun planTransfer(player: Player, inventory: Inventory, itemInternal: String, amount: Int, incoming: Boolean, source: ItemStorageSource = ItemStorageSource.Default): StorageTransfer {
        val ctx = PlayerItemStorageContext(player, inventory, itemInternal, source, if (incoming) amount else 0)
        var remaining = amount.coerceAtLeast(0)
        val steps = mutableListOf<StorageTransfer.Step>()
        for (hook in hooks) {
            if (remaining == 0 || !hook.shouldProcess(ctx)) continue
            val count = hook.contains(ctx)
            val capacity = hook.incomingCapacity(ctx)
            val moved = minOf(remaining, if (incoming) capacity else count).coerceAtLeast(0)
            if (moved == 0) continue
            steps += StorageTransfer.Step(
                amount = moved,
                valid = { hook.shouldProcess(ctx) && hook.contains(ctx) == count && (!incoming || hook.incomingCapacity(ctx) == capacity) },
                commit = { if (incoming) hook.add(ctx, moved) else hook.remove(ctx, moved) },
            )
            remaining -= moved
        }
        return StorageTransfer(steps)
    }

    public fun removeFromStorage(
        player: Player,
        inventory: Inventory,
        itemInternal: String,
        amount: Int,
    ): Int {
        if (amount <= 0) {
            return 0
        }
        var remaining = amount
        var removed = 0
        val ctx = PlayerItemStorageContext(player, inventory, itemInternal)
        for (hook in hooks) {
            if (!hook.shouldProcess(ctx)) {
                continue
            }
            val taken = hook.remove(ctx, remaining)
            removed += taken
            remaining -= taken
            if (remaining <= 0) {
                break
            }
        }
        return removed
    }

    public fun storeIncoming(
        player: Player,
        inventory: Inventory,
        itemInternal: String,
        amount: Int,
    ): Int {
        if (amount <= 0) {
            return 0
        }

        var remaining = amount
        var stored = 0
        val ctx = PlayerItemStorageContext(player, inventory, itemInternal)

        for (hook in hooks) {
            if (!hook.shouldProcess(ctx) || remaining <= 0) {
                continue
            }
            val added = hook.add(ctx, remaining)
            stored += added
            remaining -= added
        }

        return stored
    }
}

public class StorageTransfer internal constructor(private val steps: List<Step>) {
    public val amount: Int = steps.sumOf { it.amount }
    public fun valid(): Boolean = steps.all { it.valid() }
    public fun commit() { for (step in steps) check(step.commit() == step.amount) { "Storage transfer did not match its preview." } }
    internal class Step(val amount: Int, val valid: () -> Boolean, val commit: () -> Int)
}
