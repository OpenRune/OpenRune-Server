package org.rsmod.content.other.consumables

import org.rsmod.api.attr.AttributeKey
import org.rsmod.api.player.protect.ProtectedAccess

/**
 * Shared consume locks for food, potions and combo food.
 *
 * A potion blocks another potion and subsequent regular food, while combo
 * food may follow either regular food or a potion. Once combo food is eaten,
 * it blocks both remaining categories until its own consume delay expires.
 */
object ConsumableDelayState {
    private val foodUntil =
        AttributeKey<Int>(
            resetOnDeath = true,
            temp = true,
        )

    private val potionUntil =
        AttributeKey<Int>(
            resetOnDeath = true,
            temp = true,
        )

    private val comboFoodUntil =
        AttributeKey<Int>(
            resetOnDeath = true,
            temp = true,
        )

    /**
     * Tracks only attack delay contributed by consumables.
     *
     * Keeping this separate from ProtectedAccess.actionDelay prevents food
     * from incorrectly adding its full delay on top of a longer weapon or
     * interaction delay that was already active.
     */
    private val combatUntil =
        AttributeKey<Int>(
            resetOnDeath = true,
            temp = true,
        )

    fun canConsume(
        access: ProtectedAccess,
        type: ConsumableType,
    ): Boolean {
        return with(access) {
            when (type) {
                ConsumableType.FOOD ->
                    !isActive(foodUntil) &&
                        !isActive(potionUntil) &&
                        !isActive(comboFoodUntil)

                ConsumableType.POTION ->
                    !isActive(potionUntil) &&
                        !isActive(comboFoodUntil)

                ConsumableType.COMBO_FOOD ->
                    !isActive(comboFoodUntil)
            }
        }
    }

    /**
     * Records a successful consumption.
     *
     * Combat delays are additive. Example: regular food with a three-tick
     * delay followed by a karambwan with a two-tick delay produces a five-tick
     * attack lock. Potions normally configure a zero combat delay and therefore
     * do not extend that lock.
     */
    fun recordConsumption(
        access: ProtectedAccess,
        type: ConsumableType,
        consumeDelay: Int,
        combatDelay: Int,
    ) {
        with(access) {
            if (consumeDelay > 0) {
                val key = key(type)
                val expiresAt = mapClock + consumeDelay
                val current =
                    player.attr.getOrDefault(
                        key = key,
                        default = -1,
                    )

                if (expiresAt > current) {
                    player.attr[key] = expiresAt
                }
            }

            if (combatDelay > 0) {
                val currentConsumableDelay =
                    player.attr.getOrDefault(
                        key = combatUntil,
                        default = mapClock,
                    )

                val expiresAt =
                    maxOf(
                        currentConsumableDelay,
                        mapClock,
                    ) + combatDelay

                player.attr[combatUntil] = expiresAt

                actionDelay =
                    maxOf(
                        actionDelay,
                        expiresAt,
                    )
            }
        }
    }

    private fun ProtectedAccess.isActive(
        key: AttributeKey<Int>,
    ): Boolean {
        return player.attr.getOrDefault(
            key = key,
            default = -1,
        ) > mapClock
    }

    private fun key(
        type: ConsumableType,
    ): AttributeKey<Int> {
        return when (type) {
            ConsumableType.FOOD -> foodUntil
            ConsumableType.POTION -> potionUntil
            ConsumableType.COMBO_FOOD -> comboFoodUntil
        }
    }
}

enum class ConsumableType {
    FOOD,
    POTION,
    COMBO_FOOD,
}
