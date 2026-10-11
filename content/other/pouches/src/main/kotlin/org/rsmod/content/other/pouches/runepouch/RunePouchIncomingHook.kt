package org.rsmod.content.other.pouches.runepouch

import dev.openrune.rscm.RSCM.asRSCM
import jakarta.inject.Inject
import org.rsmod.api.inv.storage.*

/** Native setting 306: only complete matching stacks from pickup or Runecrafting are inserted. */
class RunePouchIncomingHook @Inject constructor() : PlayerItemStorageHook {
    override fun shouldProcess(ctx: PlayerItemStorageContext): Boolean =
        ctx.inventory === ctx.player.inv && ctx.source in SOURCES && ctx.incomingCount > 0 &&
            ctx.player.vars["varbit.take_runes_toggle"] != 0 && ctx.player.runePouchSlots() != null &&
            room(ctx) >= ctx.incomingCount

    override fun contains(ctx: PlayerItemStorageContext): Int = 0
    override fun remove(ctx: PlayerItemStorageContext, amount: Int): Int = 0
    override fun incomingCapacity(ctx: PlayerItemStorageContext): Int = room(ctx)
    override fun add(ctx: PlayerItemStorageContext, amount: Int): Int {
        if (!shouldProcess(ctx)) return 0
        val rune = RunePouchRunes.byObj(ctx.itemInternal.asRSCM()) ?: return 0
        val contents = ctx.player.readRunePouch(ctx.player.runePouchSlots() ?: return 0)
        val moved = contents.add(rune.compactId, amount)
        if (moved > 0) ctx.player.writeRunePouch(contents)
        return moved
    }
    private fun room(ctx: PlayerItemStorageContext): Int {
        val rune = RunePouchRunes.byObj(ctx.itemInternal.asRSCM()) ?: return 0
        val slots = ctx.player.runePouchSlots() ?: return 0
        val contents = ctx.player.readRunePouch(slots)
        val slot = contents.slotOf(rune.compactId)
        return if (slot >= 0) (RunePouchContents.MAX_STACK - contents.counts[slot]).coerceAtLeast(0) else 0
    }
    private companion object { val SOURCES = setOf(ItemStorageSource.GroundPickup, ItemStorageSource.Runecrafting) }
}
