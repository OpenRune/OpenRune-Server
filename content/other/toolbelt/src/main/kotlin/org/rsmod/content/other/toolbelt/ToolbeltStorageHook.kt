package org.rsmod.content.other.toolbelt

import jakarta.inject.Inject
import jakarta.inject.Singleton
import org.rsmod.api.inv.storage.PlayerItemStorageContext
import org.rsmod.api.inv.storage.PlayerItemStorageHook
import org.rsmod.api.inv.storage.VirtualItemConsumePolicy
import org.rsmod.game.entity.Player

@Singleton
internal class ToolbeltStorageHook @Inject constructor() : PlayerItemStorageHook {
    override val consumePolicy: VirtualItemConsumePolicy = VirtualItemConsumePolicy.InventoryFirst

    override fun shouldProcess(ctx: PlayerItemStorageContext): Boolean =
        Toolbelt.isEnabled() && Toolbelt.provides(ctx.player, ctx.itemInternal)

    override fun contains(ctx: PlayerItemStorageContext): Int =
        if (Toolbelt.provides(ctx.player, ctx.itemInternal)) 1 else 0

    override fun remove(ctx: PlayerItemStorageContext, amount: Int): Int = 0

    override fun add(ctx: PlayerItemStorageContext, amount: Int): Int = 0

    override fun providesContentGroup(player: Player, contentGroup: Int): Boolean =
        Toolbelt.providesContentGroup(player, contentGroup)
}
