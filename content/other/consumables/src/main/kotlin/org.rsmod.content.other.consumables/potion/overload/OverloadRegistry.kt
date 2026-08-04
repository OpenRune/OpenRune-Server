package org.rsmod.content.other.consumables.potion.overload

import org.rsmod.content.other.consumables.potion.PotionStatBoost

internal enum class OverloadType {
    COX,
    NIGHTMARE_ZONE,
}

internal object OverloadRegistry {
    const val COX_EFFECT_TIMER: String =
        "timer.potion_cox_overload"

    const val COX_DAMAGE_TIMER: String =
        "timer.potion_cox_overload_damage"

    const val NIGHTMARE_ZONE_EFFECT_TIMER: String =
        "timer.potion_nzone_overload"

    const val NIGHTMARE_ZONE_DAMAGE_TIMER: String =
        "timer.potion_nzone_overload_damage"

    private const val DURATION: Int = 500
    private const val REFRESH_INTERVAL: Int = 25
    private const val WARNING_LEAD: Int = 17

    private const val DAMAGE_INTERVAL: Int = 2
    private const val DAMAGE_HITS: Int = 5
    private const val DAMAGE_PER_HIT: Int = 10

    private const val DAMAGE_ANIMATION: String =
        "seq.human_killerwatt_electricshock"

    private const val DAMAGE_SPOTANIM: String =
        "spotanim.skeleton_killerwatt_electricshock"

    private const val WARNING_MESSAGE: String =
        "The effects of overload will wear off in 10 seconds."

    private const val EXPIRY_MESSAGE: String =
        "The effects of the overload have worn off, " +
            "and you feel normal again."

    private val COMBAT_STATS: List<String> =
        listOf(
            "stat.attack",
            "stat.strength",
            "stat.defence",
            "stat.ranged",
            "stat.magic",
        )

    val cox: OverloadDefinition =
        definition(
            effectTimer = COX_EFFECT_TIMER,
            damageTimer = COX_DAMAGE_TIMER,
            timerVarbit =
                "varbit.raids_overload_timer",
            tierVarbit =
                "varbit.raids_overload_tier",
            startClientScript = 483,
            defaultBoost = null,
        )

    val nightmareZone: OverloadDefinition =
        definition(
            effectTimer =
                NIGHTMARE_ZONE_EFFECT_TIMER,
            damageTimer =
                NIGHTMARE_ZONE_DAMAGE_TIMER,
            timerVarbit =
                "varbit.nzone_overload_potion_effects",
            tierVarbit = null,
            startClientScript = null,
            defaultBoost =
                PotionStatBoost(
                    constant = 5,
                    percent = 15,
                ),
        )

    operator fun get(
        type: OverloadType,
    ): OverloadDefinition =
        when (type) {
            OverloadType.COX ->
                cox

            OverloadType.NIGHTMARE_ZONE ->
                nightmareZone
        }

    private fun definition(
        effectTimer: String,
        damageTimer: String,
        timerVarbit: String,
        tierVarbit: String?,
        startClientScript: Int?,
        defaultBoost: PotionStatBoost?,
    ): OverloadDefinition =
        OverloadDefinition(
            effectTimer = effectTimer,
            damageTimer = damageTimer,
            timerVarbit = timerVarbit,
            tierVarbit = tierVarbit,
            startClientScript = startClientScript,
            defaultBoost = defaultBoost,
            duration = DURATION,
            refreshInterval = REFRESH_INTERVAL,
            warningLead = WARNING_LEAD,
            damageInterval = DAMAGE_INTERVAL,
            damageHits = DAMAGE_HITS,
            damagePerHit = DAMAGE_PER_HIT,
            boostedStats = COMBAT_STATS,
            warningMessage = WARNING_MESSAGE,
            expiryMessage = EXPIRY_MESSAGE,
            animation = DAMAGE_ANIMATION,
            spotAnimation = DAMAGE_SPOTANIM,
        )
}
