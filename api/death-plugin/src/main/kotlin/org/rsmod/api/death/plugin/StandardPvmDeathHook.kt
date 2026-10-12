package org.rsmod.api.death.plugin

import jakarta.inject.Inject
import org.rsmod.api.death.DeathGrave
import org.rsmod.api.death.PlayerDeathContext
import org.rsmod.api.death.PlayerDeathDrops.Companion.DROP_DURATION_PVP_EXTERNAL
import org.rsmod.api.death.PlayerDeathDrops.Companion.DROP_DURATION_STANDARD
import org.rsmod.api.death.PlayerDeathDrops.Companion.PVP_REVEAL_DELAY
import org.rsmod.api.death.PlayerDeathDrops.Companion.standardKeepCount
import org.rsmod.api.death.PlayerDeathDrops.Companion.wildernessKeepCount
import org.rsmod.api.death.PlayerDeathHandling
import org.rsmod.api.death.PlayerDeathHook
import org.rsmod.api.death.UntradeableHandling
import org.rsmod.api.player.vars.boolVarBit
import org.rsmod.game.entity.Player

// Settings > Gameplay "Food/Potions can form supply piles on death": the checkbox is inverted
// (struct 2861 param settings_setting_toggle_inversed = 1), so piles form while this is 0.
private val Player.supplyPilesDisabled by boolVarBit("varbit.gravestone_supplypiles_disabled")

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
            supplyPile = !context.player.supplyPilesDisabled,
            untradeableHandling = UntradeableHandling.DROP,
            grave = DeathGrave(),
        )

    // TODO: Live puts the lost items in a grave outside the instance
    // (https://oldschool.runescape.wiki/w/Items_Kept_on_Death); each instance has to say where.
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
