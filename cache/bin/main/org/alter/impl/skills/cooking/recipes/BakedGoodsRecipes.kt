package org.alter.impl.skills.cooking.recipes

import org.alter.impl.skills.cooking.ActionDef
import org.alter.impl.skills.cooking.CookingConstants.STATION_RANGE
import org.alter.impl.skills.cooking.CookingHelpers.multiStepCook
import org.alter.impl.skills.cooking.HeatStepDef
import org.alter.impl.skills.cooking.PrepStepDef

/**
 * Baked goods recipes - pies, cakes, and other range-baked items.
 */
object BakedGoodsRecipes {

    /** Pie recipes (multi-step prep + range bake). */
    val pieRecipes: List<ActionDef> = multiStepCook(
        key = "items.uncooked_garden_pie",
        level = 34,
        prepSteps = listOf(
            PrepStepDef(
                rowKey = "cooking_garden_pie_add_tomato",
                inputs = listOf(
                    "items.pie_shell" to 1,
                    "items.tomato" to 1
                ),
                output = "items.unfinished_garden_pie_1"
            ),
            PrepStepDef(
                rowKey = "cooking_garden_pie_add_onion",
                inputs = listOf(
                    "items.unfinished_garden_pie_1" to 1,
                    "items.onion" to 1
                ),
                output = "items.unfinished_garden_pie_2"
            ),
            PrepStepDef(
                rowKey = "cooking_garden_pie_add_cabbage",
                inputs = listOf(
                    "items.unfinished_garden_pie_2" to 1,
                    "items.cabbage" to 1
                ),
                output = "items.uncooked_garden_pie"
            )
        ),
        heatStep = HeatStepDef(
            rowKey = "cooking_garden_pie_bake",
            raw = "items.uncooked_garden_pie",
            cooked = "items.garden_pie",
            burnt = "items.burnt_pie",
            xp = 138,
            stopBurnFire = 68,
            stopBurnRange = 64,
            stationMask = STATION_RANGE
        )
    )

    /** Cake recipes (multi-step prep + range bake). */
    val cakeRecipes: List<ActionDef> = multiStepCook(
        key = "items.uncooked_cake",
        level = 40,
        prepSteps = listOf(
            PrepStepDef(
                rowKey = "cooking_cake_mix",
                inputs = listOf(
                    "items.cake_tin" to 1,
                    "items.egg" to 1,
                    "items.bucket_milk" to 1,
                    "items.pot_flour" to 1
                ),
                output = "items.uncooked_cake",
                always = listOf(
                    "items.bucket_empty" to 1,
                    "items.pot_empty" to 1
                )
            )
        ),
        heatStep = HeatStepDef(
            rowKey = "cooking_cake_bake",
            raw = "items.uncooked_cake",
            cooked = "items.cake",
            burnt = "items.burnt_cake",
            xp = 180,
            stopBurnFire = 74,
            stopBurnRange = 70,
            stationMask = STATION_RANGE,
            always = listOf(
                "items.cake_tin" to 1
            )
        )
    )

    /** All baked goods recipes combined. */
    val recipes: List<ActionDef> = pieRecipes + cakeRecipes
}
