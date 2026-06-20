package org.rsmod.api.death.plugin

import jakarta.inject.Inject
import org.rsmod.api.death.PlayerDeathContext
import org.rsmod.api.death.PlayerDeathDrops.Companion.DROP_DURATION_STANDARD
import org.rsmod.api.death.PlayerDeathHandling
import org.rsmod.api.death.PlayerDeathHook
import org.rsmod.api.death.UntradeableHandling

public class UimPlayerDeathHook @Inject constructor() : PlayerDeathHook {
    override fun handleDeath(context: PlayerDeathContext): PlayerDeathHandling? {
        if (!context.isUIM) return null
        return PlayerDeathHandling(
            keepCount = 0,
            dropReceiver = null,
            dropDuration = DROP_DURATION_STANDARD,
            revealDelay = 0,
            supplyPile = false,
            untradeableHandling = UntradeableHandling.DROP,
        )
    }
}
