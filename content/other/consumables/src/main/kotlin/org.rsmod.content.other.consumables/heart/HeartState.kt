package org.rsmod.content.other.consumables.heart

import org.rsmod.api.attr.AttributeKey

internal object HeartState {
    val cooldownExpiresAt =
        AttributeKey<Int>(
            resetOnDeath = false,
            temp = true,
        )

    val saturatedEffectExpiresAt =
        AttributeKey<Int>(
            resetOnDeath = true,
            temp = true,
        )

    val saturatedActive =
        AttributeKey<Boolean>(
            persistenceKey =
                "consumables.heart.saturated_active",
        )

    val cooldownRemainingTicks =
        AttributeKey<Int>(
            persistenceKey =
                "consumables.heart.cooldown_remaining",
        )
}
