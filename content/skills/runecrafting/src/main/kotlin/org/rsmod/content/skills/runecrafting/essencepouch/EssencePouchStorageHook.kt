package org.rsmod.content.skills.runecrafting.essencepouch

import jakarta.inject.Inject
import jakarta.inject.Singleton
import org.rsmod.api.inv.storage.PlayerItemStorageContext
import org.rsmod.api.inv.storage.PlayerItemStorageHook
import org.rsmod.api.inv.storage.VirtualItemConsumePolicy

@Singleton
internal class EssencePouchStorageHook @Inject constructor() : PlayerItemStorageHook {
    override val consumePolicy: VirtualItemConsumePolicy = VirtualItemConsumePolicy.InventoryFirst

    override fun shouldProcess(ctx: PlayerItemStorageContext): Boolean =
        EssencePouch.shouldIntercept(ctx.player, ctx.itemInternal)

    override fun contains(ctx: PlayerItemStorageContext): Int =
        EssencePouch.storedAmountForType(ctx.player, ctx.itemInternal)

    override fun remove(ctx: PlayerItemStorageContext, amount: Int): Int =
        EssencePouch.removeStoredForType(ctx.player, ctx.itemInternal, amount)

    override fun add(ctx: PlayerItemStorageContext, amount: Int): Int = 0
}
