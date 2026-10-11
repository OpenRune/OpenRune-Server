package org.rsmod.content.other.containers

import dev.openrune.ServerCacheManager
import jakarta.inject.Inject
import org.rsmod.api.death.PlayerDeathContext
import org.rsmod.api.death.PlayerDeathDropHook
import org.rsmod.api.death.PlayerDeathDrops.DeathDropResult
import org.rsmod.game.inv.InvObj

/** Death previews cannot mutate account storage. Cleanup is captured before inventory removal. */
internal class ContainerDeathHook @Inject constructor(private val store: ContainerStore) : PlayerDeathDropHook {
    private fun carried(context: PlayerDeathContext) = ContainerCatalog.all.filter { store.owns(context.player, it) }.distinctBy { it.family }
    private fun spills(spec: ContainerSpec, context: PlayerDeathContext, result: DeathDropResult): Boolean =
        context.isPvpDeath && (spec.family == "herb_sack" || (spec.family in setOf("tackle_box", "plank_sack") && result.kept.none { ContainerCatalog.byId[it.id]?.family == spec.family }))

    override fun resolve(context: PlayerDeathContext, result: DeathDropResult): DeathDropResult {
        val dropped = carried(context).filter { spills(it, context, result) }.flatMap { s ->
            store.contents(context.player, s).mapNotNull { (id, count) ->
                ServerCacheManager.getItem(id.toInt())?.takeIf { it.tradeable }?.let { InvObj(it, count) }
            }
        }
        fun List<InvObj>.retained() = filterNot { ContainerCatalog.byId[it.id]?.family == "flamtaer_bag" }
        return result.copy(kept = result.kept.retained(), supplyPile = result.supplyPile.retained(),
            lostTradeable = result.lostTradeable.retained() + dropped, lostUntradeable = result.lostUntradeable.retained())
    }

    override fun prepareCompletion(context: PlayerDeathContext): () -> Unit {
        val carried = carried(context)
        // Protection choices use the same death selection that is committed by the death service.
        return {
            for (spec in carried) {
                val retained = context.player.inv.any { ContainerCatalog.byId[it?.id]?.family == spec.family } || context.player.worn.any { ContainerCatalog.byId[it?.id]?.family == spec.family }
                when {
                    spec.family == "flamtaer_bag" || context.isPvpDeath && spec.family == "herb_sack" -> context.player.attr.remove(spec.stateKey)
                    context.isPvpDeath && spec.family in setOf("tackle_box", "plank_sack") && !retained -> {
                        for ((id, count) in store.contents(context.player, spec).toMap())
                            if (ServerCacheManager.getItem(id.toInt())?.tradeable == true) store.update(context.player, spec, id.toInt(), -count)
                    }
                }
            }
        }
    }
}
