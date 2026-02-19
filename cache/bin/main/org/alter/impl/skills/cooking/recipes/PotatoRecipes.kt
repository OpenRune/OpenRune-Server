package org.alter.impl.skills.cooking.recipes

import org.alter.impl.skills.cooking.ActionDef
import org.alter.impl.skills.cooking.CookingConstants.ChanceModifier.HOSIDIUS_10
import org.alter.impl.skills.cooking.CookingConstants.ChanceModifier.HOSIDIUS_5
import org.alter.impl.skills.cooking.CookingConstants.STATION_RANGE
import org.alter.impl.skills.cooking.CookingHelpers.chance
import org.alter.impl.skills.cooking.CookingHelpers.heatCook
import org.alter.impl.skills.cooking.CookingHelpers.multiStepCook
import org.alter.impl.skills.cooking.HeatStepDef
import org.alter.impl.skills.cooking.PrepStepDef

/**
 * Potato-based recipes — baked potato and toppings.
 */
object PotatoRecipes {

    /** Baked potato: raw potato on a range. */
    val bakedPotato: List<ActionDef> = listOf(
        heatCook(
            rowKey = "cooking_baked_potato",
            raw = "items.potato",
            cooked = "items.potato_baked",
            burnt = "items.potato_burnt",
            level = 7, xp = 15, stopBurnFire = 40, stopBurnRange = 40,
            stationMask = STATION_RANGE,
            chances = listOf(
                chance("range", STATION_RANGE, low = 108, high = 472),
                chance("hosidius_5", STATION_RANGE, modifierMask = HOSIDIUS_5, low = 120, high = 484),
                chance("hosidius_10", STATION_RANGE, modifierMask = HOSIDIUS_10, low = 133, high = 497)
            )
        )
    )

    /** Potato with butter: baked potato + pat of butter. */
    val potatoWithButter: List<ActionDef> = multiStepCook(
        key = "items.potato_butter",
        level = 39,
        prepSteps = listOf(
            PrepStepDef(
                rowKey = "cooking_potato_add_butter",
                inputs = listOf(
                    "items.potato_baked" to 1,
                    "items.pot_of_butter" to 1
                ),
                output = "items.potato_butter"
            )
        )
    )

    /** Potato with cheese: potato with butter + cheese. */
    val potatoWithCheese: List<ActionDef> = multiStepCook(
        key = "items.potato_cheese",
        level = 47,
        prepSteps = listOf(
            PrepStepDef(
                rowKey = "cooking_potato_add_cheese",
                inputs = listOf(
                    "items.potato_butter" to 1,
                    "items.cheese" to 1
                ),
                output = "items.potato_cheese"
            )
        )
    )

    /** Egg potato: potato with butter + scrambled egg. */
    val eggPotato: List<ActionDef> = multiStepCook(
        key = "items.potato_egg+tomato",
        level = 51,
        prepSteps = listOf(
            PrepStepDef(
                rowKey = "cooking_potato_add_egg",
                inputs = listOf(
                    "items.potato_butter" to 1,
                    "items.bowl_egg_scrambled" to 1
                ),
                output = "items.potato_egg+tomato",
                always = listOf("items.bowl_empty" to 1)
            )
        )
    )

    /** Mushroom potato: potato with butter + mushroom & onion. */
    val mushroomPotato: List<ActionDef> = multiStepCook(
        key = "items.potato_mushroom+onion",
        level = 64,
        prepSteps = listOf(
            PrepStepDef(
                rowKey = "cooking_potato_add_mushroom_onion",
                inputs = listOf(
                    "items.potato_butter" to 1,
                    "items.bowl_mushroom+onion" to 1
                ),
                output = "items.potato_mushroom+onion",
                always = listOf("items.bowl_empty" to 1)
            )
        )
    )

    /** Tuna potato: potato with butter + tuna and corn. */
    val tunaPotato: List<ActionDef> = multiStepCook(
        key = "items.potato_tuna+sweetcorn",
        level = 68,
        prepSteps = listOf(
            PrepStepDef(
                rowKey = "cooking_potato_add_tuna_corn",
                inputs = listOf(
                    "items.potato_butter" to 1,
                    "items.bowl_tuna+sweetcorn" to 1
                ),
                output = "items.potato_tuna+sweetcorn",
                always = listOf("items.bowl_empty" to 1)
            )
        )
    )

    /** All potato recipes combined. */
    val recipes: List<ActionDef> =
        bakedPotato +
            potatoWithButter +
            potatoWithCheese +
            eggPotato +
            mushroomPotato +
            tunaPotato
}
