package org.rsmod.api.specials.energy

import org.rsmod.api.attr.AttributeKey
import org.rsmod.game.entity.Player

public object SpecialAttackEnergyModifier {
    public fun setCostDivisor(
        player: Player,
        divisor: Int,
    ) {
        require(divisor >= 1) {
            "Special attack cost divisor must be at least 1."
        }

        player.attr[COST_DIVISOR] = divisor
    }

    public fun adjustedCost(
        player: Player,
        baseCost: Int,
    ): Int {
        if (baseCost < SPECIALIZED_REQUIREMENT_THRESHOLD) {
            return baseCost
        }

        val divisor =
            player.attr.getOrDefault(
                key = COST_DIVISOR,
                default = 1,
            )

        return ceilDiv(
            value = baseCost,
            divisor = divisor,
        )
    }

    public fun isActive(player: Player): Boolean =
        player.attr.getOrDefault(
            key = COST_DIVISOR,
            default = 1,
        ) > 1

    public fun clear(player: Player) {
        player.attr.remove(COST_DIVISOR)
    }

    private fun ceilDiv(
        value: Int,
        divisor: Int,
    ): Int =
        (value + divisor - 1) /
            divisor

    private val COST_DIVISOR: AttributeKey<Int> =
        AttributeKey(
            resetOnDeath = true,
            temp = true,
        )

    private const val SPECIALIZED_REQUIREMENT_THRESHOLD: Int =
        10
}
