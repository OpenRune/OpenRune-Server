package org.rsmod.content.skills.fishing

import dev.openrune.rscm.RSCM.asRSCM
import org.rsmod.api.death.*
import org.rsmod.api.death.PlayerDeathDrops.DeathDropResult

/** Barrels retain their account contents on death, and are always kept on a PvP death. */
internal class FishBarrelDeathHook : PlayerDeathDropHook {
    override fun resolve(context: PlayerDeathContext, result: DeathDropResult): DeathDropResult {
        if (!context.isPvpDeath)return result
        val ids = FishBarrelContents.forms.map { it.asRSCM() }.toSet()
        val retained = (result.lostTradeable + result.lostUntradeable + result.supplyPile).filter { it.id in ids }
        return result.copy(kept = result.kept + retained, lostTradeable = result.lostTradeable.filterNot { it.id in ids }, lostUntradeable = result.lostUntradeable.filterNot { it.id in ids }, supplyPile = result.supplyPile.filterNot { it.id in ids })
    }
}
