package org.rsmod.content.other.containers

import dev.openrune.rscm.RSCM.asRSCM
import jakarta.inject.Inject
import org.rsmod.api.inv.storage.*
import org.rsmod.plugin.module.PluginModule

internal class ContainersModule : PluginModule() {
    override fun bind() {
        addSetBinding<org.rsmod.content.skills.crafting.CraftingRecipeProvider>(ContainerCraftingScript::class.java)
        addSetBinding<org.rsmod.content.skills.crafting.CraftingRecipeProvider>(ForestryCraftingProvider::class.java)
        addSetBinding<org.rsmod.content.skills.farming.FarmingSeedContainer>(SeedBox::class.java)
        addSetBinding<org.rsmod.api.stats.xpmod.XpMod>(ForestryOutfitXpMod::class.java)
        addSetBinding<PlayerItemStorageHook>(ContainerStorageHook::class.java)
        addSetBinding<PlayerItemStorageHook>(BowStringSpoolStorageHook::class.java)
        addSetBinding<org.rsmod.content.interfaces.omnishop.OmnishopPurchase>(MixologyContainerRewards::class.java)
        addSetBinding<org.rsmod.content.interfaces.bank.BankDepositStorage>(PotionStorage::class.java)
        addSetBinding<org.rsmod.content.interfaces.bank.BankDepositStorage>(ContainerBankDeposits::class.java)
        addSetBinding<org.rsmod.api.death.PlayerDeathDropHook>(ContainerDeathHook::class.java)
        addSetBinding<org.rsmod.api.death.PlayerDeathDropHook>(SeedBoxDeathHook::class.java)
        addSetBinding<org.rsmod.api.death.PlayerDeathDropHook>(BoltPouchDeathHook::class.java)
        addSetBinding<org.rsmod.api.death.PlayerDeathDropHook>(QuiverDeathHook::class.java)
        addSetBinding<org.rsmod.api.player.hook.PlayerObjTakeStorageHook>(QuiverPickupLocks::class.java)
    }
}

internal class ContainerStorageHook @Inject constructor(private val store: ContainerStore) : PlayerItemStorageHook {
    private fun spec(ctx: PlayerItemStorageContext): ContainerSpec? {
        if (ctx.inventory !== ctx.player.inv) return null
        val id = ctx.itemInternal.asRSCM()
        return ContainerCatalog.all.filter { id in it.accepted && store.owns(ctx.player, it) &&
            (collects(it, id) || (it.family in CONSUMABLE && (it.family != "reagent_pouch" || it.opened))) }.maxByOrNull { it.capacity }
    }
    override fun shouldProcess(ctx: PlayerItemStorageContext) = spec(ctx) != null
    override fun contains(ctx: PlayerItemStorageContext): Int = spec(ctx)?.takeIf { it.family in CONSUMABLE }?.let { store.count(ctx.player, it, ctx.itemInternal.asRSCM()) } ?: 0
    override fun incomingCapacity(ctx: PlayerItemStorageContext): Int = spec(ctx)?.takeIf { collects(it, ctx.itemInternal.asRSCM()) }?.let { store.room(ctx.player, it, ctx.itemInternal.asRSCM()) } ?: 0
    override fun remove(ctx: PlayerItemStorageContext, amount: Int): Int {
        val s = spec(ctx) ?: return 0
        if (s.family !in CONSUMABLE) return 0
        val moved = minOf(amount, store.count(ctx.player, s, ctx.itemInternal.asRSCM()))
        if (moved > 0) store.update(ctx.player, s, ctx.itemInternal.asRSCM(), -moved)
        return moved
    }
    override fun add(ctx: PlayerItemStorageContext, amount: Int): Int {
        val s = spec(ctx) ?: return 0
        val moved = minOf(amount, incomingCapacity(ctx))
        if (moved > 0) store.update(ctx.player, s, ctx.itemInternal.asRSCM(), moved)
        return moved
    }
    private fun collects(spec: ContainerSpec, item: Int): Boolean = spec.autoCollect && spec.opened || spec.family == "forestry_kit" && (item in ForestryContainers.leaves || item == "obj.forestry_currency".asRSCM())
    private companion object { val CONSUMABLE = setOf("plank_sack", "reagent_pouch") }
}
