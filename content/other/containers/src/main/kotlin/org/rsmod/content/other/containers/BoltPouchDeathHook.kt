package org.rsmod.content.other.containers

import dev.openrune.rscm.RSCM.asRSCM
import jakarta.inject.Inject
import org.rsmod.api.death.PlayerDeathContext
import org.rsmod.api.death.PlayerDeathDropHook
import org.rsmod.api.death.PlayerDeathDrops.DeathDropResult
import org.rsmod.game.inv.InvObj

/** Bolt storage survives PvM retrieval and is spilled exactly once on PvP death. */
internal class BoltPouchDeathHook @Inject constructor() : PlayerDeathDropHook {
    private fun pouch(item: InvObj) = item.id == BoltPouch.ITEM.asRSCM()
    override fun neverProtected(item: InvObj): Boolean = pouch(item)
    override fun resolve(context: PlayerDeathContext, result: DeathDropResult): DeathDropResult {
        if (!context.isPvpDeath || !BoltPouch.owned(context.player)) return result
        fun List<InvObj>.withoutPouch() = filterNot(::pouch)
        val bolts = (1..4).mapNotNull { BoltPouch.contents(context.player, it) }
        return result.copy(
            kept = result.kept.withoutPouch(),
            supplyPile = result.supplyPile.withoutPouch(),
            lostTradeable = result.lostTradeable.withoutPouch() + bolts,
            lostUntradeable = result.lostUntradeable.withoutPouch(),
        )
    }
    override fun prepareCompletion(context: PlayerDeathContext): () -> Unit {
        val spill = context.isPvpDeath && BoltPouch.owned(context.player)
        return { if (spill) BoltPouch.clear(context.player) }
    }
}
