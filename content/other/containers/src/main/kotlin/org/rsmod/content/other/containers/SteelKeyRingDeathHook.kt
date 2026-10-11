package org.rsmod.content.other.containers

import dev.openrune.rscm.RSCM.asRSCM
import jakarta.inject.Inject
import org.rsmod.api.death.*

/** The ring and its reusable keys remain kept on both PvM and PvP deaths. */
internal class SteelKeyRingDeathHook @Inject constructor() : PlayerDeathDropHook {
    override fun resolve(context: PlayerDeathContext, result: PlayerDeathDrops.DeathDropResult): PlayerDeathDrops.DeathDropResult {
        val id = SteelKeyRing.ITEM.asRSCM()
        val rings = (result.supplyPile + result.lostTradeable + result.lostUntradeable).filter { it.id == id }
        return result.copy(kept = result.kept + rings, supplyPile = result.supplyPile.filterNot { it.id == id }, lostTradeable = result.lostTradeable.filterNot { it.id == id }, lostUntradeable = result.lostUntradeable.filterNot { it.id == id })
    }
}
