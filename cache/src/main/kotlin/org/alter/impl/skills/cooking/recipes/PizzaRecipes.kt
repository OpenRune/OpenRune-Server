package org.alter.impl.skills.cooking.recipes

import org.alter.impl.skills.cooking.ActionDef
import org.alter.impl.skills.cooking.CookingConstants.ChanceModifier.HOSIDIUS_10
import org.alter.impl.skills.cooking.CookingConstants.ChanceModifier.HOSIDIUS_5
import org.alter.impl.skills.cooking.CookingConstants.STATION_RANGE
import org.alter.impl.skills.cooking.CookingHelpers.chance
import org.alter.impl.skills.cooking.CookingHelpers.multiStepCook
import org.alter.impl.skills.cooking.HeatStepDef
import org.alter.impl.skills.cooking.PrepStepDef

/**
 * Pizza recipes — dough + toppings + range bake.
 *
 * Flow: pizza base → uncooked pizza (item-on-item) → plain pizza (bake) → topped pizza (item-on-item)
 */
object PizzaRecipes {

    /** Plain pizza: pizza base on range. */
    val plainPizzaRecipes: List<ActionDef> = multiStepCook(
        key = "items.uncooked_pizza",
        level = 35,
        prepSteps = listOf(
            PrepStepDef(
                rowKey = "cooking_pizza_add_tomato",
                inputs = listOf(
                    "items.pizza_base" to 1,
                    "items.tomato" to 1
                ),
                output = "items.incomplete_pizza"
            ),
            PrepStepDef(
                rowKey = "cooking_pizza_add_cheese",
                inputs = listOf(
                    "items.incomplete_pizza" to 1,
                    "items.cheese" to 1
                ),
                output = "items.uncooked_pizza"
            )
        ),
        heatStep = HeatStepDef(
            rowKey = "cooking_pizza_bake",
            raw = "items.uncooked_pizza",
            cooked = "items.plain_pizza",
            burnt = "items.burnt_pizza",
            xp = 143,
            stopBurnFire = 68,
            stopBurnRange = 68,
            stationMask = STATION_RANGE,
            chances = listOf(
                chance("range", STATION_RANGE, low = 48, high = 352),
                chance("hosidius_5", STATION_RANGE, modifierMask = HOSIDIUS_5, low = 60, high = 364),
                chance("hosidius_10", STATION_RANGE, modifierMask = HOSIDIUS_10, low = 73, high = 377)
            )
        )
    )

    /** Meat pizza topping: plain pizza + cooked meat. */
    val meatPizzaRecipes: List<ActionDef> = multiStepCook(
        key = "items.meat_pizza",
        level = 45,
        prepSteps = listOf(
            PrepStepDef(
                rowKey = "cooking_pizza_add_meat",
                inputs = listOf(
                    "items.plain_pizza" to 1,
                    "items.cooked_meat" to 1
                ),
                output = "items.meat_pizza",
                xp = 26
            )
        )
    )

    /** Anchovy pizza topping: plain pizza + anchovies. */
    val anchovyPizzaRecipes: List<ActionDef> = multiStepCook(
        key = "items.anchovie_pizza",
        level = 55,
        prepSteps = listOf(
            PrepStepDef(
                rowKey = "cooking_pizza_add_anchovies",
                inputs = listOf(
                    "items.plain_pizza" to 1,
                    "items.anchovies" to 1
                ),
                output = "items.anchovie_pizza",
                xp = 39
            )
        )
    )

    /** Pineapple pizza topping: plain pizza + pineapple ring/chunks. */
    val pineapplePizzaRecipes: List<ActionDef> = multiStepCook(
        key = "items.pineapple_pizza",
        level = 65,
        prepSteps = listOf(
            PrepStepDef(
                rowKey = "cooking_pizza_add_pineapple",
                inputs = listOf(
                    "items.plain_pizza" to 1,
                    "items.pineapple_ring" to 1
                ),
                output = "items.pineapple_pizza",
                xp = 52
            )
        )
    )

    /** All pizza recipes combined. */
    val recipes: List<ActionDef> =
        plainPizzaRecipes +
            meatPizzaRecipes +
            anchovyPizzaRecipes +
            pineapplePizzaRecipes
}
