package org.rsmod.content.other.containers

import dev.openrune.ServerCacheManager
import jakarta.inject.Inject
import org.rsmod.api.death.PlayerDeathContext
import org.rsmod.api.death.PlayerDeathDropHook
import org.rsmod.api.death.PlayerDeathDrops.DeathDropResult
import org.rsmod.game.inv.InvObj

/** Account storage is only cleared after death drops have actually been committed. */
internal class SeedBoxDeathHook @Inject constructor(private val store: ContainerStore, private val box: SeedBox) : PlayerDeathDropHook {
    private fun isBox(item: InvObj) = box.recognises(item.id)
    private fun lost(context: PlayerDeathContext, result: DeathDropResult) = box.owns(context.player) && result.kept.none(::isBox)
    private fun destroy(context: PlayerDeathContext, result: DeathDropResult) = lost(context, result) && context.inWilderness && context.wildernessLevel > 20
    private fun spills(context: PlayerDeathContext, result: DeathDropResult): Boolean {
        if (!box.owns(context.player)) return false
        if (context.isPvpDeath || context.player.vars["varbit.emptyondeath_seedbox"] > 0) return true
        // Since April 2021 a PvM loss above 20 Wilderness leaves ghost contents for a new box.
        if (destroy(context, result)) return context.isUIM
        return lost(context, result)
    }
    override fun resolve(context: PlayerDeathContext, result: DeathDropResult): DeathDropResult {
        val seeds = if (spills(context, result)) store.contents(context.player, SeedBox.spec).map { (id, count) -> InvObj(ServerCacheManager.getItem(id.toInt())!!, count) } else emptyList()
        fun List<InvObj>.retained() = if (destroy(context, result)) filterNot(::isBox) else this
        return result.copy(kept = result.kept.retained(), supplyPile = result.supplyPile.retained(), lostTradeable = result.lostTradeable.retained() + seeds, lostUntradeable = result.lostUntradeable.retained())
    }
    override fun prepareCompletion(context: PlayerDeathContext): () -> Unit {
        val owned = box.owns(context.player)
        val forced = context.isPvpDeath || context.player.vars["varbit.emptyondeath_seedbox"] > 0
        return {
            val retained = box.owns(context.player)
            val ghost = context.inWilderness && context.wildernessLevel > 20 && !context.isUIM
            if (owned && (forced || !retained && !ghost)) {
                store.clear(context.player, SeedBox.spec)
                context.player.attr.remove(SeedBox.ORDER)
            }
        }
    }
}
