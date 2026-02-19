package org.alter.impl.skills.cooking

import org.alter.impl.skills.cooking.CookingConstants.DEFAULT_VARIANT
import org.alter.impl.skills.cooking.CookingConstants.OutcomeKind
import org.alter.impl.skills.cooking.CookingConstants.STATION_ANY
import org.alter.impl.skills.cooking.CookingConstants.STATION_FIRE
import org.alter.impl.skills.cooking.CookingConstants.Trigger

/**
 * Helper functions for building cooking recipe definitions.
 */
object CookingHelpers {

    /**
     * Creates a [ChanceDef] for burn chance profile definitions.
     *
     * @param label Human-readable label (e.g., "base_any", "gauntlets", "hosidius_5").
     * @param stationMask Bitmask of stations this profile applies to.
     * @param modifierMask Bitmask of required modifiers (default: none).
     * @param low The low value for the statrandom calculation.
     * @param high The high value for the statrandom calculation.
     */
    fun chance(
        label: String,
        stationMask: Int,
        modifierMask: Int = 0,
        low: Int,
        high: Int
    ): ChanceDef = ChanceDef(label, stationMask, modifierMask, low, high)

    /**
     * Creates a simple heat-cooking action (raw -> cooked/burnt).
     */
    fun heatCook(
        rowKey: String,
        raw: String,
        cooked: String,
        burnt: String,
        level: Int,
        xp: Int,
        stopBurnFire: Int = 0,
        stopBurnRange: Int = 0,
        stationMask: Int = STATION_ANY,
        chances: List<ChanceDef> = emptyList()
    ): ActionDef = ActionDef(
        rowId = "dbrows.$rowKey",
        key = raw,
        level = level,
        stopBurnFire = stopBurnFire,
        stopBurnRange = stopBurnRange,
        stationMask = stationMask,
        trigger = Trigger.HEAT_SOURCE,
        inputs = listOf(InputDef(raw, 1)),
        outcomes = listOf(
            OutcomeDef(rowSuffix = "success", kind = OutcomeKind.SUCCESS, item = cooked, xp = xp, weight = 1),
            OutcomeDef(rowSuffix = "fail", kind = OutcomeKind.FAIL, item = burnt, xp = 0, weight = 1)
        ),
        chances = chances
    )

    /**
     * Creates a spit-roasting recipe (2 actions: skewer + roast).
     *
     * @param skewerRowKey DB row key for the skewering step.
     * @param roastRowKey DB row key for the roasting step.
     * @param rawMeat RSCM key for the raw meat.
     * @param skewered RSCM key for the skewered meat.
     * @param cooked RSCM key for the roasted meat.
     * @param burnt RSCM key for the burnt meat.
     * @param cookingLevel Required Cooking level for roasting.
     * @param xp Experience awarded on successful roast.
     * @param stopBurnFire Level at which burning stops on fires.
     * @param stopBurnRange Level at which burning stops on ranges.
     * @param spitItem RSCM key for the spit item (default: iron spit).
     */
    fun spitRoast(
        skewerRowKey: String,
        roastRowKey: String,
        rawMeat: String,
        skewered: String,
        cooked: String,
        burnt: String,
        cookingLevel: Int,
        xp: Int,
        stopBurnFire: Int = 0,
        stopBurnRange: Int = 0,
        spitItem: String = "items.spit_iron",
        chances: List<ChanceDef> = emptyList()
    ): List<ActionDef> = listOf(
        // Raw meat + iron spit -> skewered meat (inventory prep step)
        ActionDef(
            rowId = "dbrows.$skewerRowKey",
            key = skewered,
            variant = 1,
            level = 1,
            stopBurnFire = 1,
            stopBurnRange = 1,
            stationMask = STATION_ANY,
            trigger = Trigger.ITEM_ON_ITEM,
            inputs = listOf(
                InputDef(rawMeat, 1),
                InputDef(spitItem, 1)
            ),
            outcomes = listOf(
                OutcomeDef(rowSuffix = "success", kind = OutcomeKind.SUCCESS, item = skewered, xp = 0, weight = 1)
            )
        ),

        // Skewered meat -> roasted/burnt (fire only)
        ActionDef(
            rowId = "dbrows.$roastRowKey",
            key = skewered,
            variant = DEFAULT_VARIANT,
            level = cookingLevel,
            stopBurnFire = stopBurnFire,
            stopBurnRange = stopBurnRange,
            stationMask = STATION_FIRE,
            trigger = Trigger.HEAT_SOURCE,
            inputs = listOf(InputDef(skewered, 1)),
            outcomes = listOf(
                OutcomeDef(rowSuffix = "success", kind = OutcomeKind.SUCCESS, item = cooked, xp = xp, weight = 1),
                OutcomeDef(rowSuffix = "fail", kind = OutcomeKind.FAIL, item = burnt, xp = 0, weight = 1)
            ),
            chances = chances
        )
    )

    /**
     * Creates a multi-step cooking recipe with preparation and optional baking.
     *
     * @param key RSCM key identifying this recipe group.
     * @param level Required Cooking level.
     * @param prepSteps List of inventory preparation steps.
     * @param heatStep Optional final heat-source cooking step.
     * @param prepVariantStart Starting variant number for prep steps.
     * @param heatVariant Variant number for the heat step.
     */
    fun multiStepCook(
        key: String,
        level: Int,
        prepSteps: List<PrepStepDef>,
        heatStep: HeatStepDef? = null,
        prepVariantStart: Int = 1,
        heatVariant: Int = DEFAULT_VARIANT
    ): List<ActionDef> = buildList {
        prepSteps.forEachIndexed { index, step ->
            val alwaysOutcomes = step.always.mapIndexed { alwaysIndex, (item, count) ->
                OutcomeDef(
                    rowSuffix = "always_$alwaysIndex",
                    kind = OutcomeKind.ALWAYS,
                    item = item,
                    countMin = count,
                    countMax = count,
                    xp = 0,
                    weight = 1
                )
            }

            add(
                ActionDef(
                    rowId = "dbrows.${step.rowKey}",
                    key = key,
                    variant = prepVariantStart + index,
                    level = level,
                    stopBurnFire = level,
                    stopBurnRange = level,
                    stationMask = STATION_ANY,
                    trigger = Trigger.ITEM_ON_ITEM,
                    inputs = step.inputs.map { (item, count) -> InputDef(item, count) },
                    outcomes = listOf(
                        OutcomeDef(
                            rowSuffix = "success",
                            kind = OutcomeKind.SUCCESS,
                            item = step.output,
                            xp = step.xp,
                            weight = 1
                        )
                    ) + alwaysOutcomes
                )
            )
        }

        heatStep?.let { step ->
            val alwaysOutcomes = step.always.mapIndexed { alwaysIndex, (item, count) ->
                OutcomeDef(
                    rowSuffix = "always_$alwaysIndex",
                    kind = OutcomeKind.ALWAYS,
                    item = item,
                    countMin = count,
                    countMax = count,
                    xp = 0,
                    weight = 1
                )
            }

            add(
                ActionDef(
                    rowId = "dbrows.${step.rowKey}",
                    key = key,
                    variant = heatVariant,
                    level = level,
                    stopBurnFire = step.stopBurnFire,
                    stopBurnRange = step.stopBurnRange,
                    stationMask = step.stationMask,
                    trigger = Trigger.HEAT_SOURCE,
                    inputs = listOf(InputDef(step.raw, 1)),
                    outcomes = listOf(
                        OutcomeDef(
                            rowSuffix = "success",
                            kind = OutcomeKind.SUCCESS,
                            item = step.cooked,
                            xp = step.xp,
                            weight = 1
                        ),
                        OutcomeDef(
                            rowSuffix = "fail",
                            kind = OutcomeKind.FAIL,
                            item = step.burnt,
                            xp = 0,
                            weight = 1
                        )
                    ) + alwaysOutcomes,
                    chances = step.chances
                )
            )
        }
    }
}
