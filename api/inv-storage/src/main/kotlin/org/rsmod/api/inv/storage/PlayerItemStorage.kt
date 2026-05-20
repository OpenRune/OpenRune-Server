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

    public fun isManaged(player: Player, inventory: Inventory, itemInternal: String): Boolean {
        val ctx = PlayerItemStorageContext(player, inventory, itemInternal)
        return hooks.any { it.shouldProcess(ctx) }
    }

    override fun additionalCount(player: Player, inventory: Inventory, itemInternal: String): Int {
        val ctx = PlayerItemStorageContext(player, inventory, itemInternal)
        var total = 0
        for (hook in hooks) {
            if (!hook.shouldProcess(ctx)) {
                continue
            }
            total += hook.contains(ctx)
        }
        return total
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
