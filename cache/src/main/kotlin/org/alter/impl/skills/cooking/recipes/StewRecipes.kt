package org.alter.impl.skills.cooking.recipes

import org.alter.impl.skills.cooking.ActionDef
import org.alter.impl.skills.cooking.CookingConstants.ChanceModifier.HOSIDIUS_10
import org.alter.impl.skills.cooking.CookingConstants.ChanceModifier.HOSIDIUS_5
import org.alter.impl.skills.cooking.CookingConstants.ChanceModifier.LUMBRIDGE
import org.alter.impl.skills.cooking.CookingConstants.STATION_ANY
import org.alter.impl.skills.cooking.CookingConstants.STATION_RANGE
import org.alter.impl.skills.cooking.CookingHelpers.chance
import org.alter.impl.skills.cooking.CookingHelpers.multiStepCook
import org.alter.impl.skills.cooking.HeatStepDef
import org.alter.impl.skills.cooking.PrepStepDef

/**
 * Stew recipes — bowl of water + ingredients on heat source.
 */
object StewRecipes {

    /** Basic stew: bowl of water + cooked meat + potato on fire/range. */
    val stewRecipes: List<ActionDef> = multiStepCook(
        key = "items.uncooked_stew",
        level = 25,
        prepSteps = listOf(
            PrepStepDef(
                rowKey = "cooking_stew_add_potato",
                inputs = listOf(
                    "items.bowl_water" to 1,
                    "items.potato" to 1
                ),
                output = "items.stew1"
            ),
            PrepStepDef(
                rowKey = "cooking_stew_add_meat",
                inputs = listOf(
                    "items.stew1" to 1,
                    "items.cooked_meat" to 1
                ),
                output = "items.uncooked_stew"
            )
        ),
        heatStep = HeatStepDef(
            rowKey = "cooking_stew_cook",
            raw = "items.uncooked_stew",
            cooked = "items.stew",
            burnt = "items.burnt_stew",
            xp = 117,
            stopBurnFire = 58,
            stopBurnRange = 58,
            chances = listOf(
                chance("base_any", STATION_ANY, low = 68, high = 392),
                chance("lumbridge", STATION_RANGE, modifierMask = LUMBRIDGE, low = 78, high = 412),
                chance("hosidius_5", STATION_RANGE, modifierMask = HOSIDIUS_5, low = 80, high = 404),
                chance("hosidius_10", STATION_RANGE, modifierMask = HOSIDIUS_10, low = 93, high = 417)
            )
        )
    )

    /** Curry: bowl of water + potato + cooked meat + spice on fire/range. */
    val curryRecipes: List<ActionDef> = multiStepCook(
        key = "items.uncooked_curry",
        level = 60,
        prepSteps = listOf(
            PrepStepDef(
                rowKey = "cooking_curry_add_potato",
                inputs = listOf(
                    "items.bowl_water" to 1,
                    "items.potato" to 1
                ),
                output = "items.stew1"
            ),
            PrepStepDef(
                rowKey = "cooking_curry_add_meat",
                inputs = listOf(
                    "items.stew1" to 1,
                    "items.cooked_meat" to 1
                ),
                output = "items.uncooked_stew"
            ),
            PrepStepDef(
                rowKey = "cooking_curry_add_spice",
                inputs = listOf(
                    "items.uncooked_stew" to 1,
                    "items.spicespot" to 1
                ),
                output = "items.uncooked_curry"
            )
        ),
        heatStep = HeatStepDef(
            rowKey = "cooking_curry_cook",
            raw = "items.uncooked_curry",
            cooked = "items.curry",
            burnt = "items.burnt_curry",
            xp = 280,
            stopBurnFire = 74,
            stopBurnRange = 74,
            chances = listOf(
                chance("base_any", STATION_ANY, low = 38, high = 332),
                chance("hosidius_5", STATION_RANGE, modifierMask = HOSIDIUS_5, low = 50, high = 344),
                chance("hosidius_10", STATION_RANGE, modifierMask = HOSIDIUS_10, low = 63, high = 357)
            )
        )
    )

    /** All stew recipes combined. */
    val recipes: List<ActionDef> = stewRecipes + curryRecipes
}
