package org.rsmod.content.raids.toa.raid

import jakarta.inject.Inject
import org.rsmod.api.death.PlayerDeathCleanupHook
import org.rsmod.game.entity.Player

class ToaRetrievalDeathHook @Inject constructor() : PlayerDeathCleanupHook {
    override fun cleanup(player: Player) {
        ToaRetrieval.discard(player)
    }
}
