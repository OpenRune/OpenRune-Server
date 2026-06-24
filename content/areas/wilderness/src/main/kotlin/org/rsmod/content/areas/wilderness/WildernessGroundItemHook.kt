package org.rsmod.content.areas.wilderness

import jakarta.inject.Inject
import org.rsmod.api.area.checker.AreaChecker
import org.rsmod.api.area.checker.isInWilderness
import org.rsmod.api.death.PlayerDeathDrops
import org.rsmod.api.player.SupplyItems
import org.rsmod.api.player.hook.GroundItemDropContext
import org.rsmod.api.player.hook.GroundItemDropParams
import org.rsmod.api.player.hook.GroundItemDropSource
import org.rsmod.api.player.hook.PlayerGroundItemDropHook
import org.rsmod.api.player.hook.PlayerObjTakeValidateHook
import org.rsmod.game.entity.Player
import dev.openrune.types.ItemServerType

public class WildernessGroundItemHook @Inject constructor(private val areaChecker: AreaChecker) :
    PlayerGroundItemDropHook, PlayerObjTakeValidateHook {

    override fun adjustDrop(
        context: GroundItemDropContext,
        duration: Int,
        reveal: Int,
    ): GroundItemDropParams? {
        if (!context.coords.isInWilderness(areaChecker)) {
            return null
        }

        val type = context.type
        val isSupply = SupplyItems.isWildernessPrivateSupply(type)

        val adjustedDuration =
            when {
                context.source != GroundItemDropSource.Death &&
                    SupplyItems.isFoodOrPotion(type) -> WILDERNESS_SUPPLY_DROP_DURATION
                else -> duration
            }

        val adjustedReveal =
            when {
                isSupply -> NEVER_REVEAL
                context.source == GroundItemDropSource.Death &&
                    context.receiver != null &&
                    context.receiver !== context.player ->
                    PlayerDeathDrops.PVP_REVEAL_DELAY
                else -> IMMEDIATE_REVEAL
            }

        val ownerOnly =
            context.source == GroundItemDropSource.Death &&
                context.receiver == null &&
                isSupply

        return GroundItemDropParams(adjustedDuration, adjustedReveal, ownerOnly)
    }

    override fun validateTake(player: Player, objType: ItemServerType): String? {
        if (!player.coords.isInWilderness(areaChecker)) {
            return null
        }
        if (!SupplyItems.isFoodOrPotion(objType)) {
            return null
        }
        if (!player.recentlyInCombat(WILDERNESS_SUPPLY_PICKUP_DELAY)) {
            return null
        }
        return "You cannot take this yet."
    }

    private fun Player.recentlyInCombat(withinTicks: Int): Boolean {
        val lastCombat = maxOf(vars["varp.lastcombat"], vars["varp.lastcombat_pvp"])
        return (currentMapClock - lastCombat) < withinTicks
    }

    public companion object {
        public const val WILDERNESS_SUPPLY_DROP_DURATION: Int = 25
        public const val WILDERNESS_SUPPLY_PICKUP_DELAY: Int = 15
        private const val IMMEDIATE_REVEAL: Int = 0
        private const val NEVER_REVEAL: Int = Int.MAX_VALUE
    }
}
