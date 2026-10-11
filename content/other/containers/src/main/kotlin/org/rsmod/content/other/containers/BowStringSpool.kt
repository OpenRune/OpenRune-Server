package org.rsmod.content.other.containers

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import jakarta.inject.Inject
import org.rsmod.api.inv.storage.*
import org.rsmod.api.invtx.*
import org.rsmod.api.player.vars.VarPlayerIntMapSetter
import org.rsmod.game.entity.Player

internal object BowStringSpool {
    const val ITEM = "obj.bowstring_spool"
    const val STRING = "obj.bow_string"
    const val CHARGES = "varbit.bowstring_spool_charges"
    const val SIZE = "varbit.bowstring_spool_size"
    val capacities = listOf(250, 500, 2000, 5000, 10000, 100000)
    fun owns(p: Player) = p.inv.any { it?.id == ITEM.asRSCM() }
    fun count(p: Player) = p.vars[CHARGES]
    fun tier(p: Player) = p.vars[SIZE].coerceIn(0, 5)
    fun capacity(p: Player) = capacities[tier(p)]
    fun set(p: Player, n: Int) { require(n in 0..capacity(p)); VarPlayerIntMapSetter.set(p, CHARGES, n) }
    fun fill(p: Player, source: Int, requested: Int = Int.MAX_VALUE): Int {
        if (!owns(p) || requested <= 0) return 0
        val string = STRING.asRSCM()
        if (source != string && source != ServerCacheManager.getItem(string)!!.certlink) return 0
        val physical = p.inv.objs.sumOf { if (it?.id == source) it.count.toLong() else 0L }
        val amount = minOf(physical, requested.toLong(), (capacity(p) - count(p)).toLong()).toInt()
        if (amount <= 0) return 0
        val result = p.invDel(p.inv, source, amount, autoCommit = false)
        if (result.failure) return 0
        result.commitAll(); set(p, count(p) + amount); return amount
    }
    fun withdraw(p: Player, noted: Boolean, requested: Int): Int {
        if (!owns(p) || requested <= 0 || count(p) == 0) return 0
        val string = STRING.asRSCM()
        val id = if (noted) ServerCacheManager.getItem(string)!!.certlink else string
        val result = p.invAdd(p.inv, id, minOf(requested, count(p)), strict = false, autoCommit = false)
        if (result.failure) return 0
        val moved = result.completed()
        result.commitAll(); set(p, count(p) - moved); return moved
    }
}

/** Stored strings participate in material checks without creating phantom physical items. */
internal class BowStringSpoolStorageHook @Inject constructor() : PlayerItemStorageHook {
    override fun shouldProcess(ctx: PlayerItemStorageContext) = ctx.inventory === ctx.player.inv && ctx.itemInternal == BowStringSpool.STRING && BowStringSpool.owns(ctx.player)
    override fun contains(ctx: PlayerItemStorageContext) = BowStringSpool.count(ctx.player)
    override fun incomingCapacity(ctx: PlayerItemStorageContext) = 0
    override fun add(ctx: PlayerItemStorageContext, amount: Int) = 0
    override fun remove(ctx: PlayerItemStorageContext, amount: Int): Int {
        val moved = minOf(amount, BowStringSpool.count(ctx.player))
        if (moved > 0) BowStringSpool.set(ctx.player, BowStringSpool.count(ctx.player) - moved)
        return moved
    }
}
