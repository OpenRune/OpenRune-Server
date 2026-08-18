package org.rsmod.api.inv.storage

import org.rsmod.game.entity.Player

/**
 * Virtual item storage (coal bag, rune pouch, etc.). [PlayerItemStorage] calls [shouldProcess]
 * before every operation — only implement the storage logic here.
 */
public interface PlayerItemStorageHook {
    public val consumePolicy: VirtualItemConsumePolicy
        get() = VirtualItemConsumePolicy.InventoryFirst

    public fun shouldProcess(ctx: PlayerItemStorageContext): Boolean

    public fun contains(ctx: PlayerItemStorageContext): Int

    public fun remove(ctx: PlayerItemStorageContext, amount: Int): Int

    public fun add(ctx: PlayerItemStorageContext, amount: Int): Int

    /** True when this storage provides any item in [contentGroup] for [player]. */
    public fun providesContentGroup(player: Player, contentGroup: Int): Boolean = false
}
