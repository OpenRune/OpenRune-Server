package org.alter.impl.skills.cooking.recipes

import org.alter.impl.skills.cooking.ActionDef
import org.alter.impl.skills.cooking.CookingConstants.STATION_RANGE
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
            burnt = "items.burnt_potato",
            level = 7,
            xp = 15,
            stopBurnFire = 40,
            stopBurnRange = 40,
            stationMask = STATION_RANGE
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
                    "items.pat_of_butter" to 1
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
        key = "items.egg_potato",
        level = 51,
        prepSteps = listOf(
            PrepStepDef(
                rowKey = "cooking_potato_add_egg",
                inputs = listOf(
                    "items.potato_butter" to 1,
                    "items.bowl_egg_scrambled" to 1
                ),
                output = "items.egg_potato",
                always = listOf("items.bowl_empty" to 1)
            )
        )
    )

    /** Mushroom potato: potato with butter + mushroom & onion. */
    val mushroomPotato: List<ActionDef> = multiStepCook(
        key = "items.mushroom_potato",
        level = 64,
        prepSteps = listOf(
            PrepStepDef(
                rowKey = "cooking_potato_add_mushroom_onion",
                inputs = listOf(
                    "items.potato_butter" to 1,
                    "items.bowl_mushroom_onion" to 1
                ),
                output = "items.mushroom_potato",
                always = listOf("items.bowl_empty" to 1)
            )
        )
    )

    /** Tuna potato: potato with butter + tuna and corn. */
    val tunaPotato: List<ActionDef> = multiStepCook(
        key = "items.tuna_potato",
        level = 68,
        prepSteps = listOf(
            PrepStepDef(
                rowKey = "cooking_potato_add_tuna_corn",
                inputs = listOf(
                    "items.potato_butter" to 1,
                    "items.bowl_tuna_corn" to 1
                ),
                output = "items.tuna_potato",
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
