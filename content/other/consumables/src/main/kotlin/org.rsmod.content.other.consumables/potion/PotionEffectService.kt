package org.rsmod.content.other.consumables.potion

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import jakarta.inject.Inject
import jakarta.inject.Singleton
import org.rsmod.api.attr.AttributeKey
import org.rsmod.api.mechanics.toxins.ToxinImmunity
import org.rsmod.api.mechanics.toxins.impl.PlayerDisease
import org.rsmod.api.mechanics.toxins.impl.PlayerPoison
import org.rsmod.api.mechanics.toxins.impl.PlayerVenom
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.stat.StatBoostDecayPrevention
import org.rsmod.api.player.stat.clearPositiveStatBoost
import org.rsmod.api.player.stat.hitpoints
import org.rsmod.api.player.vars.VarPlayerIntMapSetter
import org.rsmod.api.table.PotionEffectRow
import org.rsmod.game.MapClock
import org.rsmod.game.entity.Player
import org.rsmod.game.entity.timerAt
import org.rsmod.game.hit.HitType

@Singleton
class PotionEffectService
@Inject
constructor(
    private val specialEffects: PotionSpecialEffectService,
    private val worldClock: MapClock,
) {
    fun supports(
        effect: PotionEffectRow,
    ): Boolean =
        effect.kind != KIND_HANDLER ||
            specialEffects.handles(effect.handler)

    fun canApply(
        access: ProtectedAccess,
        effect: PotionEffectRow,
    ): Boolean =
        with(access) {
            when (effect.kind) {
                KIND_DIVINE -> {
                    if (player.hitpoints <= effect.damage) {
                        mes("You need more Hitpoints to drink this potion.")
                        false
                    } else {
                        effect.baseEffect?.let { canApply(this, it) } ?: false
                    }
                }

                KIND_COMPOUND -> effect.effects.all { canApply(this, it) }

                KIND_HANDLER ->
                    specialEffects.canApply(
                        access = this,
                        effect = effect,
                    )

                else -> true
            }
        }

    fun apply(
        access: ProtectedAccess,
        effect: PotionEffectRow,
    ) {
        applyEffect(access, effect)
    }

    fun healMix(
        access: ProtectedAccess,
        amount: Int,
    ) {
        if (amount <= 0) {
            return
        }

        with(access) {
            restoreIfDrained(
                stat = "stat.hitpoints",
                constant = amount,
                percent = 0,
            )
        }
    }

    fun processDivineEffects(
        access: ProtectedAccess,
    ) {
        with(access) {
            val active =
                player.attr[
                    PotionBuffState.activeDivineEffects
                ] ?: run {
                    player.clearTimer(DIVINE_TIMER)
                    return
                }

            val warned =
                player.attr.getOrPut(
                    PotionBuffState.warnedDivineEffects,
                ) {
                    mutableSetOf()
                }

            var removedAny =
                false

            val iterator =
                active.iterator()

            while (iterator.hasNext()) {
                val (effectRow, expiresAt) =
                    iterator.next()

                val effect =
                    PotionEffectRow.getRow(effectRow)

                val remaining =
                    expiresAt - mapClock

                if (remaining <= 0) {
                    val affectedStats =
                        divineBoostStats(effect)

                    removeDivineDecayPrevention(
                        player = player,
                        effect = effect,
                    )

                     // Do not restore a stat from below base. Also do not
                     // clear a stat still protected by another active effect.

                    affectedStats.forEach { stat ->
                        if (
                            !StatBoostDecayPrevention.prevents(
                                player = player,
                                stat = stat,
                            )
                        ) {
                            player.clearPositiveStatBoost(stat)
                        }
                    }

                    iterator.remove()
                    warned.remove(effectRow)
                    removedAny = true

                    mes(
                        "Your ${divinePotionName(effect)} has expired.",
                    )
                    continue
                }

                if (
                    remaining <= DIVINE_WARNING_LEAD &&
                    warned.add(effectRow)
                ) {
                    mes(
                        "Your ${divinePotionName(effect)} " +
                            "is about to expire.",
                    )
                }
            }

            if (removedAny) {
                saveDivineEffects(
                    player = player,
                    active = active,
                )

                syncDivineVarbits(
                    player = player,
                    active = active,
                    restart = true,
                )
            }

            if (active.isEmpty()) {
                player.attr.remove(
                    PotionBuffState.activeDivineEffects,
                )

                player.attr.remove(
                    PotionBuffState.savedDivineTicks,
                )

                player.attr.remove(
                    PotionBuffState.warnedDivineEffects,
                )

                player.clearTimer(DIVINE_TIMER)
                return
            }

            player.clearTimer(DIVINE_TIMER)

            player.timer(
                DIVINE_TIMER,
                nextDivineTimerDelay(
                    active = active,
                    warned = warned,
                    clock = mapClock,
                ),
            )
        }
    }

    private fun divinePotionName(
        effect: PotionEffectRow,
    ): String =
        effect
            .key
            .removeSuffix("_boost")
            .replace('_', ' ')
            .plus(" potion")

    private fun collectDivineBoostStats(
        effect: PotionEffectRow,
        destination: MutableSet<String>,
    ) {
        when (effect.kind) {
            KIND_STAT_BOOST,
            KIND_FLAT_STAT_BOOST,
                -> {
                effect.skills.forEach { stat ->
                    destination += stat.internalName
                }
            }

            KIND_COMPOUND -> {
                effect.effects.forEach { nested ->
                    collectDivineBoostStats(
                        effect = nested,
                        destination = destination,
                    )
                }
            }

            KIND_DIVINE -> {
                effect.baseEffect?.let { baseEffect ->
                    collectDivineBoostStats(
                        effect = baseEffect,
                        destination = destination,
                    )
                }
            }
        }
    }

    private fun divineSource(
        effectRow: Int,
    ): String =
        "consumable.divine.$effectRow"

    private fun divineBoostStats(
        effect: PotionEffectRow,
    ): Set<String> {
        val stats =
            linkedSetOf<String>()

        collectDivineBoostStats(
            effect = effect,
            destination = stats,
        )

        return stats
    }

    private fun addDivineDecayPrevention(
        player: Player,
        effect: PotionEffectRow,
    ) {
        StatBoostDecayPrevention.add(
            player = player,
            stats = divineBoostStats(effect),
            source = divineSource(effect.rowId),
        )
    }

    private fun removeDivineDecayPrevention(
        player: Player,
        effect: PotionEffectRow,
    ) {
        StatBoostDecayPrevention.remove(
            player = player,
            stats = divineBoostStats(effect),
            source = divineSource(effect.rowId),
        )
    }

    private fun nextDivineTimerDelay(
        active: Map<Int, Int>,
        warned: Set<Int>,
        clock: Int,
    ): Int {
        var delay =
            Int.MAX_VALUE

        active.forEach { (effectRow, expiresAt) ->
            val remaining =
                (
                    expiresAt - clock
                    ).coerceAtLeast(1)

            delay =
                minOf(
                    delay,
                    remaining,
                )

            if (effectRow !in warned) {
                val untilWarning =
                    remaining -
                        DIVINE_WARNING_LEAD

                if (untilWarning > 0) {
                    delay =
                        minOf(
                            delay,
                            untilWarning,
                        )
                }
            }
        }

        return if (delay == Int.MAX_VALUE) {
            1
        } else {
            delay.coerceAtLeast(1)
        }
    }

    private fun applyEffect(
        access: ProtectedAccess,
        effect: PotionEffectRow,
    ) {
        with(access) {
            when (effect.kind) {
                KIND_STAT_BOOST ->
                    effect.skills.forEach { stat ->
                        statBoost(
                            stat = stat.internalName,
                            constant = effect.base,
                            percent = effect.percent,
                        )
                    }

                KIND_FLAT_STAT_BOOST ->
                    effect.skills.forEach { stat ->
                        statBoost(
                            stat = stat.internalName,
                            constant = effect.amount,
                            percent = 0,
                        )
                    }

                KIND_STAT_RESTORE ->
                    restoreStats(effect)

                KIND_PRAYER_RESTORE ->
                    restoreIfDrained(
                        stat = PRAYER,
                        constant = effect.base,
                        percent = effect.percent,
                    )

                KIND_PRAYER_REGENERATION ->
                    applyPrayerRegeneration(effect.duration)

                KIND_RUN_ENERGY ->
                    restoreRunEnergy(effect.amount)

                KIND_POISON_CURE -> {
                    if (!PlayerVenom.reduceToPoison(player)) {
                        PlayerPoison.clear(player)
                    }

                    ToxinImmunity.grantImmunity(
                        player = player,
                        poisonDuration =
                            effect.poisonImmunity,
                        venomDuration =
                            effect.venomImmunity,
                    )
                }

                KIND_VENOM_CURE -> {
                    PlayerVenom.clear(player)
                    PlayerPoison.clear(player)

                    ToxinImmunity.grantImmunity(
                        player = player,
                        poisonDuration =
                            effect.poisonImmunity,
                        venomDuration =
                            effect.venomImmunity,
                    )
                }

                KIND_DRAGONFIRE_PROTECTION ->
                    applyDragonfireProtection(effect)

                KIND_COMPOUND -> {
                    effect.effects.forEach { nested ->
                        applyEffect(
                            access = this,
                            effect = nested,
                        )
                    }

                    if (effect.stamina) {
                        applyStamina(effect.duration)
                    }

                    if (effect.curesDisease) {
                        PlayerDisease.clear(player)
                    }
                }

                KIND_HANDLER -> {
                    specialEffects.apply(
                        access = this,
                        effect = effect,
                    )

                    if (
                        specialEffects
                            .cancelsMaintainedBoosts(effect)
                    ) {
                        cancelDivineMaintenance(player)
                    }
                }

                KIND_DIVINE -> {
                    val baseEffect =
                        requireNotNull(effect.baseEffect) {
                            "Divine potion effect '${effect.key}' " +
                                "has no base effect."
                        }

                    applyEffect(
                        access = this,
                        effect = baseEffect,
                    )

                    takeInstantHit(
                        type = HitType.Typeless,
                        damage = effect.damage,
                    )

                    activateDivine(effect)
                }

                else ->
                    error(
                        "Unsupported potion effect kind: " +
                            "'${effect.kind}'.",
                    )
            }
        }
    }

    private fun ProtectedAccess.restoreStats(
        effect: PotionEffectRow,
    ) {
        val excluded =
            effect.excludedSkills.mapTo(
                hashSetOf(),
            ) {
                it.internalName
            }

        ServerCacheManager
            .getStats()
            .values
            .forEach { stat ->
                val internalName =
                    stat.internalName

                if (
                    internalName in excluded ||
                    internalName == "stat.hitpoints"
                ) {
                    return@forEach
                }

                if (
                    internalName == "stat.prayer" &&
                    !effect.restorePrayer
                ) {
                    return@forEach
                }

                restoreIfDrained(
                    stat = internalName,
                    constant = effect.base,
                    percent = effect.percent,
                )
            }
    }

    fun processPrayerRegeneration(
        access: ProtectedAccess,
    ) {
        with(access) {
            val remaining =
                player.attr[
                    PotionBuffState.prayerRegenerationPulses
                ] ?: run {
                    clearPrayerRegeneration()
                    return
                }

            restoreIfDrained(
                stat = PRAYER,
                constant = PRAYER_REGENERATION_AMOUNT,
                percent = 0,
            )

            val next =
                remaining - 1

            if (next <= 0) {
                clearPrayerRegeneration()

                mes(
                    "Your Prayer Regeneration potion has worn off.",
                )
                return
            }

            player.attr[
                PotionBuffState.prayerRegenerationPulses
            ] = next

            player.attr[
                PotionBuffState.prayerRegenerationNextPulseAt
            ] =
                mapClock + PRAYER_REGENERATION_INTERVAL

            VarPlayerIntMapSetter.set(
                player,
                PRAYER_REGENERATION_VARBIT,
                next,
            )

            player.timer(
                PRAYER_REGENERATION_TIMER,
                PRAYER_REGENERATION_INTERVAL,
            )
        }
    }

    private fun ProtectedAccess.applyPrayerRegeneration(
        duration: Int,
    ) {
        val pulses =
            duration / PRAYER_REGENERATION_INTERVAL

        if (pulses <= 0) {
            return
        }

        player.attr[
            PotionBuffState.prayerRegenerationPulses
        ] = pulses

        player.attr[
            PotionBuffState.prayerRegenerationNextPulseAt
        ] =
            mapClock + PRAYER_REGENERATION_INTERVAL

        VarPlayerIntMapSetter.set(
            player,
            PRAYER_REGENERATION_VARBIT,
            0,
        )

        VarPlayerIntMapSetter.set(
            player,
            PRAYER_REGENERATION_VARBIT,
            pulses,
        )

        startPotionBuff(
            PRAYER_REGENERATION_BUFF_STRUCT,
        )

        player.clearTimer(
            PRAYER_REGENERATION_TIMER,
        )

        player.timer(
            PRAYER_REGENERATION_TIMER,
            PRAYER_REGENERATION_INTERVAL,
        )
    }

    private fun ProtectedAccess.clearPrayerRegeneration() {
        player.attr.remove(
            PotionBuffState.prayerRegenerationPulses,
        )

        player.attr.remove(
            PotionBuffState.prayerRegenerationNextPulseAt,
        )

        player.attr.remove(
            PotionBuffState.savedPrayerRegenerationPulses,
        )

        player.attr.remove(
            PotionBuffState.savedPrayerRegenerationNextPulseTicks,
        )

        VarPlayerIntMapSetter.set(
            player,
            PRAYER_REGENERATION_VARBIT,
            0,
        )

        player.clearTimer(
            PRAYER_REGENERATION_TIMER,
        )
    }

    private fun ProtectedAccess.applyStamina(
        duration: Int,
    ) {
        if (duration <= 0) {
            return
        }

        val countdown =
            (
                duration +
                    STAMINA_COUNTDOWN_INTERVAL -
                    1
                ) / STAMINA_COUNTDOWN_INTERVAL

        player.attr[PotionBuffState.staminaExpiresAt] =
            mapClock + duration

        VarPlayerIntMapSetter.set(
            player,
            STAMINA_VARBIT,
            1,
        )

         // Force a restart when another dose produces the
         // same value.

        VarPlayerIntMapSetter.set(
            player,
            STAMINA_DURATION_VARBIT,
            0,
        )

        VarPlayerIntMapSetter.set(
            player,
            STAMINA_DURATION_VARBIT,
            countdown,
        )

        startPotionBuff(
            STAMINA_BUFF_STRUCT,
        )

        player.clearTimer(
            STAMINA_TIMER,
        )

        val firstDelay =
            if (duration > STAMINA_WARNING_LEAD) {
                duration - STAMINA_WARNING_LEAD
            } else {
                duration
            }

        player.timer(
            STAMINA_TIMER,
            firstDelay,
        )
    }

    fun processStamina(
        access: ProtectedAccess,
    ) {
        with(access) {
            val expiresAt =
                player.attr[PotionBuffState.staminaExpiresAt]
                    ?: run {
                        player.clearTimer(STAMINA_TIMER)
                        return
                    }

            val remaining =
                expiresAt - mapClock

            if (remaining <= 0) {
                player.attr.remove(
                    PotionBuffState.staminaExpiresAt,
                )

                VarPlayerIntMapSetter.set(
                    player,
                    STAMINA_VARBIT,
                    0,
                )

                VarPlayerIntMapSetter.set(
                    player,
                    STAMINA_DURATION_VARBIT,
                    0,
                )

                player.clearTimer(
                    STAMINA_TIMER,
                )

                mes(
                    "Your stamina enhancement has expired.",
                )
                return
            }

             // Handles a refreshed dose safely in case a callback from
             // the previous schedule reaches this method.

            if (remaining > STAMINA_WARNING_LEAD) {
                player.timer(
                    STAMINA_TIMER,
                    remaining - STAMINA_WARNING_LEAD,
                )
                return
            }

            mes(
                "Your stamina enhancement is about to expire.",
            )

            player.timer(
                STAMINA_TIMER,
                remaining,
            )
        }
    }

    fun processDragonfireProtection(
        access: ProtectedAccess,
        fullProtection: Boolean,
    ) {
        with(access) {
            val timer =
                if (fullProtection) {
                    SUPER_ANTIFIRE_TIMER
                } else {
                    ANTIFIRE_TIMER
                }

            val varbit =
                if (fullProtection) {
                    SUPER_ANTIFIRE_VARBIT
                } else {
                    ANTIFIRE_VARBIT
                }

            val potionName =
                if (fullProtection) {
                    "super antifire potion"
                } else {
                    "antifire potion"
                }

            val expiresAt =
                if (fullProtection) {
                    player.attr[
                        PotionBuffState.superAntifireExpiresAt
                    ]
                } else {
                    player.attr[
                        PotionBuffState.antifireExpiresAt
                    ]
                } ?: run {
                    player.clearTimer(timer)
                    return
                }

            val remaining =
                expiresAt - mapClock

            if (remaining <= 0) {
                if (fullProtection) {
                    player.attr.remove(
                        PotionBuffState.superAntifireExpiresAt,
                    )
                } else {
                    player.attr.remove(
                        PotionBuffState.antifireExpiresAt,
                    )
                }

                VarPlayerIntMapSetter.set(
                    player,
                    varbit,
                    0,
                )

                player.clearTimer(timer)

                mes(
                    "Your $potionName has expired.",
                )
                return
            }

            if (remaining > ANTIFIRE_WARNING_LEAD) {
                player.timer(
                    timer,
                    remaining - ANTIFIRE_WARNING_LEAD,
                )
                return
            }

            mes(
                "Your $potionName is about to expire.",
            )

            player.timer(
                timer,
                remaining,
            )
        }
    }

    private fun ProtectedAccess.applyDragonfireProtection(
        effect: PotionEffectRow,
    ) {
        if (effect.duration <= 0) {
            return
        }

        val countdownInterval =
            if (effect.fullProtection) {
                SUPER_ANTIFIRE_COUNTDOWN_INTERVAL
            } else {
                ANTIFIRE_COUNTDOWN_INTERVAL
            }

        val countdown =
            (
                effect.duration +
                    countdownInterval -
                    1
                ) / countdownInterval

        val firstTimerDelay =
            if (effect.duration > ANTIFIRE_WARNING_LEAD) {
                effect.duration - ANTIFIRE_WARNING_LEAD
            } else {
                effect.duration
            }

        if (effect.fullProtection) {
            player.attr[
                PotionBuffState.superAntifireExpiresAt
            ] =
                mapClock + effect.duration

            VarPlayerIntMapSetter.set(
                player,
                SUPER_ANTIFIRE_VARBIT,
                0,
            )

            VarPlayerIntMapSetter.set(
                player,
                SUPER_ANTIFIRE_VARBIT,
                countdown,
            )

            startPotionBuff(
                SUPER_ANTIFIRE_BUFF_STRUCT,
            )

            player.clearTimer(
                SUPER_ANTIFIRE_TIMER,
            )

            player.timer(
                SUPER_ANTIFIRE_TIMER,
                firstTimerDelay,
            )
        } else {
            player.attr[
                PotionBuffState.antifireExpiresAt
            ] =
                mapClock + effect.duration

            VarPlayerIntMapSetter.set(
                player,
                ANTIFIRE_VARBIT,
                0,
            )

            VarPlayerIntMapSetter.set(
                player,
                ANTIFIRE_VARBIT,
                countdown,
            )

            startPotionBuff(
                ANTIFIRE_BUFF_STRUCT,
            )

            player.clearTimer(
                ANTIFIRE_TIMER,
            )

            player.timer(
                ANTIFIRE_TIMER,
                firstTimerDelay,
            )
        }
    }

    private fun ProtectedAccess.activateDivine(
        effect: PotionEffectRow,
    ) {
        if (effect.duration <= 0) {
            return
        }

        val active =
            player.attr.getOrPut(
                PotionBuffState.activeDivineEffects,
            ) {
                mutableMapOf()
            }

        active[effect.rowId] =
            mapClock + effect.duration

        addDivineDecayPrevention(
            player = player,
            effect = effect,
        )

        val warned =
            player.attr.getOrPut(
                PotionBuffState.warnedDivineEffects,
            ) {
                mutableSetOf()
            }

        warned.remove(effect.rowId)

        syncDivineVarbits(
            player = player,
            active = active,
            restart = true,
        )

        saveDivineEffects(
            player = player,
            active = active,
        )

        player.clearTimer(DIVINE_TIMER)

        player.timer(
            DIVINE_TIMER,
            nextDivineTimerDelay(
                active = active,
                warned = warned,
                clock = mapClock,
            ),
        )
    }

    private fun cancelDivineMaintenance(
        player: Player,
    ) {
        val active =
            player.attr[
                PotionBuffState.activeDivineEffects
            ].orEmpty()

        val saved =
            player.attr[
                PotionBuffState.savedDivineTicks
            ]

        if (active.isEmpty() && saved == null) {
            return
        }

        active.keys.forEach { effectRow ->
            val effect =
                PotionEffectRow.getRow(effectRow)

            removeDivineDecayPrevention(
                player = player,
                effect = effect,
            )
        }

        player.attr.remove(
            PotionBuffState.activeDivineEffects,
        )

        player.attr.remove(
            PotionBuffState.savedDivineTicks,
        )

        player.attr.remove(
            PotionBuffState.warnedDivineEffects,
        )

        player.clearTimer(DIVINE_TIMER)

        syncDivineVarbits(
            player = player,
            active = emptyMap(),
            restart = false,
        )
    }

    private fun ProtectedAccess.startPotionBuff(
        struct: Int,
    ) {
        runClientScript(
            BUFF_BAR_START_CLIENTSCRIPT,
            struct,
            mapClock.coerceAtLeast(1),
        )
    }

    internal fun onLogin(
        player: Player,
    ) {
        val clock =
            worldClock.cycle

        restoreStamina(
            player = player,
            clock = clock,
        )

        restoreDragonfireProtection(
            player = player,
            fullProtection = false,
            clock = clock,
        )

        restoreDragonfireProtection(
            player = player,
            fullProtection = true,
            clock = clock,
        )

        restorePrayerRegeneration(
            player = player,
            clock = clock,
        )

        restoreDivineEffects(
            player = player,
            clock = clock,
        )
    }

    internal fun onLogout(
        player: Player,
    ) {
        snapshotStamina(player)

        snapshotDragonfireProtection(
            player = player,
            fullProtection = false,
        )

        snapshotDragonfireProtection(
            player = player,
            fullProtection = true,
        )

        snapshotPrayerRegeneration(player)

        val activeDivines =
            player.attr[
                PotionBuffState.activeDivineEffects
            ].orEmpty()

        saveDivineEffects(
            player = player,
            active = activeDivines,
        )

        syncDivineVarbits(
            player = player,
            active = activeDivines,
            restart = false,
        )
    }

    private fun restoreStamina(
        player: Player,
        clock: Int,
    ) {
        val remaining =
            player.attr.getOrDefault(
                PotionBuffState.savedStaminaTicks,
                0,
            )

        if (remaining <= 0) {
            VarPlayerIntMapSetter.set(
                player,
                STAMINA_VARBIT,
                0,
            )

            VarPlayerIntMapSetter.set(
                player,
                STAMINA_DURATION_VARBIT,
                0,
            )

            player.clearTimer(STAMINA_TIMER)
            return
        }

        player.attr[
            PotionBuffState.staminaExpiresAt
        ] =
            clock + remaining

        VarPlayerIntMapSetter.set(
            player,
            STAMINA_VARBIT,
            1,
        )

        VarPlayerIntMapSetter.set(
            player,
            STAMINA_DURATION_VARBIT,
            durationUnits(
                duration = remaining,
                interval = STAMINA_COUNTDOWN_INTERVAL,
            ),
        )

        player.scheduleRestoredTimer(
            timer = STAMINA_TIMER,
            clock = clock,
            delay =
                warningDelay(
                    remaining = remaining,
                    warningLead = STAMINA_WARNING_LEAD,
                ),
        )
    }

    private fun snapshotStamina(
        player: Player,
    ) {
        val expiresAt =
            player.attr[
                PotionBuffState.staminaExpiresAt
            ]

        val remaining =
            if (expiresAt == null) {
                0
            } else {
                (
                    expiresAt -
                        player.currentMapClock
                    ).coerceAtLeast(0)
            }

        saveRemaining(
            player = player,
            key = PotionBuffState.savedStaminaTicks,
            remaining = remaining,
        )

        VarPlayerIntMapSetter.set(
            player,
            STAMINA_VARBIT,
            if (remaining > 0) 1 else 0,
        )

        VarPlayerIntMapSetter.set(
            player,
            STAMINA_DURATION_VARBIT,
            durationUnits(
                duration = remaining,
                interval = STAMINA_COUNTDOWN_INTERVAL,
            ),
        )
    }

    private fun restoreDragonfireProtection(
        player: Player,
        fullProtection: Boolean,
        clock: Int,
    ) {
        val savedKey =
            if (fullProtection) {
                PotionBuffState.savedSuperAntifireTicks
            } else {
                PotionBuffState.savedAntifireTicks
            }

        val remaining =
            player.attr.getOrDefault(
                savedKey,
                0,
            )

        val varbit =
            if (fullProtection) {
                SUPER_ANTIFIRE_VARBIT
            } else {
                ANTIFIRE_VARBIT
            }

        val timer =
            if (fullProtection) {
                SUPER_ANTIFIRE_TIMER
            } else {
                ANTIFIRE_TIMER
            }

        if (remaining <= 0) {
            VarPlayerIntMapSetter.set(
                player,
                varbit,
                0,
            )

            player.clearTimer(timer)
            return
        }

        val interval =
            if (fullProtection) {
                SUPER_ANTIFIRE_COUNTDOWN_INTERVAL
            } else {
                ANTIFIRE_COUNTDOWN_INTERVAL
            }

        val runtimeKey =
            if (fullProtection) {
                PotionBuffState.superAntifireExpiresAt
            } else {
                PotionBuffState.antifireExpiresAt
            }

        player.attr[runtimeKey] =
            clock + remaining

        VarPlayerIntMapSetter.set(
            player,
            varbit,
            durationUnits(
                duration = remaining,
                interval = interval,
            ),
        )

        player.scheduleRestoredTimer(
            timer = timer,
            clock = clock,
            delay =
                warningDelay(
                    remaining = remaining,
                    warningLead = ANTIFIRE_WARNING_LEAD,
                ),
        )
    }

    private fun snapshotDragonfireProtection(
        player: Player,
        fullProtection: Boolean,
    ) {
        val runtimeKey =
            if (fullProtection) {
                PotionBuffState.superAntifireExpiresAt
            } else {
                PotionBuffState.antifireExpiresAt
            }

        val savedKey =
            if (fullProtection) {
                PotionBuffState.savedSuperAntifireTicks
            } else {
                PotionBuffState.savedAntifireTicks
            }

        val varbit =
            if (fullProtection) {
                SUPER_ANTIFIRE_VARBIT
            } else {
                ANTIFIRE_VARBIT
            }

        val interval =
            if (fullProtection) {
                SUPER_ANTIFIRE_COUNTDOWN_INTERVAL
            } else {
                ANTIFIRE_COUNTDOWN_INTERVAL
            }

        val expiresAt =
            player.attr[runtimeKey]

        val remaining =
            if (expiresAt == null) {
                0
            } else {
                (
                    expiresAt -
                        player.currentMapClock
                    ).coerceAtLeast(0)
            }

        saveRemaining(
            player = player,
            key = savedKey,
            remaining = remaining,
        )

        VarPlayerIntMapSetter.set(
            player,
            varbit,
            durationUnits(
                duration = remaining,
                interval = interval,
            ),
        )
    }

    private fun restorePrayerRegeneration(
        player: Player,
        clock: Int,
    ) {
        val pulses =
            player.attr.getOrDefault(
                PotionBuffState.savedPrayerRegenerationPulses,
                0,
            )

        if (pulses <= 0) {
            player.attr.remove(
                PotionBuffState.prayerRegenerationPulses,
            )

            player.attr.remove(
                PotionBuffState.prayerRegenerationNextPulseAt,
            )

            player.attr.remove(
                PotionBuffState.savedPrayerRegenerationNextPulseTicks,
            )

            VarPlayerIntMapSetter.set(
                player,
                PRAYER_REGENERATION_VARBIT,
                0,
            )

            player.clearTimer(
                PRAYER_REGENERATION_TIMER,
            )
            return
        }

        val delay =
            player.attr.getOrDefault(
                PotionBuffState.savedPrayerRegenerationNextPulseTicks,
                PRAYER_REGENERATION_INTERVAL,
            ).coerceIn(
                minimumValue = 1,
                maximumValue =
                    PRAYER_REGENERATION_INTERVAL,
            )

        player.attr[
            PotionBuffState.prayerRegenerationPulses
        ] = pulses

        player.attr[
            PotionBuffState.prayerRegenerationNextPulseAt
        ] =
            clock + delay

        VarPlayerIntMapSetter.set(
            player,
            PRAYER_REGENERATION_VARBIT,
            0,
        )

        VarPlayerIntMapSetter.set(
            player,
            PRAYER_REGENERATION_VARBIT,
            pulses,
        )

        player.clearTimer(
            PRAYER_REGENERATION_TIMER,
        )

        player.timerAt(
            timer = PRAYER_REGENERATION_TIMER,
            mapClock = clock,
            cycles = delay,
        )
    }

    private fun snapshotPrayerRegeneration(
        player: Player,
    ) {
        val pulses =
            player.attr[
                PotionBuffState.prayerRegenerationPulses
            ] ?: 0

        if (pulses <= 0) {
            player.attr.remove(
                PotionBuffState.savedPrayerRegenerationPulses,
            )

            player.attr.remove(
                PotionBuffState.savedPrayerRegenerationNextPulseTicks,
            )

            VarPlayerIntMapSetter.set(
                player,
                PRAYER_REGENERATION_VARBIT,
                0,
            )
            return
        }

        player.attr[
            PotionBuffState.savedPrayerRegenerationPulses
        ] = pulses

        val nextPulseAt =
            player.attr[
                PotionBuffState.prayerRegenerationNextPulseAt
            ] ?: (
                player.currentMapClock +
                    PRAYER_REGENERATION_INTERVAL
                )

        player.attr[
            PotionBuffState.savedPrayerRegenerationNextPulseTicks
        ] =
            (
                nextPulseAt -
                    player.currentMapClock
                ).coerceIn(
                    minimumValue = 1,
                    maximumValue =
                        PRAYER_REGENERATION_INTERVAL,
                )

        VarPlayerIntMapSetter.set(
            player,
            PRAYER_REGENERATION_VARBIT,
            pulses,
        )
    }

    private fun warningDelay(
        remaining: Int,
        warningLead: Int,
    ): Int =
        if (remaining > warningLead) {
            remaining - warningLead
        } else {
            remaining
        }.coerceAtLeast(1)

    private fun Player.scheduleRestoredTimer(
        timer: String,
        clock: Int,
        delay: Int,
    ) {
        clearTimer(timer)

        timerAt(
            timer = timer,
            mapClock = clock,
            cycles = delay.coerceAtLeast(1),
        )
    }

    private fun syncDivineVarbits(
        player: Player,
        active: Map<Int, Int>,
        restart: Boolean,
        clock: Int = player.currentMapClock,
    ) {
        val values =
            DIVINE_VARBITS.associateWith {
                0
            }.toMutableMap()

        active.forEach { (effectRow, expiresAt) ->
            val remaining =
                (
                    expiresAt -
                        clock
                    ).coerceAtLeast(0)

            if (remaining <= 0) {
                return@forEach
            }

            val effect =
                PotionEffectRow.getRow(effectRow)

            val hud =
                DIVINE_HUDS.firstOrNull {
                    it.effectKey == effect.key
                } ?: return@forEach

            hud.varbits.forEach { varbit ->
                values[varbit] =
                    maxOf(
                        values.getValue(varbit),
                        remaining,
                    )
            }
        }

        if (restart) {
            DIVINE_VARBITS.forEach { varbit ->
                VarPlayerIntMapSetter.set(
                    player,
                    varbit,
                    0,
                )
            }
        }

        values.forEach { (varbit, value) ->
            VarPlayerIntMapSetter.set(
                player,
                varbit,
                value,
            )
        }
    }

    private fun restoreDivineEffects(
        player: Player,
        clock: Int,
    ) {
        val saved =
            player.attr[
                PotionBuffState.savedDivineTicks
            ].orEmpty()

        val active =
            mutableMapOf<Int, Int>()

        saved.forEach { (effectKey, remaining) ->
            if (remaining <= 0) {
                return@forEach
            }

            val row =
                "dbrow.effect_$effectKey"
                    .asRSCM(RSCMType.DBROW)

            active[row] =
                clock + remaining
        }

        if (active.isEmpty()) {
            player.attr.remove(
                PotionBuffState.activeDivineEffects,
            )

            player.attr.remove(
                PotionBuffState.warnedDivineEffects,
            )

            player.attr.remove(
                PotionBuffState.savedDivineTicks,
            )

            syncDivineVarbits(
                player = player,
                active = active,
                restart = false,
                clock = clock,
            )

            player.clearTimer(DIVINE_TIMER)
            return
        }

        player.attr[
            PotionBuffState.activeDivineEffects
        ] = active

        val warned =
            mutableSetOf<Int>()

        player.attr[
            PotionBuffState.warnedDivineEffects
        ] = warned

        active.keys.forEach { effectRow ->
            addDivineDecayPrevention(
                player = player,
                effect =
                    PotionEffectRow.getRow(effectRow),
            )
        }

        syncDivineVarbits(
            player = player,
            active = active,
            restart = false,
            clock = clock,
        )

        player.clearTimer(DIVINE_TIMER)

        player.timerAt(
            timer = DIVINE_TIMER,
            mapClock = clock,
            cycles =
                nextDivineTimerDelay(
                    active = active,
                    warned = warned,
                    clock = clock,
                ),
        )
    }

    private fun saveDivineEffects(
        player: Player,
        active: Map<Int, Int>,
    ) {
        val saved =
            mutableMapOf<String, Int>()

        active.forEach { (effectRow, expiresAt) ->
            val remaining =
                (
                    expiresAt -
                        player.currentMapClock
                    ).coerceAtLeast(0)

            if (remaining <= 0) {
                return@forEach
            }

            val effect =
                PotionEffectRow.getRow(effectRow)

            saved[effect.key] =
                remaining
        }

        if (saved.isEmpty()) {
            player.attr.remove(
                PotionBuffState.savedDivineTicks,
            )
        } else {
            player.attr[
                PotionBuffState.savedDivineTicks
            ] = saved
        }
    }

    private data class DivineHud(
        val effectKey: String,
        val varbits: List<String>,
    )

    private fun saveRemaining(
        player: Player,
        key: AttributeKey<Int>,
        remaining: Int,
    ) {
        if (remaining > 0) {
            player.attr[key] = remaining
        } else {
            player.attr.remove(key)
        }
    }

    companion object {
        const val STAMINA_TIMER: String = "timer.potion_stamina"
        const val ANTIFIRE_TIMER: String = "timer.potion_antifire"
        const val SUPER_ANTIFIRE_TIMER: String = "timer.potion_super_antifire"
        const val DIVINE_TIMER: String = "timer.potion_divine"

        const val STAMINA_VARBIT: String = "varbit.stamina_active"
        const val ANTIFIRE_VARBIT: String = "varbit.antifire_potion"
        const val SUPER_ANTIFIRE_VARBIT: String = "varbit.super_antifire_potion"

        private const val KIND_STAT_BOOST = "stat_boost"
        private const val KIND_FLAT_STAT_BOOST = "flat_stat_boost"
        private const val KIND_STAT_RESTORE = "stat_restore"
        private const val KIND_PRAYER_RESTORE = "prayer_restore"
        private const val KIND_RUN_ENERGY = "run_energy"
        private const val KIND_POISON_CURE = "poison_cure"
        private const val KIND_VENOM_CURE = "venom_cure"
        private const val KIND_DRAGONFIRE_PROTECTION = "dragonfire_protection"
        private const val KIND_COMPOUND = "compound"
        private const val KIND_HANDLER = "handler"
        private const val KIND_DIVINE = "divine"

        private const val ANTIFIRE_COUNTDOWN_INTERVAL: Int = 30
        private const val SUPER_ANTIFIRE_COUNTDOWN_INTERVAL: Int = 20

        private const val KIND_PRAYER_REGENERATION: String =
            "prayer_regeneration"

        private const val PRAYER: String =
            "stat.prayer"

        private const val PRAYER_REGENERATION_INTERVAL: Int = 12
        private const val PRAYER_REGENERATION_AMOUNT: Int = 1

        const val PRAYER_REGENERATION_TIMER: String =
            "timer.potion_prayer_regeneration"

        const val PRAYER_REGENERATION_VARBIT: String =
            "varbit.prayer_regeneration_potion_timer"

        private const val ANTIFIRE_WARNING_LEAD: Int = 25
        private const val BUFF_BAR_START_CLIENTSCRIPT: Int = 5931

        private const val STAMINA_BUFF_STRUCT: Int = 3086
        private const val ANTIFIRE_BUFF_STRUCT: Int = 3102
        private const val SUPER_ANTIFIRE_BUFF_STRUCT: Int = 3103
        private const val PRAYER_REGENERATION_BUFF_STRUCT: Int = 1014

        const val STAMINA_DURATION_VARBIT: String =
            "varbit.stamina_duration"

        private const val STAMINA_COUNTDOWN_INTERVAL: Int = 10
        private const val STAMINA_WARNING_LEAD: Int = 25

        private const val DIVINE_WARNING_LEAD: Int = 25

        private const val DIVINE_ATTACK_VARBIT: String =
            "varbit.divineattack_potion_time"

        private const val DIVINE_STRENGTH_VARBIT: String =
            "varbit.divinestrength_potion_time"

        private const val DIVINE_DEFENCE_VARBIT: String =
            "varbit.divinedefence_potion_time"

        private const val DIVINE_RANGED_VARBIT: String =
            "varbit.divinerange_potion_time"

        private const val DIVINE_MAGIC_VARBIT: String =
            "varbit.divinemagic_potion_time"

        private const val DIVINE_COMBAT_VARBIT: String =
            "varbit.divinecombat_potion_time"

        private const val DIVINE_BASTION_VARBIT: String =
            "varbit.divinebastion_potion_time"

        private const val DIVINE_BATTLEMAGE_VARBIT: String =
            "varbit.divinebattlemage_potion_time"

        private val DIVINE_HUDS: List<DivineHud> =
            listOf(
                DivineHud(
                    effectKey =
                        "divine_super_attack_boost",
                    varbits =
                        listOf(
                            DIVINE_ATTACK_VARBIT,
                        ),
                ),
                DivineHud(
                    effectKey =
                        "divine_super_strength_boost",
                    varbits =
                        listOf(
                            DIVINE_STRENGTH_VARBIT,
                        ),
                ),
                DivineHud(
                    effectKey =
                        "divine_super_defence_boost",
                    varbits =
                        listOf(
                            DIVINE_DEFENCE_VARBIT,
                        ),
                ),
                DivineHud(
                    effectKey =
                        "divine_ranging_boost",
                    varbits =
                        listOf(
                            DIVINE_RANGED_VARBIT,
                        ),
                ),
                DivineHud(
                    effectKey =
                        "divine_magic_boost",
                    varbits =
                        listOf(
                            DIVINE_MAGIC_VARBIT,
                        ),
                ),
                DivineHud(
                    effectKey =
                        "divine_super_combat_boost",
                    varbits =
                        listOf(
                            DIVINE_ATTACK_VARBIT,
                            DIVINE_STRENGTH_VARBIT,
                            DIVINE_DEFENCE_VARBIT,
                            DIVINE_COMBAT_VARBIT,
                        ),
                ),
                DivineHud(
                    effectKey =
                        "divine_bastion_boost",
                    varbits =
                        listOf(
                            DIVINE_RANGED_VARBIT,
                            DIVINE_DEFENCE_VARBIT,
                            DIVINE_BASTION_VARBIT,
                        ),
                ),
                DivineHud(
                    effectKey =
                        "divine_battlemage_boost",
                    varbits =
                        listOf(
                            DIVINE_MAGIC_VARBIT,
                            DIVINE_DEFENCE_VARBIT,
                            DIVINE_BATTLEMAGE_VARBIT,
                        ),
                ),
            )

        private val DIVINE_VARBITS: List<String> =
            DIVINE_HUDS
                .flatMap(DivineHud::varbits)
                .distinct()
    }
}
