package org.rsmod.content.areas.wilderness

import jakarta.inject.Inject
import org.rsmod.api.death.PlayerDeathContext
import org.rsmod.api.death.PlayerDeathDrops.Companion.DROP_DURATION_PVP
import org.rsmod.api.death.PlayerDeathDrops.Companion.DROP_DURATION_REVENANT
import org.rsmod.api.death.PlayerDeathDrops.Companion.DROP_DURATION_STANDARD
import org.rsmod.api.death.PlayerDeathDrops.Companion.PVP_REVEAL_DELAY
import org.rsmod.api.death.PlayerDeathDrops.Companion.standardKeepCount
import org.rsmod.api.death.PlayerDeathDrops.Companion.wildernessKeepCount
import org.rsmod.api.death.PlayerDeathHandling
import org.rsmod.api.death.PlayerDeathHook
import org.rsmod.api.death.UntradeableHandling

public class WildernessDeathHook @Inject constructor() : PlayerDeathHook {
    override fun handleDeath(context: PlayerDeathContext): PlayerDeathHandling? {
        if (!context.inWilderness) return null
        if (context.inInstance) return wildernessInstance(context)
        if (context.isPvpDeath) return wildernessPvpDeath(context)
        if (context.inRevenantCaves) return revenantCaves(context)
        return wildernessPvmDeath(context)
    }

    private fun wildernessPvpDeath(context: PlayerDeathContext): PlayerDeathHandling {
        val above20 = context.wildernessLevel > ABOVE_20_THRESHOLD
        return PlayerDeathHandling(
            keepCount = wildernessKeepCount(context.isSkulled, context.hasProtectItem),
            dropReceiver = context.killer,
            dropDuration = DROP_DURATION_PVP,
            revealDelay = if (context.killer != null) PVP_REVEAL_DELAY else 0,
            supplyPile = false,
            untradeableHandling = if (above20) UntradeableHandling.COINS else UntradeableHandling.DROP,
        )
    }

    private fun wildernessPvmDeath(context: PlayerDeathContext): PlayerDeathHandling =
        PlayerDeathHandling(
            keepCount = standardKeepCount(context.hasProtectItem),
            dropReceiver = context.player,
            dropDuration = DROP_DURATION_STANDARD,
            revealDelay = PVP_REVEAL_DELAY,
            supplyPile = true,
            untradeableHandling = UntradeableHandling.DROP,
        )

    private fun revenantCaves(context: PlayerDeathContext): PlayerDeathHandling =
        PlayerDeathHandling(
            keepCount = standardKeepCount(context.hasProtectItem),
            dropReceiver = null,
            dropDuration = DROP_DURATION_REVENANT,
            revealDelay = 0,
            supplyPile = false,
            untradeableHandling = UntradeableHandling.COINS,
        )

    private fun wildernessInstance(context: PlayerDeathContext): PlayerDeathHandling =
        PlayerDeathHandling(
            keepCount = standardKeepCount(context.hasProtectItem),
            dropReceiver = context.player,
            dropDuration = DROP_DURATION_STANDARD,
            revealDelay = PVP_REVEAL_DELAY,
            supplyPile = false,
            untradeableHandling = UntradeableHandling.KEEP,
        )

    private companion object {
        private const val ABOVE_20_THRESHOLD = 20
    }
}
