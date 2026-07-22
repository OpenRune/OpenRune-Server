package org.rsmod.content.other.consumables.potion

import jakarta.inject.Inject
import jakarta.inject.Singleton
import org.rsmod.api.mechanics.toxins.impl.PlayerPoison
import org.rsmod.api.mechanics.toxins.impl.PlayerVenom
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.stat.hitpoints
import org.rsmod.api.player.stat.stat
import org.rsmod.api.player.stat.statBase
import org.rsmod.api.table.PotionEffectRow
import org.rsmod.content.other.consumables.heart.HeartEffectService
import org.rsmod.content.other.consumables.potion.cox.CoxPotionEffect
import org.rsmod.content.other.consumables.potion.moons.MoonlightPotionEffect
import org.rsmod.content.other.consumables.potion.nmz.NightmareZonePotionEffect
import org.rsmod.content.other.consumables.potion.toa.ToaPotionEffect
import org.rsmod.game.MapClock
import org.rsmod.game.entity.Player
import org.rsmod.game.entity.timerAt
import org.rsmod.game.hit.HitType

@Singleton
class PotionSpecialEffectService
@Inject
constructor(
    private val coxEffects: CoxPotionEffect,
    private val toaEffects: ToaPotionEffect,
    private val nightmareZoneEffects: NightmareZonePotionEffect,
    private val moonlightEffects: MoonlightPotionEffect,
    private val relicymEffects: RelicymEffect,
    private val heartEffects: HeartEffectService,
    private val worldClock: MapClock,
) {
    internal fun handles(
        handler: String,
    ): Boolean =
        coxEffects.handles(handler) ||
            toaEffects.handles(handler) ||
            nightmareZoneEffects.handles(handler) ||
            moonlightEffects.handles(handler) ||
            relicymEffects.handles(handler) ||
            handler in STANDARD_HANDLERS

    fun canApply(
        access: ProtectedAccess,
        effect: PotionEffectRow,
    ): Boolean {
        if (coxEffects.handles(effect.handler)) {
            return coxEffects.canApply(
                access = access,
                effect = effect,
            )
        }

        if (
            toaEffects.handles(effect.handler) ||
            moonlightEffects.handles(effect.handler) ||
            relicymEffects.handles(effect.handler)
        ) {
            return true
        }

        if (nightmareZoneEffects.handles(effect.handler)) {
            return nightmareZoneEffects.canApply(
                access = access,
                effect = effect,
            )
        }

        return when (effect.handler) {
            HANDLER_SARADOMIN_BREW,
            HANDLER_ARMADYL_BREW,
            HANDLER_ZAMORAK_BREW,
            HANDLER_ANCIENT_BREW,
            HANDLER_FORGOTTEN_BREW,
            HANDLER_MENAPHITE_REMEDY,
            HANDLER_GUTHIX_REST,
                -> true

            else ->
                error(
                    "Unknown potion effect handler: " +
                        "'${effect.handler}'.",
                )
        }
    }

    internal fun cancelsMaintainedBoosts(
        effect: PotionEffectRow,
    ): Boolean =
        effect.handler == HANDLER_MENAPHITE_REMEDY

    fun apply(
        access: ProtectedAccess,
        effect: PotionEffectRow,
    ) {
        if (coxEffects.handles(effect.handler)) {
            coxEffects.apply(
                access = access,
                effect = effect,
            )
            return
        }

        if (toaEffects.handles(effect.handler)) {
            toaEffects.apply(
                access = access,
                effect = effect,
            )
            return
        }

        if (nightmareZoneEffects.handles(effect.handler)) {
            nightmareZoneEffects.apply(
                access = access,
                effect = effect,
            )
            return
        }

        if (moonlightEffects.handles(effect.handler)) {
            moonlightEffects.apply(access)
            return
        }

        if (relicymEffects.handles(effect.handler)) {
            relicymEffects.apply(access)
            return
        }

        with(access) {
            when (effect.handler) {
                HANDLER_SARADOMIN_BREW ->
                    applySaradominBrew()

                HANDLER_ARMADYL_BREW ->
                    applyArmadylBrew()

                HANDLER_ZAMORAK_BREW ->
                    applyZamorakBrew()

                HANDLER_ANCIENT_BREW ->
                    applyAncientBrew(
                        magicPercent = 5,
                        magicConstant = 2,
                    )

                HANDLER_FORGOTTEN_BREW ->
                    applyAncientBrew(
                        magicPercent = 8,
                        magicConstant = 3,
                    )

                HANDLER_MENAPHITE_REMEDY ->
                    applyMenaphiteRemedy()

                HANDLER_GUTHIX_REST ->
                    applyGuthixRest()

                else ->
                    error(
                        "Unknown potion effect handler: " +
                            "'${effect.handler}'.",
                    )
            }
        }
    }

    fun processMenaphiteRemedy(
        access: ProtectedAccess,
    ) {
        with(access) {
            val expiresAt =
                player.attr[
                    PotionBuffState.menaphiteRemedyExpiresAt
                ] ?: run {
                    clearMenaphiteRemedy()
                    return
                }

            val remaining = expiresAt - mapClock

            if (remaining <= 0) {
                clearMenaphiteRemedy()
                return
            }

            restoreMenaphiteStats(
                constant = MENAPHITE_RESTORE_CONSTANT,
                percent = MENAPHITE_RESTORE_PERCENT,
            )

            player.timer(
                MENAPHITE_REMEDY_TIMER,
                minOf(
                    MENAPHITE_REMEDY_INTERVAL,
                    remaining,
                ).coerceAtLeast(1),
            )
        }
    }

    private fun ProtectedAccess.applySaradominBrew() {
        statBoost(
            stat = "stat.hitpoints",
            constant = 2,
            percent = 15,
        )

        statBoost(
            stat = "stat.defence",
            constant = 2,
            percent = 20,
        )

        drainCurrentStats(
            stats = SARADOMIN_DRAIN_STATS,
            constant = 2,
            percent = 10,
        )
    }

    private fun ProtectedAccess.applyArmadylBrew() {
        statBoost(
            stat = "stat.hitpoints",
            constant = 2,
            percent = 10,
        )

        statBoost(
            stat = "stat.ranged",
            constant = 4,
            percent = 10,
        )

        drainCurrentStats(
            stats = ARMADYL_DRAIN_STATS,
            constant = 2,
            percent = 10,
            minimum = 0,
        )
    }

    private fun ProtectedAccess.applyZamorakBrew() {
        statBoost(
            stat = "stat.attack",
            constant = 2,
            percent = 20,
        )

        statBoost(
            stat = "stat.strength",
            constant = 2,
            percent = 12,
        )

        restoreIfDrained(
            stat = "stat.prayer",
            constant = 0,
            percent = 10,
        )

        drainCurrentStat(
            stat = "stat.defence",
            constant = 2,
            percent = 10,
            minimum = 0,
        )

        val damage =
            player.hitpoints *
                ZAMORAK_HITPOINTS_PERCENT /
                100

        if (damage > 0) {
            takeInstantHit(
                type = HitType.Typeless,
                damage = damage,
            )
        }
    }

    private fun ProtectedAccess.applyAncientBrew(
        magicPercent: Int,
        magicConstant: Int,
    ) {
        statBoost(
            stat = "stat.magic",
            constant = magicConstant,
            percent = magicPercent,
        )

        applyAncientBrewPrayerBoost()

        drainCurrentStats(
            stats = ANCIENT_BREW_DRAIN_STATS,
            constant = 2,
            percent = 10,
            minimum = 0,
        )
    }

    private fun ProtectedAccess.applyAncientBrewPrayerBoost() {
        val base =
            player.statBase(PRAYER)

        val current =
            player.stat(PRAYER)

        val cap =
            base +
                base *
                ANCIENT_BREW_PRAYER_CAP_PERCENT /
                100

        if (current >= cap) {
            return
        }

        val restore =
            base *
                ANCIENT_BREW_PRAYER_RESTORE_PERCENT /
                100 +
                ANCIENT_BREW_PRAYER_RESTORE_CONSTANT

        statAdd(
            stat = PRAYER,
            constant =
                minOf(
                    restore,
                    cap - current,
                ),
            percent = 0,
        )
    }

    private fun ProtectedAccess.applyMenaphiteRemedy() {
        heartEffects.cancelMaintainedBoost(player)

        restoreMenaphiteStats(
            constant = MENAPHITE_RESTORE_CONSTANT,
            percent = MENAPHITE_RESTORE_PERCENT,
        )

        player.attr[
            PotionBuffState.menaphiteRemedyExpiresAt
        ] =
            mapClock + MENAPHITE_REMEDY_DURATION

        player.attr[
            PotionBuffState.savedMenaphiteRemedyTicks
        ] =
            MENAPHITE_REMEDY_DURATION

        player.clearTimer(
            MENAPHITE_REMEDY_TIMER,
        )

        player.timer(
            MENAPHITE_REMEDY_TIMER,
            MENAPHITE_REMEDY_INTERVAL,
        )
    }

    private fun ProtectedAccess.restoreMenaphiteStats(
        constant: Int,
        percent: Int,
    ) {
        MENAPHITE_REMEDY_STATS.forEach { stat ->
            restoreIfDrained(
                stat = stat,
                constant = constant,
                percent = percent,
            )
        }
    }

    private fun ProtectedAccess.clearMenaphiteRemedy() {
        player.attr.remove(
            PotionBuffState.menaphiteRemedyExpiresAt,
        )

        player.attr.remove(
            PotionBuffState.savedMenaphiteRemedyTicks,
        )

        player.clearTimer(
            MENAPHITE_REMEDY_TIMER,
        )
    }

    private fun ProtectedAccess.applyGuthixRest() {
        statBoost(
            stat = "stat.hitpoints",
            constant = GUTHIX_REST_HEAL,
            percent = 0,
        )

        restoreRunEnergy(
            percent = GUTHIX_REST_RUN_ENERGY,
        )

         // Venom conversion takes priority. The newly created poison
         // should not also be weakened by this same dose.

        if (
            PlayerVenom.reduceToPoison(
                player = player,
                initialPoisonDamage =
                    GUTHIX_REST_CONVERTED_POISON_DAMAGE,
            )
        ) {
            return
        }

        PlayerPoison.reduceSeverity(
            player = player,
            amount =
                GUTHIX_REST_POISON_SEVERITY_REDUCTION,
        )
    }

    internal fun onLogin(
        player: Player,
    ) {
        val remaining =
            player.attr.getOrDefault(
                PotionBuffState.savedMenaphiteRemedyTicks,
                0,
            )

        if (remaining <= 0) {
            player.attr.remove(
                PotionBuffState.menaphiteRemedyExpiresAt,
            )

            player.clearTimer(
                MENAPHITE_REMEDY_TIMER,
            )
            return
        }

        val clock = worldClock.cycle

        player.attr[
            PotionBuffState.menaphiteRemedyExpiresAt
        ] =
            clock + remaining

        player.clearTimer(
            MENAPHITE_REMEDY_TIMER,
        )

        player.timerAt(
            timer = MENAPHITE_REMEDY_TIMER,
            mapClock = clock,
            cycles =
                minOf(
                    MENAPHITE_REMEDY_INTERVAL,
                    remaining,
                ).coerceAtLeast(1),
        )
    }

    internal fun onLogout(
        player: Player,
    ) {
        val expiresAt =
            player.attr[
                PotionBuffState.menaphiteRemedyExpiresAt
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

        if (remaining > 0) {
            player.attr[
                PotionBuffState.savedMenaphiteRemedyTicks
            ] = remaining
        } else {
            player.attr.remove(
                PotionBuffState.savedMenaphiteRemedyTicks,
            )
        }
    }

    companion object {
        const val MENAPHITE_REMEDY_TIMER: String =
            "timer.potion_menaphite_remedy"

        private const val MENAPHITE_REMEDY_INTERVAL: Int = 25
        private const val MENAPHITE_REMEDY_DURATION: Int = 500

        private const val MENAPHITE_RESTORE_CONSTANT: Int = 6
        private const val MENAPHITE_RESTORE_PERCENT: Int = 16

        private const val GUTHIX_REST_HEAL: Int = 5
        private const val GUTHIX_REST_RUN_ENERGY: Int = 5
        private const val GUTHIX_REST_CONVERTED_POISON_DAMAGE: Int = 6
        private const val GUTHIX_REST_POISON_SEVERITY_REDUCTION: Int = 5

        private const val ZAMORAK_HITPOINTS_PERCENT: Int = 12

        private const val PRAYER: String =
            "stat.prayer"

        private const val ANCIENT_BREW_PRAYER_RESTORE_PERCENT: Int = 10
        private const val ANCIENT_BREW_PRAYER_RESTORE_CONSTANT: Int = 2
        private const val ANCIENT_BREW_PRAYER_CAP_PERCENT: Int = 5

        private const val HANDLER_SARADOMIN_BREW: String =
            "saradomin_brew"

        private const val HANDLER_ZAMORAK_BREW: String =
            "zamorak_brew"

        private const val HANDLER_ARMADYL_BREW: String =
            "armadyl_brew"

        private const val HANDLER_ANCIENT_BREW: String =
            "ancient_brew"

        private const val HANDLER_FORGOTTEN_BREW: String =
            "forgotten_brew"

        private const val HANDLER_MENAPHITE_REMEDY: String =
            "menaphite_remedy"

        private const val HANDLER_GUTHIX_REST: String =
            "guthix_rest"

        private val STANDARD_HANDLERS: Set<String> =
            setOf(
                HANDLER_SARADOMIN_BREW,
                HANDLER_ARMADYL_BREW,
                HANDLER_ZAMORAK_BREW,
                HANDLER_ANCIENT_BREW,
                HANDLER_FORGOTTEN_BREW,
                HANDLER_MENAPHITE_REMEDY,
                HANDLER_GUTHIX_REST,
            )

        private val SARADOMIN_DRAIN_STATS: List<String> =
            listOf(
                "stat.attack",
                "stat.strength",
                "stat.ranged",
                "stat.magic",
            )

        private val ARMADYL_DRAIN_STATS: List<String> =
            listOf(
                "stat.attack",
                "stat.strength",
                "stat.defence",
                "stat.magic",
            )

        private val ANCIENT_BREW_DRAIN_STATS: List<String> =
            listOf(
                "stat.attack",
                "stat.strength",
                "stat.defence",
            )

        private val MENAPHITE_REMEDY_STATS: List<String> =
            listOf(
                "stat.attack",
                "stat.strength",
                "stat.defence",
                "stat.ranged",
                "stat.magic",
            )
    }
}
