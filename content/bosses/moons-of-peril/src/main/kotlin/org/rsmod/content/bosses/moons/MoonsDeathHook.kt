package org.rsmod.content.bosses.moons

import jakarta.inject.Inject
import org.rsmod.api.death.DeathGrave
import org.rsmod.api.death.PlayerDeathContext
import org.rsmod.api.death.PlayerDeathDrops.Companion.DROP_DURATION_STANDARD
import org.rsmod.api.death.PlayerDeathDrops.Companion.PVP_REVEAL_DELAY
import org.rsmod.api.death.PlayerDeathDrops.Companion.standardKeepCount
import org.rsmod.api.death.PlayerDeathHandling
import org.rsmod.api.death.PlayerDeathHook
import org.rsmod.api.death.UntradeableHandling

class MoonsDeathHook @Inject constructor(private val fights: MoonsFights) : PlayerDeathHook {
    override val priority: Int = 10

    override fun handleDeath(context: PlayerDeathContext): PlayerDeathHandling? {
        val player = context.player
        if (context.recentPvpDamage || fights.of(player) == null) return null
        return PlayerDeathHandling(
            keepCount = standardKeepCount(context.hasProtectItem),
            dropReceiver = player,
            dropDuration = DROP_DURATION_STANDARD,
            revealDelay = PVP_REVEAL_DELAY,
            supplyPile = player.vars["varbit.gravestone_supplypiles_disabled"] == 0,
            untradeableHandling = UntradeableHandling.DROP,
            grave = DeathGrave(coords = Moon.ANTECHAMBER),
        )
    }
}
