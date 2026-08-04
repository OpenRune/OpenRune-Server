package org.rsmod.content.other.consumables.food

import jakarta.inject.Inject
import jakarta.inject.Singleton
import org.rsmod.api.config.constants
import org.rsmod.api.mechanics.toxins.impl.PlayerPoison
import org.rsmod.api.mechanics.toxins.impl.PlayerVenom
import org.rsmod.api.player.output.UpdateRun
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.stat.baseHitpointsLvl
import org.rsmod.api.player.stat.hitpoints
import org.rsmod.content.other.consumables.food.kebab.KebabEffect
import org.rsmod.content.other.consumables.food.kebab.KebabOutcome
import org.rsmod.content.other.consumables.food.stew.SpicyStewEffect
import org.rsmod.content.other.consumables.food.stew.SpicyStewState

@Singleton
class FoodSpecialEffectService
@Inject
constructor(
    private val spicyStew: SpicyStewEffect,
    private val kebab: KebabEffect,
) {
    internal fun handles(
        effect: String,
    ): Boolean =
        effect in SPECIAL_EFFECTS

    internal fun prepare(
        access: ProtectedAccess,
        effect: String,
        defaultHeal: Int,
        defaultCanOverheal: Boolean,
    ): FoodSpecialOutcome {
        return with(access) {
            val baseHitpoints =
                player.baseHitpointsLvl

            val defaultMaximum =
                baseHitpoints +
                    if (defaultCanOverheal) {
                        defaultHeal
                    } else {
                        0
                    }

            when (effect) {
                HADDOCK ->
                    FoodSpecialOutcome(
                        healAmount = 18,
                        maximumHitpoints =
                            baseHitpoints +
                                minOf(
                                    baseHitpoints * 20 / 100,
                                    10,
                                ),
                    )

                STRAWBERRY ->
                    normalHeal(
                        amount =
                            ceilPercent(
                                value = baseHitpoints,
                                percent = 6,
                            ),
                    )

                WATERMELON_SLICE ->
                    normalHeal(
                        amount =
                            ceilPercent(
                                value = baseHitpoints,
                                percent = 5,
                            ),
                    )

                COOKED_SWEETCORN ->
                    normalHeal(
                        amount =
                            baseHitpoints / 10 + 1,
                    )

                THIN_SNAIL_MEAT ->
                    randomHeal(
                        minimum = 5,
                        maximum = 7,
                    )

                LEAN_SNAIL_MEAT ->
                    randomHeal(
                        minimum = 5,
                        maximum = 8,
                    )

                FAT_SNAIL_MEAT ->
                    randomHeal(
                        minimum = 7,
                        maximum = 9,
                    )

                SPIDER_ON_STICK,
                SPIDER_ON_SHAFT,
                    ->
                    randomHeal(
                        minimum = 7,
                        maximum = 10,
                    )

                GIANT_FROGSPAWN ->
                    randomHeal(
                        minimum = 3,
                        maximum = 6,
                    )

                COOKED_SLIMY_EEL ->
                    randomHeal(
                        minimum = 6,
                        maximum = 10,
                    )

                CAVE_EEL ->
                    randomHeal(
                        minimum = 8,
                        maximum = 12,
                    )

                COOKED_WILD_KEBBIT ->
                    delayedHeal(
                        immediateAmount = 4,
                        delayedAmount = 4,
                    )

                COOKED_LARUPIA ->
                    delayedHeal(
                        immediateAmount = 6,
                        delayedAmount = 5,
                    )

                COOKED_BARB_TAILED_KEBBIT ->
                    delayedHeal(
                        immediateAmount = 7,
                        delayedAmount = 5,
                    )

                COOKED_GRAAHK ->
                    delayedHeal(
                        immediateAmount = 8,
                        delayedAmount = 6,
                    )

                COOKED_KYATT ->
                    delayedHeal(
                        immediateAmount = 9,
                        delayedAmount = 8,
                    )

                COOKED_FENNEC_FOX ->
                    delayedHeal(
                        immediateAmount = 11,
                        delayedAmount = 8,
                    )

                COOKED_DASHING_KEBBIT ->
                    delayedHeal(
                        immediateAmount = 13,
                        delayedAmount = 10,
                        delayedEffect =
                            DelayedFoodEffect.RESTORE_RUN_ENERGY,
                    )

                COOKED_SUNLIGHT_ANTELOPE ->
                    delayedHeal(
                        immediateAmount = 12,
                        delayedAmount = 9,
                    )

                COOKED_MOONLIGHT_ANTELOPE ->
                    delayedHeal(
                        immediateAmount = 14,
                        delayedAmount = 12,
                        delayedEffect =
                            DelayedFoodEffect.CURE_POISON,
                    )

                KEBAB -> {
                    val kebabOutcome =
                        kebab.prepare(
                            access = this,
                        )

                    FoodSpecialOutcome(
                        healAmount =
                            kebabOutcome.healAmount,
                        maximumHitpoints =
                            baseHitpoints,
                        message =
                            kebabOutcome.message,
                        showGenericHealMessage =
                            kebabOutcome.showGenericHealMessage,
                        kebabOutcome =
                            kebabOutcome,
                    )
                }

                SPICY_STEW ->
                    FoodSpecialOutcome(
                        healAmount = defaultHeal,
                        maximumHitpoints = baseHitpoints,
                        spicyStewState =
                            spicyStew.snapshot(
                                access = access,
                            ),
                    )

                else ->
                    FoodSpecialOutcome(
                        healAmount = defaultHeal,
                        maximumHitpoints = defaultMaximum,
                    )
            }
        }
    }

    internal fun applyAfterConsume(
        access: ProtectedAccess,
        effect: String,
        outcome: FoodSpecialOutcome,
    ) {
        with(access) {
            if (outcome.delayedHealAmount > 0) {
                scheduleDelayedHeal(
                    amount = outcome.delayedHealAmount,
                    delay = outcome.delayedHealDelay,
                    delayedEffect = outcome.delayedEffect,
                )
            }

            when (effect) {
                KEBAB ->
                    kebab.apply(
                        access = this,
                        outcome =
                            outcome.kebabOutcome
                                ?: error(
                                    "Kebab food outcome is missing.",
                                ),
                    )

                SPICY_STEW ->
                    spicyStew.apply(
                        access = this,
                        state =
                            outcome.spicyStewState
                                ?: SpicyStewState.EMPTY,
                    )

                HADDOCK,
                STRAWBERRY,
                WATERMELON_SLICE,
                COOKED_SWEETCORN,
                THIN_SNAIL_MEAT,
                LEAN_SNAIL_MEAT,
                FAT_SNAIL_MEAT,
                SPIDER_ON_STICK,
                SPIDER_ON_SHAFT,
                GIANT_FROGSPAWN,
                COOKED_SLIMY_EEL,
                CAVE_EEL,
                COOKED_WILD_KEBBIT,
                COOKED_LARUPIA,
                COOKED_BARB_TAILED_KEBBIT,
                COOKED_GRAAHK,
                COOKED_KYATT,
                COOKED_FENNEC_FOX,
                COOKED_DASHING_KEBBIT,
                COOKED_SUNLIGHT_ANTELOPE,
                COOKED_MOONLIGHT_ANTELOPE,
                    -> Unit

                else ->
                    error(
                        "Unhandled special food effect: '$effect'.",
                    )
            }
        }
    }

    internal fun processDelayedHeals(
        access: ProtectedAccess,
    ) {
        with(access) {
            val pending =
                player.attr[
                    FoodSpecialState.pendingHunterHeal
                ] ?: return

            if (pending.dueAt > mapClock) {
                player.timer(
                    DELAYED_HEAL_TIMER,
                    maxOf(
                        1,
                        pending.dueAt - mapClock,
                    ),
                )
                return
            }

            player.attr.remove(
                FoodSpecialState.pendingHunterHeal,
            )

            healToBase(
                amount = pending.amount,
            )

            when (pending.effect) {
                DelayedFoodEffect.NONE -> Unit

                DelayedFoodEffect.RESTORE_RUN_ENERGY ->
                    restoreRunEnergy(
                        percent = DASHING_RUN_ENERGY,
                    )

                DelayedFoodEffect.CURE_POISON ->
                    curePoisonOrReduceVenom()
            }
        }
    }

    private fun ProtectedAccess.normalHeal(
        amount: Int,
    ): FoodSpecialOutcome {
        return FoodSpecialOutcome(
            healAmount = amount,
            maximumHitpoints =
                player.baseHitpointsLvl,
        )
    }

    private fun ProtectedAccess.randomHeal(
        minimum: Int,
        maximum: Int,
    ): FoodSpecialOutcome {
        return normalHeal(
            amount =
                random.of(
                    minInclusive = minimum,
                    maxInclusive = maximum,
                ),
        )
    }

    private fun ProtectedAccess.delayedHeal(
        immediateAmount: Int,
        delayedAmount: Int,
        delayedEffect: DelayedFoodEffect =
            DelayedFoodEffect.NONE,
    ): FoodSpecialOutcome {
        return FoodSpecialOutcome(
            healAmount = immediateAmount,
            maximumHitpoints =
                player.baseHitpointsLvl,
            delayedHealAmount = delayedAmount,
            delayedHealDelay =
                DELAYED_HEAL_DELAY,
            delayedEffect = delayedEffect,
        )
    }

    private fun ProtectedAccess.scheduleDelayedHeal(
        amount: Int,
        delay: Int,
        delayedEffect: DelayedFoodEffect,
    ) {
        if (amount <= 0 || delay <= 0) {
            return
        }

        val pending =
            PendingFoodHeal(
                dueAt = mapClock + delay,
                amount = amount,
                effect = delayedEffect,
            )

         // A newly consumed hunter meat replaces the previous pending heal.
         // Ordinary food and potions never touch this state.

        player.attr[
            FoodSpecialState.pendingHunterHeal
        ] = pending

        player.clearTimer(DELAYED_HEAL_TIMER)

        player.timer(
            DELAYED_HEAL_TIMER,
            delay,
        )
    }

    private fun ProtectedAccess.healToBase(
        amount: Int,
    ) {
        if (amount <= 0) {
            return
        }

        val actual =
            (
                player.baseHitpointsLvl -
                    player.hitpoints
                ).coerceIn(
                    minimumValue = 0,
                    maximumValue = amount,
                )

        if (actual <= 0) {
            return
        }

        statAdd(
            stat = "stat.hitpoints",
            constant = actual,
            percent = 0,
        )
    }

    private fun ProtectedAccess.curePoisonOrReduceVenom() {
        if (!PlayerVenom.reduceToPoison(player)) {
            PlayerPoison.clear(player)
        }
    }

    private fun ProtectedAccess.restoreRunEnergy(
        percent: Int,
    ) {
        if (percent <= 0) {
            return
        }

        val restored =
            (
                player.runEnergy +
                    percent * RUN_ENERGY_PERCENT_SCALE
                ).coerceAtMost(
                    constants.run_max_energy,
                )

        if (restored == player.runEnergy) {
            return
        }

        player.runEnergy =
            restored

        UpdateRun.energy(
            player = player,
            energy = restored,
        )
    }

    private fun ceilPercent(
        value: Int,
        percent: Int,
    ): Int {
        return (
            value * percent +
                99
            ) / 100
    }

    companion object {
        const val DELAYED_HEAL_TIMER: String =
            "timer.food_delayed_heal"

        private const val DELAYED_HEAL_DELAY: Int = 7
        private const val DASHING_RUN_ENERGY: Int = 10
        private const val RUN_ENERGY_PERCENT_SCALE: Int = 10

        private const val HADDOCK: String =
            "haddock"

        private const val STRAWBERRY: String =
            "strawberry"

        private const val WATERMELON_SLICE: String =
            "watermelon_slice"

        private const val COOKED_SWEETCORN: String =
            "cooked_sweetcorn"

        private const val THIN_SNAIL_MEAT: String =
            "thin_snail_meat"

        private const val LEAN_SNAIL_MEAT: String =
            "lean_snail_meat"

        private const val FAT_SNAIL_MEAT: String =
            "fat_snail_meat"

        private const val SPIDER_ON_STICK: String =
            "spider_on_stick"

        private const val SPIDER_ON_SHAFT: String =
            "spider_on_shaft"

        private const val GIANT_FROGSPAWN: String =
            "giant_frogspawn"

        private const val COOKED_SLIMY_EEL: String =
            "cooked_slimy_eel"

        private const val CAVE_EEL: String =
            "cave_eel"

        private const val COOKED_WILD_KEBBIT: String =
            "cooked_wild_kebbit"

        private const val COOKED_LARUPIA: String =
            "cooked_larupia"

        private const val COOKED_BARB_TAILED_KEBBIT: String =
            "cooked_barb_tailed_kebbit"

        private const val COOKED_GRAAHK: String =
            "cooked_graahk"

        private const val COOKED_KYATT: String =
            "cooked_kyatt"

        private const val COOKED_FENNEC_FOX: String =
            "cooked_fennec_fox"

        private const val COOKED_DASHING_KEBBIT: String =
            "cooked_dashing_kebbit"

        private const val COOKED_SUNLIGHT_ANTELOPE: String =
            "cooked_sunlight_antelope"

        private const val COOKED_MOONLIGHT_ANTELOPE: String =
            "cooked_moonlight_antelope"

        private const val KEBAB: String =
            "kebab"

        private const val SPICY_STEW: String =
            "spicy_stew"

        private val SPECIAL_EFFECTS: Set<String> =
            setOf(
                HADDOCK,
                STRAWBERRY,
                WATERMELON_SLICE,
                COOKED_SWEETCORN,
                THIN_SNAIL_MEAT,
                LEAN_SNAIL_MEAT,
                FAT_SNAIL_MEAT,
                SPIDER_ON_STICK,
                SPIDER_ON_SHAFT,
                GIANT_FROGSPAWN,
                COOKED_SLIMY_EEL,
                CAVE_EEL,
                COOKED_WILD_KEBBIT,
                COOKED_LARUPIA,
                COOKED_BARB_TAILED_KEBBIT,
                COOKED_GRAAHK,
                COOKED_KYATT,
                COOKED_FENNEC_FOX,
                COOKED_DASHING_KEBBIT,
                COOKED_SUNLIGHT_ANTELOPE,
                COOKED_MOONLIGHT_ANTELOPE,
                KEBAB,
                SPICY_STEW,
            )
    }
}

internal data class FoodSpecialOutcome(
    val healAmount: Int,
    val maximumHitpoints: Int,
    val message: String? = null,
    val showGenericHealMessage: Boolean = true,
    val delayedHealAmount: Int = 0,
    val delayedHealDelay: Int = 0,
    val delayedEffect: DelayedFoodEffect =
        DelayedFoodEffect.NONE,
    val spicyStewState: SpicyStewState? = null,
    val kebabOutcome: KebabOutcome? = null,
)
