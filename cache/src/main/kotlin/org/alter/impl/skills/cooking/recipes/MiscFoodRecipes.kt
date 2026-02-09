package org.alter.impl.skills.cooking.recipes

import org.alter.impl.skills.cooking.ActionDef
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
            raw = "items.sweetcorn_raw",
            cooked = "items.sweetcorn_cooked",
            burnt = "items.burnt_sweetcorn",
            level = 28,
            xp = 104,
            stopBurnFire = 61,
            stopBurnRange = 61
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
                output = "items.bowl_egg_uncooked"
            )
        )
    ) + listOf(
        heatCook(
            rowKey = "cooking_scrambled_egg_cook",
            raw = "items.bowl_egg_uncooked",
            cooked = "items.bowl_egg_scrambled",
            burnt = "items.burnt_egg",
            level = 13,
            xp = 50,
            stopBurnFire = 46,
            stopBurnRange = 46
        )
    )

    /** Fried onions: chopped onion + bowl on fire/range. */
    val friedOnions: List<ActionDef> = listOf(
        heatCook(
            rowKey = "cooking_fried_onions",
            raw = "items.bowl_onion_chopped",
            cooked = "items.bowl_onion_fried",
            burnt = "items.burnt_onion",
            level = 42,
            xp = 60,
            stopBurnFire = 74,
            stopBurnRange = 74
        )
    )

    /** Fried mushrooms: sliced mushrooms + bowl on fire/range. */
    val friedMushrooms: List<ActionDef> = listOf(
        heatCook(
            rowKey = "cooking_fried_mushrooms",
            raw = "items.bowl_mushroom_sliced",
            cooked = "items.bowl_mushroom_fried",
            burnt = "items.burnt_mushroom",
            level = 46,
            xp = 60,
            stopBurnFire = 80,
            stopBurnRange = 80
        )
    )

    /** Mushroom & onion: fried mushrooms + fried onions. */
    val mushroomAndOnion: List<ActionDef> = multiStepCook(
        key = "items.bowl_mushroom_onion",
        level = 57,
        prepSteps = listOf(
            PrepStepDef(
                rowKey = "cooking_mushroom_onion_combine",
                inputs = listOf(
                    "items.bowl_mushroom_fried" to 1,
                    "items.bowl_onion_fried" to 1
                ),
                output = "items.bowl_mushroom_onion",
                always = listOf("items.bowl_empty" to 1)
            )
        )
    )

    /** Tuna and corn: cooked tuna + cooked sweetcorn + bowl. */
    val tunaAndCorn: List<ActionDef> = multiStepCook(
        key = "items.bowl_tuna_corn",
        level = 67,
        prepSteps = listOf(
            PrepStepDef(
                rowKey = "cooking_tuna_corn_combine",
                inputs = listOf(
                    "items.tuna" to 1,
                    "items.sweetcorn_cooked" to 1,
                    "items.bowl_empty" to 1
                ),
                output = "items.bowl_tuna_corn"
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
