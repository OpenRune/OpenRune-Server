package org.rsmod.api.inv.storage

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
}
