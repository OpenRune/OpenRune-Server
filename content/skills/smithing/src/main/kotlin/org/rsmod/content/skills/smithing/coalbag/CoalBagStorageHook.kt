package org.rsmod.content.skills.smithing.coalbag

import jakarta.inject.Inject
import jakarta.inject.Singleton
import org.rsmod.api.inv.storage.PlayerItemStorageContext
import org.rsmod.api.inv.storage.PlayerItemStorageHook
import org.rsmod.api.inv.storage.VirtualItemConsumePolicy

@Singleton
internal class CoalBagStorageHook @Inject constructor() : PlayerItemStorageHook {
    override val consumePolicy: VirtualItemConsumePolicy = VirtualItemConsumePolicy.InventoryFirst

    override fun contains(ctx: PlayerItemStorageContext): Int = CoalBag.storedAmount(ctx.player)

    override fun remove(ctx: PlayerItemStorageContext, amount: Int): Int =
        CoalBag.removeStored(ctx.player, amount)

    override fun add(ctx: PlayerItemStorageContext, amount: Int): Int =
        if (CoalBag.shouldInterceptIncoming(ctx.player)) CoalBag.depositUpTo(ctx.player, amount) else 0

    override fun incomingCapacity(ctx: PlayerItemStorageContext): Int = if (CoalBag.shouldInterceptIncoming(ctx.player)) CoalBag.freeSpace(ctx.player) else 0

    override fun shouldProcess(ctx: PlayerItemStorageContext): Boolean =
        ctx.inventory === ctx.player.inv && CoalBag.isCoal(ctx.itemInternal) && CoalBag.owns(ctx.player)
}
