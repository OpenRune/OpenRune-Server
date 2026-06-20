package org.rsmod.content.areas.wilderness

import jakarta.inject.Inject
import org.rsmod.api.area.checker.AreaChecker
import org.rsmod.api.area.checker.wildernessLevel
import org.rsmod.api.attr.AttributeKey
import org.rsmod.api.player.hook.PlayerTeleportValidateHook
import org.rsmod.api.player.hook.WildernessTeleportType
import org.rsmod.game.entity.Player

public class WildernessTeleportHook @Inject constructor() : PlayerTeleportValidateHook {
    override fun validate(
        player: Player,
        type: WildernessTeleportType,
        areaChecker: AreaChecker,
    ): String? {
        val level = player.coords.wildernessLevel(areaChecker)
        if (level < 0) {
            return null
        }

        if (type == WildernessTeleportType.Chronicle) {
            return "The Chronicle doesn't work in the Wilderness."
        }

        if (type == WildernessTeleportType.Minigame) {
            return "You cannot teleport to a minigame from the Wilderness."
        }

        if (player.vars["varbit.teleblock_cycles"] > 0) {
            return "A teleport block has been cast on you."
        }

        if (isPvpSpecTeleportBlocked(player)) {
            return "You cannot use teleports so soon after using a special attack."
        }

        val maxLevel =
            when (type) {
                WildernessTeleportType.MemberLevel30 ->
                    if (player.members) {
                        MEMBER_MAX_WILDERNESS_LEVEL
                    } else {
                        STANDARD_MAX_WILDERNESS_LEVEL
                    }
                WildernessTeleportType.Standard -> STANDARD_MAX_WILDERNESS_LEVEL
                WildernessTeleportType.Chronicle,
                WildernessTeleportType.Minigame,
                WildernessTeleportType.Exempt,
                -> return null
            }

        if (level > maxLevel) {
            return if (maxLevel == MEMBER_MAX_WILDERNESS_LEVEL) {
                "You can't use this teleport after level 30 wilderness."
            } else {
                "You can't use this teleport after level 20 wilderness."
            }
        }

        return null
    }

    private fun isPvpSpecTeleportBlocked(player: Player): Boolean {
        val lastSpecTick = player.attr[LAST_PVP_OFFENSIVE_SPEC_TICK_ATTR] ?: return false
        return (player.currentMapClock - lastSpecTick) <= PVP_SPEC_TELEPORT_BLOCK_TICKS
    }

    public companion object {
        public val LAST_PVP_OFFENSIVE_SPEC_TICK_ATTR: AttributeKey<Int> = AttributeKey()

        private const val STANDARD_MAX_WILDERNESS_LEVEL = 20
        private const val MEMBER_MAX_WILDERNESS_LEVEL = 30
        private const val PVP_SPEC_TELEPORT_BLOCK_TICKS = 3
    }
}
