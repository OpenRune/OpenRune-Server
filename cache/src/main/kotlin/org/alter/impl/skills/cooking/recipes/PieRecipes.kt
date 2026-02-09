package org.alter.impl.skills.cooking.recipes

import org.alter.impl.skills.cooking.ActionDef
import org.alter.impl.skills.cooking.CookingConstants.STATION_RANGE
import org.alter.impl.skills.cooking.CookingHelpers.multiStepCook
import org.alter.impl.skills.cooking.HeatStepDef
import org.alter.impl.skills.cooking.PrepStepDef

/**
 * Remaining pie recipes beyond garden pie.
 *
 * All pies follow the same flow:
 * 1. Prep steps: add fillings to a pie shell (item-on-item)
 * 2. Heat step: bake the uncooked pie on a range
 */
object PieRecipes {

    /** Redberry pie (level 10). */
    val redberryPie: List<ActionDef> = multiStepCook(
        key = "items.uncooked_redberry_pie",
        level = 10,
        prepSteps = listOf(
            PrepStepDef(
                rowKey = "cooking_redberry_pie_add_berries",
                inputs = listOf(
                    "items.pie_shell" to 1,
                    "items.redberries" to 1
                ),
                output = "items.uncooked_redberry_pie"
            )
        ),
        heatStep = HeatStepDef(
            rowKey = "cooking_redberry_pie_bake",
            raw = "items.uncooked_redberry_pie",
            cooked = "items.redberry_pie",
            burnt = "items.burnt_pie",
            xp = 78,
            stopBurnFire = 44,
            stopBurnRange = 44,
            stationMask = STATION_RANGE
        )
    )

    /** Meat pie (level 20). */
    val meatPie: List<ActionDef> = multiStepCook(
        key = "items.uncooked_meat_pie",
        level = 20,
        prepSteps = listOf(
            PrepStepDef(
                rowKey = "cooking_meat_pie_add_meat",
                inputs = listOf(
                    "items.pie_shell" to 1,
                    "items.cooked_meat" to 1
                ),
                output = "items.uncooked_meat_pie"
            )
        ),
        heatStep = HeatStepDef(
            rowKey = "cooking_meat_pie_bake",
            raw = "items.uncooked_meat_pie",
            cooked = "items.meat_pie",
            burnt = "items.burnt_pie",
            xp = 110,
            stopBurnFire = 54,
            stopBurnRange = 54,
            stationMask = STATION_RANGE
        )
    )

    /** Mud pie (level 29). */
    val mudPie: List<ActionDef> = multiStepCook(
        key = "items.raw_mud_pie",
        level = 29,
        prepSteps = listOf(
            PrepStepDef(
                rowKey = "cooking_mud_pie_add_compost",
                inputs = listOf(
                    "items.pie_shell" to 1,
                    "items.compost" to 1
                ),
                output = "items.part_mud_pie_1"
            ),
            PrepStepDef(
                rowKey = "cooking_mud_pie_add_water",
                inputs = listOf(
                    "items.part_mud_pie_1" to 1,
                    "items.bucket_water" to 1
                ),
                output = "items.part_mud_pie_2",
                always = listOf("items.bucket_empty" to 1)
            ),
            PrepStepDef(
                rowKey = "cooking_mud_pie_add_clay",
                inputs = listOf(
                    "items.part_mud_pie_2" to 1,
                    "items.clay" to 1
                ),
                output = "items.raw_mud_pie"
            )
        ),
        heatStep = HeatStepDef(
            rowKey = "cooking_mud_pie_bake",
            raw = "items.raw_mud_pie",
            cooked = "items.mud_pie",
            burnt = "items.burnt_pie",
            xp = 128,
            stopBurnFire = 63,
            stopBurnRange = 63,
            stationMask = STATION_RANGE
        )
    )

    /** Apple pie (level 30). */
    val applePie: List<ActionDef> = multiStepCook(
        key = "items.uncooked_apple_pie",
        level = 30,
        prepSteps = listOf(
            PrepStepDef(
                rowKey = "cooking_apple_pie_add_apple",
                inputs = listOf(
                    "items.pie_shell" to 1,
                    "items.cooking_apple" to 1
                ),
                output = "items.uncooked_apple_pie"
            )
        ),
        heatStep = HeatStepDef(
            rowKey = "cooking_apple_pie_bake",
            raw = "items.uncooked_apple_pie",
            cooked = "items.apple_pie",
            burnt = "items.burnt_pie",
            xp = 130,
            stopBurnFire = 64,
            stopBurnRange = 64,
            stationMask = STATION_RANGE
        )
    )

    /** Fish pie (level 47). */
    val fishPie: List<ActionDef> = multiStepCook(
        key = "items.raw_fish_pie",
        level = 47,
        prepSteps = listOf(
            PrepStepDef(
                rowKey = "cooking_fish_pie_add_trout",
                inputs = listOf(
                    "items.pie_shell" to 1,
                    "items.trout" to 1
                ),
                output = "items.part_fish_pie_1"
            ),
            PrepStepDef(
                rowKey = "cooking_fish_pie_add_cod",
                inputs = listOf(
                    "items.part_fish_pie_1" to 1,
                    "items.cod" to 1
                ),
                output = "items.part_fish_pie_2"
            ),
            PrepStepDef(
                rowKey = "cooking_fish_pie_add_potato",
                inputs = listOf(
                    "items.part_fish_pie_2" to 1,
                    "items.potato" to 1
                ),
                output = "items.raw_fish_pie"
            )
        ),
        heatStep = HeatStepDef(
            rowKey = "cooking_fish_pie_bake",
            raw = "items.raw_fish_pie",
            cooked = "items.fish_pie",
            burnt = "items.burnt_pie",
            xp = 164,
            stopBurnFire = 81,
            stopBurnRange = 81,
            stationMask = STATION_RANGE
        )
    )

    /** Botanical pie (level 52). */
    val botanicalPie: List<ActionDef> = multiStepCook(
        key = "items.uncooked_botanical_pie",
        level = 52,
        prepSteps = listOf(
            PrepStepDef(
                rowKey = "cooking_botanical_pie_add_golovanova",
                inputs = listOf(
                    "items.pie_shell" to 1,
                    "items.golovanova_fruit_top" to 1
                ),
                output = "items.uncooked_botanical_pie"
            )
        ),
        heatStep = HeatStepDef(
            rowKey = "cooking_botanical_pie_bake",
            raw = "items.uncooked_botanical_pie",
            cooked = "items.botanical_pie",
            burnt = "items.burnt_pie",
            xp = 180,
            stopBurnFire = 86,
            stopBurnRange = 86,
            stationMask = STATION_RANGE
        )
    )

    /** Mushroom pie (level 60). */
    val mushroomPie: List<ActionDef> = multiStepCook(
        key = "items.raw_mushroom_pie",
        level = 60,
        prepSteps = listOf(
            PrepStepDef(
                rowKey = "cooking_mushroom_pie_add_sulliuscep",
                inputs = listOf(
                    "items.pie_shell" to 1,
                    "items.sulliuscep_cap" to 1
                ),
                output = "items.raw_mushroom_pie"
            )
        ),
        heatStep = HeatStepDef(
            rowKey = "cooking_mushroom_pie_bake",
            raw = "items.raw_mushroom_pie",
            cooked = "items.mushroom_pie",
            burnt = "items.burnt_pie",
            xp = 200,
            stopBurnFire = 94,
            stopBurnRange = 94,
            stationMask = STATION_RANGE
        )
    )

    /** Admiral pie (level 70). */
    val admiralPie: List<ActionDef> = multiStepCook(
        key = "items.raw_admiral_pie",
        level = 70,
        prepSteps = listOf(
            PrepStepDef(
                rowKey = "cooking_admiral_pie_add_salmon",
                inputs = listOf(
                    "items.pie_shell" to 1,
                    "items.salmon" to 1
                ),
                output = "items.part_admiral_pie_1"
            ),
            PrepStepDef(
                rowKey = "cooking_admiral_pie_add_tuna",
                inputs = listOf(
                    "items.part_admiral_pie_1" to 1,
                    "items.tuna" to 1
                ),
                output = "items.part_admiral_pie_2"
            ),
            PrepStepDef(
                rowKey = "cooking_admiral_pie_add_potato",
                inputs = listOf(
                    "items.part_admiral_pie_2" to 1,
                    "items.potato" to 1
                ),
                output = "items.raw_admiral_pie"
            )
        ),
        heatStep = HeatStepDef(
            rowKey = "cooking_admiral_pie_bake",
            raw = "items.raw_admiral_pie",
            cooked = "items.admiral_pie",
            burnt = "items.burnt_pie",
            xp = 210,
            stopBurnFire = 100,
            stopBurnRange = 94,
            stationMask = STATION_RANGE
        )
    )

    /** Dragonfruit pie (level 73). */
    val dragonfruitPie: List<ActionDef> = multiStepCook(
        key = "items.uncooked_dragonfruit_pie",
        level = 73,
        prepSteps = listOf(
            PrepStepDef(
                rowKey = "cooking_dragonfruit_pie_add_fruit",
                inputs = listOf(
                    "items.pie_shell" to 1,
                    "items.dragonfruit" to 1
                ),
                output = "items.uncooked_dragonfruit_pie"
            )
        ),
        heatStep = HeatStepDef(
            rowKey = "cooking_dragonfruit_pie_bake",
            raw = "items.uncooked_dragonfruit_pie",
            cooked = "items.dragonfruit_pie",
            burnt = "items.burnt_pie",
            xp = 220,
            stopBurnFire = 100,
            stopBurnRange = 97,
            stationMask = STATION_RANGE
        )
    )

    /** Wild pie (level 85). */
    val wildPie: List<ActionDef> = multiStepCook(
        key = "items.raw_wild_pie",
        level = 85,
        prepSteps = listOf(
            PrepStepDef(
                rowKey = "cooking_wild_pie_add_bear",
                inputs = listOf(
                    "items.pie_shell" to 1,
                    "items.raw_bear_meat" to 1
                ),
                output = "items.part_wild_pie_1"
            ),
            PrepStepDef(
                rowKey = "cooking_wild_pie_add_chompy",
                inputs = listOf(
                    "items.part_wild_pie_1" to 1,
                    "items.raw_chompy" to 1
                ),
                output = "items.part_wild_pie_2"
            ),
            PrepStepDef(
                rowKey = "cooking_wild_pie_add_rabbit",
                inputs = listOf(
                    "items.part_wild_pie_2" to 1,
                    "items.raw_rabbit" to 1
                ),
                output = "items.raw_wild_pie"
            )
        ),
        heatStep = HeatStepDef(
            rowKey = "cooking_wild_pie_bake",
            raw = "items.raw_wild_pie",
            cooked = "items.wild_pie",
            burnt = "items.burnt_pie",
            xp = 240,
            stopBurnFire = 100,
            stopBurnRange = 100,
            stationMask = STATION_RANGE
        )
    )

    /** Summer pie (level 95). */
    val summerPie: List<ActionDef> = multiStepCook(
        key = "items.raw_summer_pie",
        level = 95,
        prepSteps = listOf(
            PrepStepDef(
                rowKey = "cooking_summer_pie_add_watermelon",
                inputs = listOf(
                    "items.pie_shell" to 1,
                    "items.watermelon" to 1
                ),
                output = "items.part_summer_pie_1"
            ),
            PrepStepDef(
                rowKey = "cooking_summer_pie_add_apple",
                inputs = listOf(
                    "items.part_summer_pie_1" to 1,
                    "items.cooking_apple" to 1
                ),
                output = "items.part_summer_pie_2"
            ),
            PrepStepDef(
                rowKey = "cooking_summer_pie_add_strawberry",
                inputs = listOf(
                    "items.part_summer_pie_2" to 1,
                    "items.strawberry" to 1
                ),
                output = "items.raw_summer_pie"
            )
        ),
        heatStep = HeatStepDef(
            rowKey = "cooking_summer_pie_bake",
            raw = "items.raw_summer_pie",
            cooked = "items.summer_pie",
            burnt = "items.burnt_pie",
            xp = 260,
            stopBurnFire = 100,
            stopBurnRange = 100,
            stationMask = STATION_RANGE
        )
    )

    /** All pie recipes combined. */
    val recipes: List<ActionDef> =
        redberryPie +
            meatPie +
            mudPie +
            applePie +
            fishPie +
            botanicalPie +
            mushroomPie +
            admiralPie +
            dragonfruitPie +
            wildPie +
            summerPie
}
