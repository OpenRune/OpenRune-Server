package org.rsmod.api.player.hit

import kotlin.math.min
import org.rsmod.api.attr.AttributeKey
import org.rsmod.api.player.stat.hitpoints
import org.rsmod.game.entity.Player

public object PlayerAbsorption {
    public fun points(player: Player): Int =
        player.attr.getOrDefault(
            key = POINTS,
            default = 0,
        )

    public fun add(
        player: Player,
        amount: Int,
        maximum: Int,
    ): Int {
        require(amount >= 0) {
            "Absorption amount must not be negative."
        }
        require(maximum >= 0) {
            "Absorption maximum must not be negative."
        }

        val updated =
            (points(player) + amount)
                .coerceAtMost(maximum)

        if (updated <= 0) {
            clear(player)
        } else {
            player.attr[POINTS] = updated
        }

        return updated
    }

    /**
     * Applies absorption to damage that could actually be dealt at the
     * player's current Hitpoints.
     *
     * @return The damage that remains after absorption is consumed.
     */
    public fun absorb(
        player: Player,
        incomingDamage: Int,
    ): Int {
        if (incomingDamage <= 0) {
            return 0
        }

        val applicableDamage =
            min(
                player.hitpoints,
                incomingDamage,
            )

        if (applicableDamage <= 0) {
            return 0
        }

        val current = points(player)
        if (current <= 0) {
            return applicableDamage
        }

        val absorbed =
            min(
                current,
                applicableDamage,
            )

        val remainingPoints =
            current - absorbed

        if (remainingPoints <= 0) {
            clear(player)
        } else {
            player.attr[POINTS] = remainingPoints
        }

        return applicableDamage - absorbed
    }

    public fun clear(player: Player) {
        player.attr.remove(POINTS)
    }

    private val POINTS: AttributeKey<Int> =
        AttributeKey(
            resetOnDeath = true,
            temp = true,
        )
}
