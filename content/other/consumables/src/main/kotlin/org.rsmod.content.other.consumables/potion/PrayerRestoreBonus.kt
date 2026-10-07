package org.rsmod.content.other.consumables.potion

import org.rsmod.api.player.protect.ProtectedAccess

internal object PrayerRestoreBonus {
    const val EXTRA_PERCENT: Int = 2

    val WORN: Set<String> =
        setOf(
            "obj.skillcape_prayer",
            "obj.skillcape_prayer_trimmed",
            "obj.skillcape_max",
            "obj.skillcape_max_worn",
            "obj.nzone_rotg",
            "obj.sw_rotg",
            "obj.pvpa_rotg",
        )

    val CARRIED: Set<String> =
        setOf(
            "obj.deal_wrench_blessed",
            "obj.skillcape_prayer",
            "obj.skillcape_prayer_trimmed",
            "obj.skillcape_max",
            "obj.skillcape_max_worn",
        )

    fun applies(isWorn: (String) -> Boolean, isCarried: (String) -> Boolean): Boolean =
        WORN.any(isWorn) || CARRIED.any(isCarried)

    fun percent(basePercent: Int, applies: Boolean): Int =
        if (applies) basePercent + EXTRA_PERCENT else basePercent
}

internal fun ProtectedAccess.prayerRestorePercent(basePercent: Int): Int =
    PrayerRestoreBonus.percent(
        basePercent = basePercent,
        applies = PrayerRestoreBonus.applies(isWorn = { it in worn }, isCarried = { it in inv }),
    )
