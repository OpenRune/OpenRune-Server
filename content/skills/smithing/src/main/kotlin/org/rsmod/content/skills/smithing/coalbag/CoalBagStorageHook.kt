package org.rsmod.content.skills.smithing.coalbag

import jakarta.inject.Inject
import jakarta.inject.Singleton
import org.rsmod.api.inv.storage.PlayerItemStorageContext
import org.rsmod.api.inv.storage.PlayerItemStorageHook
import org.rsmod.api.inv.storage.VirtualItemConsumePolicy

@Singleton
internal class CoalBagStorageHook @Inject constructor() : PlayerItemStorageHook {
    override val consumePolicy: VirtualItemConsumePolicy = VirtualItemConsumePolicy.StorageFirst

    override fun contains(ctx: PlayerItemStorageContext): Int = CoalBag.storedAmount(ctx.player)

    override fun remove(ctx: PlayerItemStorageContext, amount: Int): Int =
        CoalBag.removeStored(ctx.player, amount)

    override fun add(ctx: PlayerItemStorageContext, amount: Int): Int =
        CoalBag.depositUpTo(ctx.player, amount)

    override fun shouldProcess(ctx: PlayerItemStorageContext): Boolean =
        CoalBag.isCoal(ctx.itemInternal) && CoalBag.shouldInterceptIncoming(ctx.player)
}
