package org.rsmod.api.death

import jakarta.inject.Inject
import jakarta.inject.Singleton
import org.rsmod.api.death.PlayerDeathDrops.Companion.DROP_DURATION_STANDARD
import org.rsmod.api.death.PlayerDeathDrops.Companion.PVP_REVEAL_DELAY
import org.rsmod.api.death.PlayerDeathDrops.Companion.standardKeepCount

@Singleton
public class PlayerDeathHandlingResolver
@Inject
constructor(
    private val hooks: Set<PlayerDeathHook>,
) {
    public fun resolve(context: PlayerDeathContext): PlayerDeathHandling {
        for (hook in hooks) {
            val handling = hook.handleDeath(context)
            if (handling != null) return handling
        }
        return defaultHandling(context)
    }

    private fun defaultHandling(context: PlayerDeathContext): PlayerDeathHandling =
        PlayerDeathHandling(
            keepCount = standardKeepCount(context.hasProtectItem),
            dropReceiver = context.player,
            dropDuration = DROP_DURATION_STANDARD,
            revealDelay = PVP_REVEAL_DELAY,
            supplyPile = false,
            untradeableHandling = UntradeableHandling.DROP,
        )
}
