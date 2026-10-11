package org.rsmod.content.skills.fishing

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import org.rsmod.api.attr.AttributeKey
import org.rsmod.api.inv.storage.*
import org.rsmod.api.table.fishing.FishingSpotRow
import org.rsmod.game.entity.Player

/** A shared, persistent account inventory; never exposes its fish as cooking or eating inputs. */
internal object FishBarrelContents {
    const val CAPACITY = 28
    val forms = listOf("obj.fish_barrel_closed", "obj.fish_barrel_open", "obj.fish_sack_barrel_closed", "obj.fish_sack_barrel_open")
    val key = AttributeKey<MutableMap<String, Int>>(persistenceKey = "fish_barrel")
    val allowed: Set<String> by lazy {
        FishingSpotRow.all().map { RSCM.getReverseMapping(RSCMType.OBJ, it.fish.id) }.plus("obj.tbwt_raw_karambwan").filterTo(linkedSetOf()) { symbol ->
            val type = ServerCacheManager.getItem(symbol.asRSCM())!!
            !type.stackable && type.certtemplate == 0 && !symbol.contains("harpoonfish") && !symbol.contains("leechfin") && !symbol.contains("trophy") &&
                !symbol.contains("cox_") && !symbol.contains("raid_") && !symbol.contains("camdozaal") && !symbol.contains("moon_")
        }
    }
    fun owns(player: Player) = forms.any { it in player.inv || it in player.worn }
    fun opened(player: Player) = listOf(forms[1], forms[3]).any { it in player.inv || it in player.worn }
    fun contents(player: Player): Map<String, Int> = player.attr[key].orEmpty()
    fun total(player: Player) = contents(player).values.sumOf { it.toLong() }.coerceIn(0, Int.MAX_VALUE.toLong()).toInt()
    fun room(player: Player, item: String) = if (item in allowed && opened(player)) (CAPACITY - total(player)).coerceAtLeast(0) else 0
    fun update(player: Player, item: String, delta: Int) {
        val copy = contents(player).toMutableMap(); val n = (copy[item] ?: 0) + delta; check(n >= 0)
        if (n == 0)copy.remove(item)else copy[item] = n
        if (copy.isEmpty())player.attr.remove(key)else player.attr[key] = copy
    }
}
internal class FishBarrelStorageHook : PlayerItemStorageHook {
    override fun shouldProcess(ctx: PlayerItemStorageContext) = ctx.inventory === ctx.player.inv && ctx.itemInternal in FishBarrelContents.allowed && FishBarrelContents.opened(ctx.player)
    override fun contains(ctx: PlayerItemStorageContext) = 0
    override fun incomingCapacity(ctx: PlayerItemStorageContext) = FishBarrelContents.room(ctx.player, ctx.itemInternal)
    override fun add(ctx: PlayerItemStorageContext, amount: Int): Int { val n = minOf(amount, incomingCapacity(ctx)); if (n > 0)FishBarrelContents.update(ctx.player, ctx.itemInternal, n); return n }
    override fun remove(ctx: PlayerItemStorageContext, amount: Int) = 0
}
