package org.alter.impl.skills.cooking.recipes

import org.alter.impl.skills.cooking.ActionDef
import org.alter.impl.skills.cooking.CookingConstants.ChanceModifier.HOSIDIUS_10
import org.alter.impl.skills.cooking.CookingConstants.ChanceModifier.HOSIDIUS_5
import org.alter.impl.skills.cooking.CookingConstants.STATION_ANY
import org.alter.impl.skills.cooking.CookingConstants.STATION_RANGE
import org.alter.impl.skills.cooking.CookingHelpers.chance
import org.alter.impl.skills.cooking.CookingHelpers.heatCook
import org.alter.impl.skills.cooking.CookingHelpers.multiStepCook
import org.alter.impl.skills.cooking.PrepStepDef

/**
 * Miscellaneous cooking recipes — vegetable side dishes, dairy, scrambled egg, etc.
 */
object MiscFoodRecipes {

    /** Sweetcorn: raw sweetcorn on fire/range. */
    val sweetcorn: List<ActionDef> = listOf(
        heatCook(
            rowKey = "cooking_sweetcorn",
            raw = "items.sweetcorn",
            cooked = "items.sweetcorn_cooked",
            burnt = "items.sweetcorn_burnt",
            level = 28, xp = 104, stopBurnFire = 61, stopBurnRange = 61,
            chances = listOf(
                chance("base_any", STATION_ANY, low = 78, high = 412),
                chance("hosidius_5", STATION_RANGE, modifierMask = HOSIDIUS_5, low = 90, high = 424),
                chance("hosidius_10", STATION_RANGE, modifierMask = HOSIDIUS_10, low = 103, high = 437)
            )
        )
    )

    /** Scrambled egg: egg + bowl on range. */
    val scrambledEgg: List<ActionDef> = multiStepCook(
        key = "items.bowl_egg_scrambled",
        level = 13,
        prepSteps = listOf(
            PrepStepDef(
                rowKey = "cooking_scrambled_egg_mix",
                inputs = listOf(
                    "items.egg" to 1,
                    "items.bowl_empty" to 1
                ),
                output = "items.bowl_egg_raw"
            )
        )
    ) + listOf(
        heatCook(
            rowKey = "cooking_scrambled_egg_cook",
            raw = "items.bowl_egg_raw",
            cooked = "items.bowl_egg_scrambled",
            burnt = "items.bowl_egg_burnt",
            level = 13, xp = 50, stopBurnFire = 46, stopBurnRange = 46,
            stationMask = STATION_RANGE,
            chances = listOf(
                chance("range", STATION_RANGE, low = 90, high = 438),
                chance("hosidius_5", STATION_RANGE, modifierMask = HOSIDIUS_5, low = 102, high = 450),
                chance("hosidius_10", STATION_RANGE, modifierMask = HOSIDIUS_10, low = 115, high = 463)
            )
        )
    )

    /** Fried onions: chopped onion + bowl on fire/range. */
    val friedOnions: List<ActionDef> = listOf(
        heatCook(
            rowKey = "cooking_fried_onions",
            raw = "items.bowl_onion",
            cooked = "items.bowl_onion_fried",
            burnt = "items.bowl_onion_burnt",
            level = 42, xp = 60, stopBurnFire = 74, stopBurnRange = 74,
            chances = listOf(
                chance("base_any", STATION_ANY, low = 36, high = 322),
                chance("hosidius_5", STATION_RANGE, modifierMask = HOSIDIUS_5, low = 48, high = 334),
                chance("hosidius_10", STATION_RANGE, modifierMask = HOSIDIUS_10, low = 61, high = 347)
            )
        )
    )

    /** Fried mushrooms: sliced mushrooms + bowl on fire/range. */
    val friedMushrooms: List<ActionDef> = listOf(
        heatCook(
            rowKey = "cooking_fried_mushrooms",
            raw = "items.bowl_mushroom_raw",
            cooked = "items.bowl_mushroom_fried",
            burnt = "items.bowl_mushroom_burnt",
            level = 46, xp = 60, stopBurnFire = 80, stopBurnRange = 80,
            chances = listOf(
                chance("base_any", STATION_ANY, low = 16, high = 282),
                chance("hosidius_5", STATION_RANGE, modifierMask = HOSIDIUS_5, low = 28, high = 294),
                chance("hosidius_10", STATION_RANGE, modifierMask = HOSIDIUS_10, low = 41, high = 307)
            )
        )
    )

    /** Mushroom & onion: fried mushrooms + fried onions. */
    val mushroomAndOnion: List<ActionDef> = multiStepCook(
        key = "items.bowl_mushroom+onion",
        level = 57,
        prepSteps = listOf(
            PrepStepDef(
                rowKey = "cooking_mushroom_onion_combine",
                inputs = listOf(
                    "items.bowl_mushroom_fried" to 1,
                    "items.bowl_onion_fried" to 1
                ),
                output = "items.bowl_mushroom+onion",
                always = listOf("items.bowl_empty" to 1)
            )
        )
    )

    /** Tuna and corn: cooked tuna + cooked sweetcorn + bowl. */
    val tunaAndCorn: List<ActionDef> = multiStepCook(
        key = "items.bowl_tuna+sweetcorn",
        level = 67,
        prepSteps = listOf(
            PrepStepDef(
                rowKey = "cooking_tuna_corn_combine",
                inputs = listOf(
                    "items.tuna" to 1,
                    "items.sweetcorn_cooked" to 1,
                    "items.bowl_empty" to 1
                ),
                output = "items.bowl_tuna+sweetcorn"
            )
        )
    )

    /** Chocolate cake: cake + chocolate bar. */
    val chocolateCake: List<ActionDef> = multiStepCook(
        key = "items.chocolate_cake",
        level = 50,
        prepSteps = listOf(
            PrepStepDef(
                rowKey = "cooking_chocolate_cake",
                inputs = listOf(
                    "items.cake" to 1,
                    "items.chocolate_bar" to 1
                ),
                output = "items.chocolate_cake"
            )
        )
    )

    /** All misc cooking recipes combined. */
    val recipes: List<ActionDef> =
        sweetcorn +
            scrambledEgg +
            friedOnions +
            friedMushrooms +
            mushroomAndOnion +
            tunaAndCorn +
            chocolateCake
}
