package org.alter.impl.skills.cooking.recipes

import org.alter.impl.skills.cooking.ActionDef
import org.alter.impl.skills.cooking.CookingConstants.OutcomeKind
import org.alter.impl.skills.cooking.CookingConstants.Trigger
import org.alter.impl.skills.cooking.InputDef
import org.alter.impl.skills.cooking.OutcomeDef

/**
 * Wine recipes — fermentation mechanic.
 *
 * Wine is made by using grapes on a jug of water. The fermentation occurs
 * automatically after a delay (12 seconds / ~20 ticks). XP is awarded only
 * on success. The success/failure is determined at the time of combining,
 * not at fermentation completion.
 *
 * Since wine doesn't use a heat source, it's modeled as an ITEM_ON_ITEM
 * trigger. The actual fermentation delay is handled by CookingEvents.
 */
object WineRecipes {

    val jugOfWine: ActionDef = ActionDef(
        rowId = "dbrows.cooking_wine",
        key = "items.grapes",
        variant = 1,
        level = 35,
        stopBurnFire = 68,
        stopBurnRange = 68,
        stationMask = 0, // wine doesn't use a station; marker for event handler
        trigger = Trigger.ITEM_ON_ITEM,
        inputs = listOf(
            InputDef("items.grapes", 1),
            InputDef("items.jug_water", 1)
        ),
        outcomes = listOf(
            OutcomeDef(
                rowSuffix = "success",
                kind = OutcomeKind.SUCCESS,
                item = "items.jug_unfermented_wine",
                xp = 200,
                weight = 1
            ),
            OutcomeDef(
                rowSuffix = "fail",
                kind = OutcomeKind.FAIL,
                item = "items.jug_unfermented_wine",
                xp = 0,
                weight = 1
            )
        )
    )

    /** All wine recipes. */
    val recipes: List<ActionDef> = listOf(jugOfWine)
}
