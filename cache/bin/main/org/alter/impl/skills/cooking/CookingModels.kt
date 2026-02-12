package org.alter.impl.skills.cooking

/**
 * Data models for cooking recipe definitions.
 */

/**
 * Defines an input ingredient for a cooking action.
 *
 * @property item RSCM key for the item (e.g., "items.raw_shrimp").
 * @property count Number of this item consumed per action.
 */
data class InputDef(
    val item: String,
    val count: Int = 1
)

/**
 * Defines a possible outcome of a cooking action.
 *
 * @property rowSuffix Unique suffix for the DB row (e.g., "success", "fail").
 * @property kind The outcome type (SUCCESS, FAIL, or ALWAYS).
 * @property item RSCM key for the produced item.
 * @property countMin Minimum quantity produced.
 * @property countMax Maximum quantity produced.
 * @property xp Experience awarded for this outcome.
 * @property weight Weighted chance for this outcome when multiple exist.
 */
data class OutcomeDef(
    val rowSuffix: String,
    val kind: Int,
    val item: String,
    val countMin: Int = 1,
    val countMax: Int = 1,
    val xp: Int = 0,
    val weight: Int = 1
)

/**
 * Defines a complete cooking action with inputs, outcomes, and requirements.
 *
 * @property rowId DB row identifier (e.g., "dbrows.cooking_shrimps").
 * @property key RSCM key used to identify this action group.
 * @property variant Variant number for multi-step recipes.
 * @property level Required Cooking level.
 * @property stopBurnFire Level at which burning stops on fires.
 * @property stopBurnRange Level at which burning stops on ranges.
 * @property stationMask Bitmask for allowed cooking stations.
 * @property trigger How the action is initiated (HEAT_SOURCE or ITEM_ON_ITEM).
 * @property inputs List of required input items.
 * @property outcomes List of possible outcomes.
 */
data class ActionDef(
    val rowId: String,
    val key: String,
    val variant: Int = CookingConstants.DEFAULT_VARIANT,
    val level: Int,
    val stopBurnFire: Int,
    val stopBurnRange: Int,
    val stationMask: Int = CookingConstants.STATION_ANY,
    val trigger: Int = CookingConstants.Trigger.HEAT_SOURCE,
    val inputs: List<InputDef>,
    val outcomes: List<OutcomeDef>
)

/**
 * Defines a preparation step for multi-step cooking (inventory item-on-item).
 *
 * @property rowKey DB row key suffix.
 * @property inputs List of (item RSCM key, count) pairs consumed.
 * @property output RSCM key for the produced item.
 * @property xp Experience awarded.
 * @property always Items always returned (e.g., empty containers).
 */
data class PrepStepDef(
    val rowKey: String,
    val inputs: List<Pair<String, Int>>,
    val output: String,
    val xp: Int = 0,
    val always: List<Pair<String, Int>> = emptyList()
)

/**
 * Defines the final heat-source step for multi-step cooking.
 *
 * @property rowKey DB row key suffix.
 * @property raw RSCM key for the uncooked item.
 * @property cooked RSCM key for the successfully cooked item.
 * @property burnt RSCM key for the burnt item.
 * @property xp Experience awarded on success.
 * @property stopBurnFire Level at which burning stops on fires.
 * @property stopBurnRange Level at which burning stops on ranges.
 * @property stationMask Bitmask for allowed cooking stations.
 * @property always Items always returned (e.g., cake tin).
 */
data class HeatStepDef(
    val rowKey: String,
    val raw: String,
    val cooked: String,
    val burnt: String,
    val xp: Int,
    val stopBurnFire: Int,
    val stopBurnRange: Int,
    val stationMask: Int = CookingConstants.STATION_ANY,
    val always: List<Pair<String, Int>> = emptyList()
)
