package org.rsmod.content.other.consumables.potion

import org.rsmod.api.attr.AttributeKey

internal object PotionBuffState {
    val activeDivineEffects =
        AttributeKey<MutableMap<Int, Int>>(
            resetOnDeath = true,
            temp = true,
        )

    val warnedDivineEffects =
        AttributeKey<MutableSet<Int>>(
            resetOnDeath = true,
            temp = true,
        )

    val staminaExpiresAt =
        AttributeKey<Int>(
            resetOnDeath = true,
            temp = true,
        )

    val antifireExpiresAt =
        AttributeKey<Int>(
            resetOnDeath = true,
            temp = true,
        )

    val superAntifireExpiresAt =
        AttributeKey<Int>(
            resetOnDeath = true,
            temp = true,
        )

    val prayerRegenerationPulses =
        AttributeKey<Int>(
            resetOnDeath = true,
            temp = true,
        )

    val prayerRegenerationNextPulseAt =
        AttributeKey<Int>(
            resetOnDeath = true,
            temp = true,
        )

    val menaphiteRemedyExpiresAt =
        AttributeKey<Int>(
            resetOnDeath = true,
            temp = true,
        )

    val savedStaminaTicks =
        AttributeKey<Int>(
            persistenceKey =
                "consumables.potion.stamina_remaining",
        )

    val savedAntifireTicks =
        AttributeKey<Int>(
            persistenceKey =
                "consumables.potion.antifire_remaining",
        )

    val savedSuperAntifireTicks =
        AttributeKey<Int>(
            persistenceKey =
                "consumables.potion.super_antifire_remaining",
        )

    val savedPrayerRegenerationPulses =
        AttributeKey<Int>(
            persistenceKey =
                "consumables.potion.prayer_regeneration_pulses",
        )

    val savedPrayerRegenerationNextPulseTicks =
        AttributeKey<Int>(
            persistenceKey =
                "consumables.potion.prayer_regeneration_next_pulse",
        )

    val savedDivineTicks =
        AttributeKey<MutableMap<String, Int>>(
            persistenceKey =
                "consumables.potion.divine_remaining",
        )

    val savedMenaphiteRemedyTicks =
        AttributeKey<Int>(
            persistenceKey =
                "consumables.potion.menaphite_remedy_remaining",
        )
}
