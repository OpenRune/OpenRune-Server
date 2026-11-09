package org.rsmod.content.other.planksack

import jakarta.inject.Inject
import jakarta.inject.Singleton
import org.rsmod.api.inv.storage.PlayerItemStorageContext
import org.rsmod.api.inv.storage.PlayerItemStorageHook
import org.rsmod.api.inv.storage.VirtualItemConsumePolicy

@Singleton
class PlankSackStorageHook @Inject constructor() : PlayerItemStorageHook {
    override val consumePolicy: VirtualItemConsumePolicy = VirtualItemConsumePolicy.InventoryFirst

    override fun shouldProcess(ctx: PlayerItemStorageContext): Boolean =
        ctx.inventory === ctx.player.inv &&
            PlankSack.plank(ctx.itemInternal) != null &&
            PlankSack.hasSack(ctx.player)

    override fun contains(ctx: PlayerItemStorageContext): Int {
        val plank = PlankSack.plank(ctx.itemInternal) ?: return 0
        return PlankSack.stored(ctx.player, plank)
    }

    override fun remove(ctx: PlayerItemStorageContext, amount: Int): Int {
        val plank = PlankSack.plank(ctx.itemInternal) ?: return 0
        return PlankSack.remove(ctx.player, plank, amount)
    }

    override fun add(ctx: PlayerItemStorageContext, amount: Int): Int = 0
}
