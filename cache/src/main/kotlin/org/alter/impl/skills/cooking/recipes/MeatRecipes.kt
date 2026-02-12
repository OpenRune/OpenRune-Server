package org.alter.impl.skills.cooking.recipes

import org.alter.impl.skills.cooking.ActionDef
import org.alter.impl.skills.cooking.CookingConstants.STATION_FIRE
import org.alter.impl.skills.cooking.CookingHelpers.heatCook
import org.alter.impl.skills.cooking.CookingHelpers.spitRoast

/**
 * Meat cooking recipes - standard cooking and spit roasting.
 */
object MeatRecipes {

    /** Basic meat cooking recipes (fire/range). */
    val basicMeatRecipes: List<ActionDef> = listOf(
        heatCook(
            rowKey = "cooking_chompy",
            raw = "items.raw_chompy",
            cooked = "items.cooked_chompy",
            burnt = "items.ruined_chompy",
            level = 30,
            xp = 100,
            stopBurnFire = 63,
            stopBurnRange = 63
        ),
        heatCook(
            rowKey = "cooking_beef",
            raw = "items.raw_beef",
            cooked = "items.cooked_meat",
            burnt = "items.burnt_meat",
            level = 1,
            xp = 30,
            stopBurnFire = 34,
            stopBurnRange = 34
        ),
        heatCook(
            rowKey = "cooking_chicken",
            raw = "items.raw_chicken",
            cooked = "items.cooked_chicken",
            burnt = "items.burnt_chicken",
            level = 1,
            xp = 30,
            stopBurnFire = 34,
            stopBurnRange = 34
        ),
        heatCook(
            rowKey = "cooking_rat_meat",
            raw = "items.raw_rat_meat",
            cooked = "items.cooked_meat",
            burnt = "items.burnt_meat",
            level = 1,
            xp = 30,
            stopBurnFire = 34,
            stopBurnRange = 34
        ),
        heatCook(
            rowKey = "cooking_bear_meat",
            raw = "items.raw_bear_meat",
            cooked = "items.cooked_meat",
            burnt = "items.burnt_meat",
            level = 1,
            xp = 30,
            stopBurnFire = 34,
            stopBurnRange = 34
        ),
        heatCook(
            rowKey = "cooking_rabbit",
            raw = "items.raw_rabbit",
            cooked = "items.cooked_rabbit",
            burnt = "items.burnt_meat",
            level = 1,
            xp = 30,
            stopBurnFire = 34,
            stopBurnRange = 34
        ),
        heatCook(
            rowKey = "cooking_ugthanki_meat",
            raw = "items.raw_ugthanki_meat",
            cooked = "items.cooked_ugthanki_meat",
            burnt = "items.burnt_meat",
            level = 1,
            xp = 40,
            stopBurnFire = 34,
            stopBurnRange = 34
        )
    )

    /** Spit roasting recipes (skewer + fire-only roast). */
    val spitRoastRecipes: List<ActionDef> =
        spitRoast(
            skewerRowKey = "cooking_skewer_bird_meat",
            roastRowKey = "cooking_roast_bird_meat",
            rawMeat = "items.spit_raw_bird_meat",
            skewered = "items.spit_skewered_bird_meat",
            cooked = "items.spit_roasted_bird_meat",
            burnt = "items.spit_burned_bird_meat",
            cookingLevel = 11,
            xp = 60,
            stopBurnFire = 44,
            stopBurnRange = 44
        ) +
        spitRoast(
            skewerRowKey = "cooking_skewer_beast_meat",
            roastRowKey = "cooking_roast_beast_meat",
            rawMeat = "items.spit_raw_beast_meat",
            skewered = "items.spit_skewered_beast_meat",
            cooked = "items.spit_roasted_beast_meat",
            burnt = "items.spit_burned_beast_meat",
            cookingLevel = 21,
            xp = 82,
            stopBurnFire = 55,
            stopBurnRange = 55
        ) +
        spitRoast(
            skewerRowKey = "cooking_skewer_rabbit_meat",
            roastRowKey = "cooking_roast_rabbit_meat",
            rawMeat = "items.raw_rabbit",
            skewered = "items.spit_skewered_rabbit_meat",
            cooked = "items.spit_roasted_rabbit_meat",
            burnt = "items.spit_burned_rabbit_meat",
            cookingLevel = 16,
            xp = 70,
            stopBurnFire = 99,
            stopBurnRange = 99
        )

    /**
     * Hunter meat recipes — meats obtained from pitfall trapping.
     * These are standard heatCook recipes; some are fire-only.
     *
     * Note: Small kebbits (wild, barb-tailed, dashing) are eaten raw and do NOT
     * require cooking. Only pitfall-trapped animals have raw→cooked transitions.
     */
    val hunterMeatRecipes: List<ActionDef> = listOf(
        heatCook(
            rowKey = "cooking_larupia",
            raw = "items.hunting_larupia_meat",
            cooked = "items.larupia_cooked",
            burnt = "items.burnt_largebeast",
            level = 31,
            xp = 92,
            stopBurnFire = 59,
            stopBurnRange = 59,
            stationMask = STATION_FIRE
        ),
        heatCook(
            rowKey = "cooking_graahk",
            raw = "items.hunting_graahk_meat",
            cooked = "items.graahk_cooked",
            burnt = "items.burnt_largebeast",
            level = 41,
            xp = 124,
            stopBurnFire = 75,
            stopBurnRange = 75,
            stationMask = STATION_FIRE
        ),
        heatCook(
            rowKey = "cooking_kyatt",
            raw = "items.hunting_kyatt_meat",
            cooked = "items.kyatt_cooked",
            burnt = "items.burnt_largebeast",
            level = 51,
            xp = 143,
            stopBurnFire = 86,
            stopBurnRange = 80
        ),
        heatCook(
            rowKey = "cooking_pyre_fox",
            raw = "items.hunting_fennecfox_meat",
            cooked = "items.fennecfox_cooked",
            burnt = "items.burnt_foxmeat",
            level = 59,
            xp = 154,
            stopBurnFire = 93,
            stopBurnRange = 92
        ),
        heatCook(
            rowKey = "cooking_sunlight_antelope",
            raw = "items.hunting_antelopesun_meat",
            cooked = "items.antelopesun_cooked",
            burnt = "items.burnt_antelope",
            level = 68,
            xp = 175,
            stopBurnFire = 100,
            stopBurnRange = 95
        ),
        heatCook(
            rowKey = "cooking_moonlight_antelope",
            raw = "items.hunting_antelopemoon_meat",
            cooked = "items.antelopemoon_cooked",
            burnt = "items.burnt_antelope",
            level = 92,
            xp = 220,
            stopBurnFire = 100,
            stopBurnRange = 100
        )
    )

    /** All meat recipes combined. */
    val recipes: List<ActionDef> = basicMeatRecipes + spitRoastRecipes + hunterMeatRecipes
}
