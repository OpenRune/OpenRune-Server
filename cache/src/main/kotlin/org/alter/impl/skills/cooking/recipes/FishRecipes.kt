package org.alter.impl.skills.cooking.recipes

import org.alter.impl.skills.cooking.ActionDef
import org.alter.impl.skills.cooking.CookingConstants.STATION_FIRE
import org.alter.impl.skills.cooking.CookingHelpers.heatCook

/**
 * Fish cooking recipes - standard heat-source cooking.
 */
object FishRecipes {

    val recipes: List<ActionDef> = listOf(
        heatCook(
            rowKey = "cooking_shrimps",
            raw = "items.raw_shrimp",
            cooked = "items.shrimp",
            burnt = "items.burnt_shrimp",
            level = 1,
            xp = 30,
            stopBurnFire = 34,
            stopBurnRange = 34
        ),
        heatCook(
            rowKey = "cooking_anchovies",
            raw = "items.raw_anchovies",
            cooked = "items.anchovies",
            burnt = "items.burntfish1",
            level = 1,
            xp = 30,
            stopBurnFire = 34,
            stopBurnRange = 34
        ),
        heatCook(
            rowKey = "cooking_sardine",
            raw = "items.raw_sardine",
            cooked = "items.sardine",
            burnt = "items.burntfish5",
            level = 1,
            xp = 40,
            stopBurnFire = 38,
            stopBurnRange = 38
        ),
        heatCook(
            rowKey = "cooking_herring",
            raw = "items.raw_herring",
            cooked = "items.herring",
            burnt = "items.burntfish3",
            level = 5,
            xp = 50,
            stopBurnFire = 41,
            stopBurnRange = 41
        ),
        heatCook(
            rowKey = "cooking_mackerel",
            raw = "items.raw_mackerel",
            cooked = "items.mackerel",
            burnt = "items.burntfish3",
            level = 10,
            xp = 60,
            stopBurnFire = 45,
            stopBurnRange = 45
        ),
        heatCook(
            rowKey = "cooking_trout",
            raw = "items.raw_trout",
            cooked = "items.trout",
            burnt = "items.burntfish2",
            level = 15,
            xp = 70,
            stopBurnFire = 49,
            stopBurnRange = 49
        ),
        heatCook(
            rowKey = "cooking_cod",
            raw = "items.raw_cod",
            cooked = "items.cod",
            burnt = "items.burntfish2",
            level = 18,
            xp = 75,
            stopBurnFire = 51,
            stopBurnRange = 49
        ),
        heatCook(
            rowKey = "cooking_pike",
            raw = "items.raw_pike",
            cooked = "items.pike",
            burnt = "items.burntfish5",
            level = 20,
            xp = 80,
            stopBurnFire = 54,
            stopBurnRange = 54
        ),
        heatCook(
            rowKey = "cooking_salmon",
            raw = "items.raw_salmon",
            cooked = "items.salmon",
            burnt = "items.burntfish2",
            level = 25,
            xp = 90,
            stopBurnFire = 58,
            stopBurnRange = 58
        ),
        heatCook(
            rowKey = "cooking_slimy_eel",
            raw = "items.mort_slimey_eel",
            cooked = "items.mort_slimey_eel_cooked",
            burnt = "items.burnt_eel",
            level = 28,
            xp = 95,
            stopBurnFire = 61,
            stopBurnRange = 61
        ),
        heatCook(
            rowKey = "cooking_tuna",
            raw = "items.raw_tuna",
            cooked = "items.tuna",
            burnt = "items.burntfish4",
            level = 30,
            xp = 100,
            stopBurnFire = 63,
            stopBurnRange = 63
        ),
        heatCook(
            rowKey = "cooking_lobster",
            raw = "items.raw_lobster",
            cooked = "items.lobster",
            burnt = "items.burnt_lobster",
            level = 40,
            xp = 120,
            stopBurnFire = 74,
            stopBurnRange = 74
        ),
        heatCook(
            rowKey = "cooking_bass",
            raw = "items.raw_bass",
            cooked = "items.bass",
            burnt = "items.burntfish3",
            level = 43,
            xp = 130,
            stopBurnFire = 79,
            stopBurnRange = 79
        ),
        heatCook(
            rowKey = "cooking_swordfish",
            raw = "items.raw_swordfish",
            cooked = "items.swordfish",
            burnt = "items.burnt_swordfish",
            level = 45,
            xp = 140,
            stopBurnFire = 86,
            stopBurnRange = 80
        ),
        heatCook(
            rowKey = "cooking_monkfish",
            raw = "items.raw_monkfish",
            cooked = "items.monkfish",
            burnt = "items.burnt_monkfish",
            level = 62,
            xp = 150,
            stopBurnFire = 92,
            stopBurnRange = 90
        ),
        heatCook(
            rowKey = "cooking_shark",
            raw = "items.raw_shark",
            cooked = "items.shark",
            burnt = "items.burnt_shark",
            level = 80,
            xp = 210,
            stopBurnFire = 100,
            stopBurnRange = 100
        ),
        heatCook(
            rowKey = "cooking_sea_turtle",
            raw = "items.raw_seaturtle",
            cooked = "items.seaturtle",
            burnt = "items.burnt_seaturtle",
            level = 82,
            xp = 211,
            stopBurnFire = 100,
            stopBurnRange = 100
        ),
        heatCook(
            rowKey = "cooking_anglerfish",
            raw = "items.raw_anglerfish",
            cooked = "items.anglerfish",
            burnt = "items.burnt_anglerfish",
            level = 84,
            xp = 230,
            stopBurnFire = 100,
            stopBurnRange = 100
        ),
        heatCook(
            rowKey = "cooking_dark_crab",
            raw = "items.raw_dark_crab",
            cooked = "items.dark_crab",
            burnt = "items.burnt_dark_crab",
            level = 90,
            xp = 215,
            stopBurnFire = 100,
            stopBurnRange = 100
        ),
        heatCook(
            rowKey = "cooking_manta_ray",
            raw = "items.raw_mantaray",
            cooked = "items.mantaray",
            burnt = "items.burnt_mantaray",
            level = 91,
            xp = 216,
            stopBurnFire = 100,
            stopBurnRange = 100
        ),
        heatCook(
            rowKey = "cooking_karambwan",
            raw = "items.tbwt_raw_karambwan",
            cooked = "items.tbwt_cooked_karambwan",
            burnt = "items.tbwt_burnt_karambwan",
            level = 30,
            xp = 190,
            stopBurnFire = 99,
            stopBurnRange = 93
        ),
        heatCook(
            rowKey = "cooking_rainbow_fish",
            raw = "items.hunting_raw_fish_special",
            cooked = "items.hunting_fish_special",
            burnt = "items.burnt_rainbow_fish",
            level = 35,
            xp = 110,
            stopBurnFire = 64,
            stopBurnRange = 60
        ),
        heatCook(
            rowKey = "cooking_cave_eel",
            raw = "items.raw_cave_eel",
            cooked = "items.cave_eel",
            burnt = "items.burnt_cave_eel",
            level = 38,
            xp = 115,
            stopBurnFire = 74,
            stopBurnRange = 70
        ),

        // --- Snails (fire-only, Morytania) ---
        heatCook(
            rowKey = "cooking_thin_snail",
            raw = "items.snail_corpse1",
            cooked = "items.snail_corpse_cooked1",
            burnt = "items.burnt_snail",
            level = 12,
            xp = 70,
            stopBurnFire = 47,
            stopBurnRange = 47,
            stationMask = STATION_FIRE
        ),
        heatCook(
            rowKey = "cooking_lean_snail",
            raw = "items.snail_corpse2",
            cooked = "items.snail_corpse_cooked2",
            burnt = "items.burnt_snail",
            level = 17,
            xp = 80,
            stopBurnFire = 47,
            stopBurnRange = 50
        ),
        heatCook(
            rowKey = "cooking_fat_snail",
            raw = "items.snail_corpse3",
            cooked = "items.snail_corpse_cooked3",
            burnt = "items.burnt_snail",
            level = 22,
            xp = 95,
            stopBurnFire = 56,
            stopBurnRange = 56,
            stationMask = STATION_FIRE
        )
    )
}
