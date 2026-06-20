package org.rsmod.api.death.plugin

import jakarta.inject.Inject
import org.rsmod.api.death.PlayerDeathContext
import org.rsmod.api.death.PlayerDeathDrops.Companion.DROP_DURATION_PVP_EXTERNAL
import org.rsmod.api.death.PlayerDeathDrops.Companion.DROP_DURATION_STANDARD
import org.rsmod.api.death.PlayerDeathDrops.Companion.PVP_REVEAL_DELAY
import org.rsmod.api.death.PlayerDeathDrops.Companion.standardKeepCount
import org.rsmod.api.death.PlayerDeathDrops.Companion.wildernessKeepCount
import org.rsmod.api.death.PlayerDeathHandling
import org.rsmod.api.death.PlayerDeathHook
import org.rsmod.api.death.UntradeableHandling

public class StandardPvmDeathHook @Inject constructor() : PlayerDeathHook {
    override fun handleDeath(context: PlayerDeathContext): PlayerDeathHandling? {
        if (context.inWilderness) return null
        if (context.recentPvpDamage) return pvpOutsideWilderness(context)
        if (context.inInstance) return instancePvmDeath(context)
        return standardPvmDeath(context)
    }

    private fun standardPvmDeath(context: PlayerDeathContext): PlayerDeathHandling =
        PlayerDeathHandling(
            keepCount = standardKeepCount(context.hasProtectItem),
            dropReceiver = context.player,
            dropDuration = DROP_DURATION_STANDARD,
            revealDelay = PVP_REVEAL_DELAY,
            supplyPile = true,
            untradeableHandling = UntradeableHandling.DROP,
        )

    private fun instancePvmDeath(context: PlayerDeathContext): PlayerDeathHandling =
        PlayerDeathHandling(
            keepCount = standardKeepCount(context.hasProtectItem),
            dropReceiver = context.player,
            dropDuration = DROP_DURATION_STANDARD,
            revealDelay = PVP_REVEAL_DELAY,
            supplyPile = false,
            untradeableHandling = UntradeableHandling.KEEP,
        )

    private fun pvpOutsideWilderness(context: PlayerDeathContext): PlayerDeathHandling =
        PlayerDeathHandling(
            keepCount = wildernessKeepCount(context.isSkulled, context.hasProtectItem),
            dropReceiver = context.killer ?: context.player,
            dropDuration = DROP_DURATION_PVP_EXTERNAL,
            revealDelay = PVP_REVEAL_DELAY,
            supplyPile = false,
            untradeableHandling = UntradeableHandling.DROP,
        )
}
