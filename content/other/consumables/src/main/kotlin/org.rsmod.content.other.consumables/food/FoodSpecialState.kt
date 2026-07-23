package org.rsmod.content.other.consumables.food

import org.rsmod.api.attr.AttributeKey

internal object FoodSpecialState {
    /**
      * OSRS keeps one pending hunter-meat heal. Eating another hunter meat
      * replaces the previous delayed heal instead of stacking both.
     */

    val pendingHunterHeal:
        AttributeKey<PendingFoodHeal> =
        AttributeKey(
            resetOnDeath = true,
            temp = true,
        )
}

internal data class PendingFoodHeal(
    val dueAt: Int,
    val amount: Int,
    val effect: DelayedFoodEffect,
)

internal enum class DelayedFoodEffect {
    NONE,
    RESTORE_RUN_ENERGY,
    CURE_POISON,
}
