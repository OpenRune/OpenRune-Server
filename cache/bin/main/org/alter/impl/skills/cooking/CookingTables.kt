package org.alter.impl.skills.cooking

import dev.openrune.definition.dbtables.dbTable
import dev.openrune.definition.util.VarType

/**
 * DB table builders for cooking skill data.
 *
 * These functions generate the cache tables used by the game client and server
 * to look up cooking recipes, inputs, and outcomes.
 */
object CookingTables {

    private const val ACTION_KEY = 0
    private const val ACTION_VARIANT = 1
    private const val ACTION_LEVEL = 2
    private const val ACTION_STOP_BURN_FIRE = 3
    private const val ACTION_STOP_BURN_RANGE = 4
    private const val ACTION_STATION_MASK = 5
    private const val ACTION_TRIGGER = 6

    /**
     * Builds the cooking_actions DB table containing action metadata.
     */
    fun actions() = dbTable("tables.cooking_actions") {
        column("key", ACTION_KEY, VarType.OBJ)
        column("variant", ACTION_VARIANT, VarType.INT)
        column("level", ACTION_LEVEL, VarType.INT)
        column("stop_burn_fire", ACTION_STOP_BURN_FIRE, VarType.INT)
        column("stop_burn_range", ACTION_STOP_BURN_RANGE, VarType.INT)
        column("station_mask", ACTION_STATION_MASK, VarType.INT)
        column("trigger", ACTION_TRIGGER, VarType.INT)

        CookingRecipeRegistry.allRecipes.forEach { action ->
            row(action.rowId) {
                columnRSCM(ACTION_KEY, action.key)
                column(ACTION_VARIANT, action.variant)
                column(ACTION_LEVEL, action.level)
                column(ACTION_STOP_BURN_FIRE, action.stopBurnFire)
                column(ACTION_STOP_BURN_RANGE, action.stopBurnRange)
                column(ACTION_STATION_MASK, action.stationMask)
                column(ACTION_TRIGGER, action.trigger)
            }
        }
    }

    private const val INPUT_KEY = 0
    private const val INPUT_VARIANT = 1
    private const val INPUT_ITEM = 2
    private const val INPUT_COUNT = 3

    /**
     * Builds the cooking_action_inputs DB table containing input requirements.
     */
    fun actionInputs() = dbTable("tables.cooking_action_inputs") {
        column("key", INPUT_KEY, VarType.OBJ)
        column("variant", INPUT_VARIANT, VarType.INT)
        column("item", INPUT_ITEM, VarType.OBJ)
        column("count", INPUT_COUNT, VarType.INT)

        CookingRecipeRegistry.allRecipes.forEach { action ->
            action.inputs.forEachIndexed { index, input ->
                row("${action.rowId}_input_$index") {
                    columnRSCM(INPUT_KEY, action.key)
                    column(INPUT_VARIANT, action.variant)
                    columnRSCM(INPUT_ITEM, input.item)
                    column(INPUT_COUNT, input.count)
                }
            }
        }
    }

    private const val OUTCOME_KEY = 0
    private const val OUTCOME_VARIANT = 1
    private const val OUTCOME_KIND = 2
    private const val OUTCOME_ITEM = 3
    private const val OUTCOME_COUNT_MIN = 4
    private const val OUTCOME_COUNT_MAX = 5
    private const val OUTCOME_XP = 6
    private const val OUTCOME_WEIGHT = 7

    /**
     * Builds the cooking_action_outcomes DB table containing possible results.
     */
    fun actionOutcomes() = dbTable("tables.cooking_action_outcomes") {
        column("key", OUTCOME_KEY, VarType.OBJ)
        column("variant", OUTCOME_VARIANT, VarType.INT)
        column("kind", OUTCOME_KIND, VarType.INT)
        column("item", OUTCOME_ITEM, VarType.OBJ)
        column("count_min", OUTCOME_COUNT_MIN, VarType.INT)
        column("count_max", OUTCOME_COUNT_MAX, VarType.INT)
        column("xp", OUTCOME_XP, VarType.INT)
        column("weight", OUTCOME_WEIGHT, VarType.INT)

        CookingRecipeRegistry.allRecipes.forEach { action ->
            action.outcomes.forEach { outcome ->
                row("${action.rowId}_outcome_${outcome.rowSuffix}") {
                    columnRSCM(OUTCOME_KEY, action.key)
                    column(OUTCOME_VARIANT, action.variant)
                    column(OUTCOME_KIND, outcome.kind)
                    columnRSCM(OUTCOME_ITEM, outcome.item)
                    column(OUTCOME_COUNT_MIN, outcome.countMin)
                    column(OUTCOME_COUNT_MAX, outcome.countMax)
                    column(OUTCOME_XP, outcome.xp)
                    column(OUTCOME_WEIGHT, outcome.weight)
                }
            }
        }
    }
}
