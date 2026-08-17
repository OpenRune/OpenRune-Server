package org.rsmod.content.other.consumables.food.kebab

import dev.openrune.ServerCacheManager
import jakarta.inject.Singleton
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.stat.baseHitpointsLvl
import org.rsmod.api.player.stat.stat

@Singleton
class KebabEffect {
    /**
     * Resolves all randomness before the inventory transaction.
     *
     * This method does not alter stats or send messages.
     */
    fun prepare(
        access: ProtectedAccess,
    ): KebabOutcome {
        return with(access) {
            val baseHitpoints =
                player.baseHitpointsLvl

            when (
                random.of(
                    minInclusive = 0,
                    maxInclusive = OUTCOME_ROLL_MAX,
                )
            ) {
                OUTCOME_AMAZING ->
                    KebabOutcome(
                        type = KebabOutcomeType.Amazing,
                        healAmount =
                            AMAZING_HEAL_BASE +
                                ceilPercent(
                                    value = baseHitpoints,
                                    percent =
                                        AMAZING_HEAL_PERCENT,
                                ),
                        message =
                            "Wow, that was an amazing kebab! " +
                                "You feel really invigorated.",
                        showGenericHealMessage = false,
                    )

                in OUTCOME_GOOD ->
                    KebabOutcome(
                        type = KebabOutcomeType.Good,
                        healAmount =
                            GOOD_HEAL_BASE +
                                ceilPercent(
                                    value = baseHitpoints,
                                    percent =
                                        GOOD_HEAL_PERCENT,
                                ),
                        message =
                            "That was a good kebab. " +
                                "You feel a lot better.",
                        showGenericHealMessage = false,
                    )

                in OUTCOME_NORMAL ->
                    KebabOutcome(
                        type = KebabOutcomeType.Normal,
                        healAmount =
                            NORMAL_HEAL_BASE +
                                ceilPercent(
                                    value = baseHitpoints,
                                    percent =
                                        NORMAL_HEAL_PERCENT,
                                ),

                        showGenericHealMessage = true,
                    )

                OUTCOME_NOTHING ->
                    KebabOutcome(
                        type = KebabOutcomeType.Nothing,
                        healAmount = 0,
                        message =
                            "That kebab didn't seem to do a lot.",
                        showGenericHealMessage = false,
                    )

                OUTCOME_DODGY ->
                    prepareDrainOutcome(
                        type = KebabOutcomeType.Dodgy,
                        message =
                            "That tasted a bit dodgy. " +
                                "You feel a bit ill.",
                    )

                OUTCOME_VERY_DODGY ->
                    prepareDrainOutcome(
                        type = KebabOutcomeType.VeryDodgy,
                        message =
                            "That tasted very dodgy. " +
                                "You feel very ill.",
                    )

                else ->
                    error("Invalid kebab outcome roll.")
            }
        }
    }

    /**
     * Applies stat changes only after the kebab was removed
     * successfully from the inventory.
     */
    fun apply(
        access: ProtectedAccess,
        outcome: KebabOutcome,
    ) {
        with(access) {
            when (outcome.type) {
                KebabOutcomeType.Amazing ->
                    boostMeleeStats()

                KebabOutcomeType.Dodgy ->
                    applyDodgyOutcome(outcome)

                KebabOutcomeType.VeryDodgy ->
                    applyVeryDodgyOutcome(outcome)

                KebabOutcomeType.Good,
                KebabOutcomeType.Normal,
                KebabOutcomeType.Nothing,
                    -> Unit
            }
        }
    }

    private fun ProtectedAccess.prepareDrainOutcome(
        type: KebabOutcomeType,
        message: String,
    ): KebabOutcome {
        val availableStats =
            ServerCacheManager
                .getStats()
                .values
                .asSequence()
                .map { stat -> stat.internalName }
                .filter { stat -> stat != HITPOINTS }
                .sorted()
                .toList()

        require(availableStats.isNotEmpty()) {
            "No stats are available for the kebab drain outcome."
        }

        val randomStat =
            availableStats[
                random.of(
                    minInclusive = 0,
                    maxInclusive =
                        availableStats.lastIndex,
                )
            ]

        return KebabOutcome(
            type = type,
            healAmount = 0,
            message = message,
            showGenericHealMessage = false,
            randomStat = randomStat,

             // OSRS does not apply the random drain or its follow-up
             // message when the selected stat is level 2 or lower.

            randomStatWasDrainable =
                player.stat(randomStat) > MINIMUM_DRAIN_LEVEL,
        )
    }

    private fun ProtectedAccess.boostMeleeStats() {
        MELEE_STATS.forEach { stat ->
            statBoost(
                stat = stat,
                constant = AMAZING_MELEE_BOOST,
                percent = 0,
            )
        }
    }

    private fun ProtectedAccess.applyDodgyOutcome(
        outcome: KebabOutcome,
    ) {
        val randomStat =
            outcome.randomStat
                ?: error(
                    "Dodgy kebab outcome has no random stat.",
                )

        if (!outcome.randomStatWasDrainable) {
            return
        }

        statDrain(
            stat = randomStat,
            constant = DODGY_RANDOM_DRAIN,
            percent = 0,
        )

        mes(
            "Eating the kebab has damaged your " +
                "${randomStat.displayName()} stat.",
        )
    }

    private fun ProtectedAccess.applyVeryDodgyOutcome(
        outcome: KebabOutcome,
    ) {

         // Individual melee stats at level 2 or lower are not
         // reduced.

        MELEE_STATS.forEach { stat ->
            if (player.stat(stat) > MINIMUM_DRAIN_LEVEL) {
                statDrain(
                    stat = stat,
                    constant = VERY_DODGY_MELEE_DRAIN,
                    percent = 0,
                )
            }
        }

        val randomStat =
            outcome.randomStat
                ?: error(
                    "Very-dodgy kebab outcome has no random stat.",
                )

        if (!outcome.randomStatWasDrainable) {
            return
        }

         // The random stat may also be a melee stat. In that case,
         // both drains intentionally apply.

        statDrain(
            stat = randomStat,
            constant = VERY_DODGY_RANDOM_DRAIN,
            percent = 0,
        )

        mes(
            "Eating the kebab has done damage to " +
                "some of your stats.",
        )
    }

    private fun ceilPercent(
        value: Int,
        percent: Int,
    ): Int {
        return (
            value * percent +
                PERCENT_ROUNDING_OFFSET
            ) / PERCENT_DIVISOR
    }

    private fun String.displayName(): String {
        return substringAfter(
            delimiter = "stat.",
            missingDelimiterValue = this,
        )
            .replace(
                oldChar = '_',
                newChar = ' ',
            )
            .replaceFirstChar { character ->
                character.uppercase()
            }
    }

    private companion object {
        const val OUTCOME_ROLL_MAX: Int = 31

        const val OUTCOME_AMAZING: Int = 0

        val OUTCOME_GOOD: IntRange = 1..8

        val OUTCOME_NORMAL: IntRange = 9..28

        const val OUTCOME_NOTHING: Int = 29

        const val OUTCOME_DODGY: Int = 30

        const val OUTCOME_VERY_DODGY: Int = 31

        const val AMAZING_HEAL_BASE: Int = 7

        const val AMAZING_HEAL_PERCENT: Int = 24

        const val GOOD_HEAL_BASE: Int = 6

        const val GOOD_HEAL_PERCENT: Int = 14

        const val NORMAL_HEAL_BASE: Int = 3

        const val NORMAL_HEAL_PERCENT: Int = 7

        const val AMAZING_MELEE_BOOST: Int = 2

        const val DODGY_RANDOM_DRAIN: Int = 3

        const val VERY_DODGY_MELEE_DRAIN: Int = 3

        const val VERY_DODGY_RANDOM_DRAIN: Int = 4

        const val MINIMUM_DRAIN_LEVEL: Int = 2

        const val PERCENT_DIVISOR: Int = 100

        const val PERCENT_ROUNDING_OFFSET: Int = PERCENT_DIVISOR - 1

        const val HITPOINTS: String =
            "stat.hitpoints"

        val MELEE_STATS: List<String> =
            listOf(
                "stat.attack",
                "stat.strength",
                "stat.defence",
            )
    }
}

data class KebabOutcome(
    val type: KebabOutcomeType,
    val healAmount: Int,
    val message: String? = null,
    val showGenericHealMessage: Boolean,
    val randomStat: String? = null,
    val randomStatWasDrainable: Boolean = false,
)

enum class KebabOutcomeType {
    Amazing,
    Good,
    Normal,
    Nothing,
    Dodgy,
    VeryDodgy,
}
