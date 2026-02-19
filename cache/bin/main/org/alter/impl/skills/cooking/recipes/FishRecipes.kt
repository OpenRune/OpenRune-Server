package org.alter.impl.skills.cooking.recipes

import org.alter.impl.skills.cooking.ActionDef
import org.alter.impl.skills.cooking.CookingConstants.ChanceModifier.GAUNTLETS
import org.alter.impl.skills.cooking.CookingConstants.ChanceModifier.HOSIDIUS_10
import org.alter.impl.skills.cooking.CookingConstants.ChanceModifier.HOSIDIUS_5
import org.alter.impl.skills.cooking.CookingConstants.ChanceModifier.LUMBRIDGE
import org.alter.impl.skills.cooking.CookingConstants.STATION_ANY
import org.alter.impl.skills.cooking.CookingConstants.STATION_FIRE
import org.alter.impl.skills.cooking.CookingConstants.STATION_RANGE
import org.alter.impl.skills.cooking.CookingHelpers.chance
import org.alter.impl.skills.cooking.CookingHelpers.heatCook

/**
 * Fish cooking recipes — standard heat-source cooking.
 *
 * Chance profiles sourced from OSRS Wiki skill_chances data.
 */
object FishRecipes {

    val recipes: List<ActionDef> = listOf(
        // ---- Low-level fish (shared fire/range curve + Lumbridge) ----
        heatCook(
            rowKey = "cooking_shrimps",
            raw = "items.raw_shrimp",
            cooked = "items.shrimp",
            burnt = "items.burnt_shrimp",
            level = 1, xp = 30, stopBurnFire = 34, stopBurnRange = 34,
            chances = listOf(
                chance("base_any", STATION_ANY, low = 128, high = 512),
                chance("lumbridge", STATION_RANGE, modifierMask = LUMBRIDGE, low = 138, high = 532),
                chance("hosidius_5", STATION_RANGE, modifierMask = HOSIDIUS_5, low = 140, high = 524),
                chance("hosidius_10", STATION_RANGE, modifierMask = HOSIDIUS_10, low = 153, high = 537)
            )
        ),
        heatCook(
            rowKey = "cooking_anchovies",
            raw = "items.raw_anchovies",
            cooked = "items.anchovies",
            burnt = "items.burntfish1",
            level = 1, xp = 30, stopBurnFire = 34, stopBurnRange = 34,
            chances = listOf(
                chance("base_any", STATION_ANY, low = 128, high = 512),
                chance("lumbridge", STATION_RANGE, modifierMask = LUMBRIDGE, low = 138, high = 532),
                chance("hosidius_5", STATION_RANGE, modifierMask = HOSIDIUS_5, low = 140, high = 524),
                chance("hosidius_10", STATION_RANGE, modifierMask = HOSIDIUS_10, low = 153, high = 537)
            )
        ),
        heatCook(
            rowKey = "cooking_sardine",
            raw = "items.raw_sardine",
            cooked = "items.sardine",
            burnt = "items.burntfish5",
            level = 1, xp = 40, stopBurnFire = 38, stopBurnRange = 38,
            chances = listOf(
                chance("base_any", STATION_ANY, low = 118, high = 492),
                chance("lumbridge", STATION_RANGE, modifierMask = LUMBRIDGE, low = 128, high = 512),
                chance("hosidius_5", STATION_RANGE, modifierMask = HOSIDIUS_5, low = 130, high = 504),
                chance("hosidius_10", STATION_RANGE, modifierMask = HOSIDIUS_10, low = 143, high = 517)
            )
        ),
        heatCook(
            rowKey = "cooking_herring",
            raw = "items.raw_herring",
            cooked = "items.herring",
            burnt = "items.burntfish3",
            level = 5, xp = 50, stopBurnFire = 41, stopBurnRange = 41,
            chances = listOf(
                chance("base_any", STATION_ANY, low = 108, high = 472),
                chance("lumbridge", STATION_RANGE, modifierMask = LUMBRIDGE, low = 118, high = 492),
                chance("hosidius_5", STATION_RANGE, modifierMask = HOSIDIUS_5, low = 120, high = 484),
                chance("hosidius_10", STATION_RANGE, modifierMask = HOSIDIUS_10, low = 133, high = 497)
            )
        ),
        heatCook(
            rowKey = "cooking_mackerel",
            raw = "items.raw_mackerel",
            cooked = "items.mackerel",
            burnt = "items.burntfish3",
            level = 10, xp = 60, stopBurnFire = 45, stopBurnRange = 45,
            chances = listOf(
                chance("base_any", STATION_ANY, low = 98, high = 452),
                chance("lumbridge", STATION_RANGE, modifierMask = LUMBRIDGE, low = 108, high = 472),
                chance("hosidius_5", STATION_RANGE, modifierMask = HOSIDIUS_5, low = 110, high = 464),
                chance("hosidius_10", STATION_RANGE, modifierMask = HOSIDIUS_10, low = 123, high = 477)
            )
        ),
        heatCook(
            rowKey = "cooking_trout",
            raw = "items.raw_trout",
            cooked = "items.trout",
            burnt = "items.burntfish2",
            level = 15, xp = 70, stopBurnFire = 49, stopBurnRange = 49,
            chances = listOf(
                chance("base_any", STATION_ANY, low = 88, high = 432),
                chance("lumbridge", STATION_RANGE, modifierMask = LUMBRIDGE, low = 98, high = 452),
                chance("hosidius_5", STATION_RANGE, modifierMask = HOSIDIUS_5, low = 100, high = 444),
                chance("hosidius_10", STATION_RANGE, modifierMask = HOSIDIUS_10, low = 113, high = 457)
            )
        ),

        // ---- Mid-level fish ----
        heatCook(
            rowKey = "cooking_cod",
            raw = "items.raw_cod",
            cooked = "items.cod",
            burnt = "items.burntfish2",
            level = 18, xp = 75, stopBurnFire = 51, stopBurnRange = 49,
            chances = listOf(
                chance("fire", STATION_FIRE, low = 83, high = 422),
                chance("range", STATION_RANGE, low = 88, high = 432),
                chance("lumbridge", STATION_RANGE, modifierMask = LUMBRIDGE, low = 93, high = 442),
                chance("hosidius_5", STATION_RANGE, modifierMask = HOSIDIUS_5, low = 100, high = 444),
                chance("hosidius_10", STATION_RANGE, modifierMask = HOSIDIUS_10, low = 113, high = 457)
            )
        ),
        heatCook(
            rowKey = "cooking_pike",
            raw = "items.raw_pike",
            cooked = "items.pike",
            burnt = "items.burntfish5",
            level = 20, xp = 80, stopBurnFire = 54, stopBurnRange = 54,
            chances = listOf(
                chance("base_any", STATION_ANY, low = 78, high = 412),
                chance("lumbridge", STATION_RANGE, modifierMask = LUMBRIDGE, low = 88, high = 432),
                chance("hosidius_5", STATION_RANGE, modifierMask = HOSIDIUS_5, low = 90, high = 424),
                chance("hosidius_10", STATION_RANGE, modifierMask = HOSIDIUS_10, low = 103, high = 437)
            )
        ),
        heatCook(
            rowKey = "cooking_salmon",
            raw = "items.raw_salmon",
            cooked = "items.salmon",
            burnt = "items.burntfish2",
            level = 25, xp = 90, stopBurnFire = 58, stopBurnRange = 58,
            chances = listOf(
                chance("base_any", STATION_ANY, low = 68, high = 392),
                chance("lumbridge", STATION_RANGE, modifierMask = LUMBRIDGE, low = 78, high = 402),
                chance("hosidius_5", STATION_RANGE, modifierMask = HOSIDIUS_5, low = 80, high = 404),
                chance("hosidius_10", STATION_RANGE, modifierMask = HOSIDIUS_10, low = 93, high = 417)
            )
        ),
        heatCook(
            rowKey = "cooking_slimy_eel",
            raw = "items.mort_slimey_eel",
            cooked = "items.mort_slimey_eel_cooked",
            burnt = "items.burnt_eel",
            level = 28, xp = 95, stopBurnFire = 61, stopBurnRange = 61,
            chances = listOf(
                chance("base_any", STATION_ANY, low = 63, high = 382),
                chance("hosidius_5", STATION_RANGE, modifierMask = HOSIDIUS_5, low = 75, high = 394),
                chance("hosidius_10", STATION_RANGE, modifierMask = HOSIDIUS_10, low = 88, high = 407)
            )
        ),
        heatCook(
            rowKey = "cooking_tuna",
            raw = "items.raw_tuna",
            cooked = "items.tuna",
            burnt = "items.burntfish4",
            level = 30, xp = 100, stopBurnFire = 63, stopBurnRange = 63,
            chances = listOf(
                chance("base_any", STATION_ANY, low = 58, high = 372),
                chance("hosidius_5", STATION_RANGE, modifierMask = HOSIDIUS_5, low = 70, high = 384),
                chance("hosidius_10", STATION_RANGE, modifierMask = HOSIDIUS_10, low = 83, high = 397)
            )
        ),

        // ---- Mid-high fish (gauntlet-affected) ----
        heatCook(
            rowKey = "cooking_lobster",
            raw = "items.raw_lobster",
            cooked = "items.lobster",
            burnt = "items.burnt_lobster",
            level = 40, xp = 120, stopBurnFire = 74, stopBurnRange = 74,
            chances = listOf(
                chance("base_any", STATION_ANY, low = 38, high = 332),
                chance("gauntlets", STATION_ANY, modifierMask = GAUNTLETS, low = 55, high = 368),
                chance("hosidius_5", STATION_RANGE, modifierMask = HOSIDIUS_5, low = 50, high = 344),
                chance("hosidius_10", STATION_RANGE, modifierMask = HOSIDIUS_10, low = 63, high = 357),
                chance("hosidius_5_gauntlets", STATION_RANGE, modifierMask = HOSIDIUS_5 or GAUNTLETS, low = 67, high = 380),
                chance("hosidius_10_gauntlets", STATION_RANGE, modifierMask = HOSIDIUS_10 or GAUNTLETS, low = 80, high = 393)
            )
        ),
        heatCook(
            rowKey = "cooking_bass",
            raw = "items.raw_bass",
            cooked = "items.bass",
            burnt = "items.burntfish3",
            level = 43, xp = 130, stopBurnFire = 79, stopBurnRange = 79,
            chances = listOf(
                chance("base_any", STATION_ANY, low = 33, high = 312),
                chance("hosidius_5", STATION_RANGE, modifierMask = HOSIDIUS_5, low = 45, high = 324),
                chance("hosidius_10", STATION_RANGE, modifierMask = HOSIDIUS_10, low = 58, high = 337)
            )
        ),
        heatCook(
            rowKey = "cooking_swordfish",
            raw = "items.raw_swordfish",
            cooked = "items.swordfish",
            burnt = "items.burnt_swordfish",
            level = 45, xp = 140, stopBurnFire = 86, stopBurnRange = 80,
            chances = listOf(
                chance("fire", STATION_FIRE, low = 18, high = 292),
                chance("range", STATION_RANGE, low = 30, high = 310),
                chance("gauntlets", STATION_ANY, modifierMask = GAUNTLETS, low = 30, high = 310),
                chance("hosidius_5", STATION_RANGE, modifierMask = HOSIDIUS_5, low = 42, high = 322),
                chance("hosidius_10", STATION_RANGE, modifierMask = HOSIDIUS_10, low = 55, high = 335)
            )
        ),

        // ---- High-level fish ----
        heatCook(
            rowKey = "cooking_monkfish",
            raw = "items.raw_monkfish",
            cooked = "items.monkfish",
            burnt = "items.burnt_monkfish",
            level = 62, xp = 150, stopBurnFire = 92, stopBurnRange = 90,
            chances = listOf(
                chance("fire", STATION_FIRE, low = 11, high = 275),
                chance("range", STATION_RANGE, low = 13, high = 280),
                chance("gauntlets", STATION_ANY, modifierMask = GAUNTLETS, low = 24, high = 290),
                chance("hosidius_5", STATION_RANGE, modifierMask = HOSIDIUS_5, low = 25, high = 292),
                chance("hosidius_10", STATION_RANGE, modifierMask = HOSIDIUS_10, low = 38, high = 305),
                chance("hosidius_5_gauntlets", STATION_RANGE, modifierMask = HOSIDIUS_5 or GAUNTLETS, low = 36, high = 302),
                chance("hosidius_10_gauntlets", STATION_RANGE, modifierMask = HOSIDIUS_10 or GAUNTLETS, low = 49, high = 315)
            )
        ),
        heatCook(
            rowKey = "cooking_shark",
            raw = "items.raw_shark",
            cooked = "items.shark",
            burnt = "items.burnt_shark",
            level = 80, xp = 210, stopBurnFire = 100, stopBurnRange = 100,
            chances = listOf(
                chance("fire", STATION_FIRE, low = 1, high = 202),
                chance("range", STATION_RANGE, low = 1, high = 232),
                chance("gauntlets", STATION_ANY, modifierMask = GAUNTLETS, low = 15, high = 270),
                chance("hosidius_5", STATION_RANGE, modifierMask = HOSIDIUS_5, low = 13, high = 244),
                chance("hosidius_10", STATION_RANGE, modifierMask = HOSIDIUS_10, low = 26, high = 257),
                chance("hosidius_5_gauntlets", STATION_RANGE, modifierMask = HOSIDIUS_5 or GAUNTLETS, low = 27, high = 282),
                chance("hosidius_10_gauntlets", STATION_RANGE, modifierMask = HOSIDIUS_10 or GAUNTLETS, low = 40, high = 295)
            )
        ),
        heatCook(
            rowKey = "cooking_sea_turtle",
            raw = "items.raw_seaturtle",
            cooked = "items.seaturtle",
            burnt = "items.burnt_seaturtle",
            level = 82, xp = 211, stopBurnFire = 100, stopBurnRange = 100,
            chances = listOf(
                chance("fire", STATION_FIRE, low = 1, high = 202),
                chance("range", STATION_RANGE, low = 1, high = 222),
                chance("hosidius_5", STATION_RANGE, modifierMask = HOSIDIUS_5, low = 13, high = 234),
                chance("hosidius_10", STATION_RANGE, modifierMask = HOSIDIUS_10, low = 26, high = 247)
            )
        ),
        heatCook(
            rowKey = "cooking_anglerfish",
            raw = "items.raw_anglerfish",
            cooked = "items.anglerfish",
            burnt = "items.burnt_anglerfish",
            level = 84, xp = 230, stopBurnFire = 100, stopBurnRange = 100,
            chances = listOf(
                chance("fire", STATION_FIRE, low = 1, high = 200),
                chance("range", STATION_RANGE, low = 1, high = 220),
                chance("gauntlets", STATION_ANY, modifierMask = GAUNTLETS, low = 12, high = 260),
                chance("hosidius_5", STATION_RANGE, modifierMask = HOSIDIUS_5, low = 13, high = 232),
                chance("hosidius_10", STATION_RANGE, modifierMask = HOSIDIUS_10, low = 26, high = 245),
                chance("hosidius_5_gauntlets", STATION_RANGE, modifierMask = HOSIDIUS_5 or GAUNTLETS, low = 24, high = 272),
                chance("hosidius_10_gauntlets", STATION_RANGE, modifierMask = HOSIDIUS_10 or GAUNTLETS, low = 37, high = 285)
            )
        ),
        heatCook(
            rowKey = "cooking_dark_crab",
            raw = "items.raw_dark_crab",
            cooked = "items.dark_crab",
            burnt = "items.burnt_dark_crab",
            level = 90, xp = 215, stopBurnFire = 100, stopBurnRange = 100,
            chances = listOf(
                chance("base_any", STATION_ANY, low = 10, high = 222),
                chance("hosidius_5", STATION_RANGE, modifierMask = HOSIDIUS_5, low = 22, high = 234),
                chance("hosidius_10", STATION_RANGE, modifierMask = HOSIDIUS_10, low = 35, high = 247)
            )
        ),
        heatCook(
            rowKey = "cooking_manta_ray",
            raw = "items.raw_mantaray",
            cooked = "items.mantaray",
            burnt = "items.burnt_mantaray",
            level = 91, xp = 216, stopBurnFire = 100, stopBurnRange = 100,
            chances = listOf(
                chance("fire", STATION_FIRE, low = 1, high = 202),
                chance("range", STATION_RANGE, low = 1, high = 222),
                chance("hosidius_5", STATION_RANGE, modifierMask = HOSIDIUS_5, low = 13, high = 234),
                chance("hosidius_10", STATION_RANGE, modifierMask = HOSIDIUS_10, low = 26, high = 247)
            )
        ),

        // ---- Special fish ----
        heatCook(
            rowKey = "cooking_karambwan",
            raw = "items.tbwt_raw_karambwan",
            cooked = "items.tbwt_cooked_karambwan",
            burnt = "items.tbwt_burnt_karambwan",
            level = 30, xp = 190, stopBurnFire = 99, stopBurnRange = 93,
            chances = listOf(
                chance("base_any", STATION_ANY, low = 70, high = 255),
                chance("hosidius_5", STATION_RANGE, modifierMask = HOSIDIUS_5, low = 82, high = 267),
                chance("hosidius_10", STATION_RANGE, modifierMask = HOSIDIUS_10, low = 95, high = 280)
            )
        ),
        heatCook(
            rowKey = "cooking_rainbow_fish",
            raw = "items.hunting_raw_fish_special",
            cooked = "items.hunting_fish_special",
            burnt = "items.burntfish2",
            level = 35, xp = 110, stopBurnFire = 64, stopBurnRange = 60,
            chances = listOf(
                chance("base_any", STATION_ANY, low = 56, high = 370),
                chance("hosidius_5", STATION_RANGE, modifierMask = HOSIDIUS_5, low = 68, high = 382),
                chance("hosidius_10", STATION_RANGE, modifierMask = HOSIDIUS_10, low = 81, high = 395)
            )
        ),
        heatCook(
            rowKey = "cooking_cave_eel",
            raw = "items.raw_cave_eel",
            cooked = "items.cave_eel",
            burnt = "items.burnt_cave_eel",
            level = 38, xp = 115, stopBurnFire = 74, stopBurnRange = 70,
            chances = listOf(
                chance("base_any", STATION_ANY, low = 38, high = 332),
                chance("hosidius_5", STATION_RANGE, modifierMask = HOSIDIUS_5, low = 50, high = 344),
                chance("hosidius_10", STATION_RANGE, modifierMask = HOSIDIUS_10, low = 63, high = 357)
            )
        ),

        // ---- Snails ----
        heatCook(
            rowKey = "cooking_thin_snail",
            raw = "items.snail_corpse1",
            cooked = "items.snail_corpse_cooked1",
            burnt = "items.burnt_snail",
            level = 12, xp = 70, stopBurnFire = 47, stopBurnRange = 47,
            stationMask = STATION_FIRE,
            chances = listOf(
                chance("base_any", STATION_ANY, low = 93, high = 444),
                chance("lumbridge", STATION_RANGE, modifierMask = LUMBRIDGE, low = 103, high = 464),
                chance("hosidius_5", STATION_RANGE, modifierMask = HOSIDIUS_5, low = 105, high = 456),
                chance("hosidius_10", STATION_RANGE, modifierMask = HOSIDIUS_10, low = 118, high = 469)
            )
        ),
        heatCook(
            rowKey = "cooking_lean_snail",
            raw = "items.snail_corpse2",
            cooked = "items.snail_corpse_cooked2",
            burnt = "items.burnt_snail",
            level = 17, xp = 80, stopBurnFire = 47, stopBurnRange = 50,
            chances = listOf(
                chance("fire", STATION_FIRE, low = 93, high = 444),
                chance("range", STATION_RANGE, low = 85, high = 428),
                chance("lumbridge", STATION_RANGE, modifierMask = LUMBRIDGE, low = 95, high = 448),
                chance("hosidius_5", STATION_RANGE, modifierMask = HOSIDIUS_5, low = 97, high = 440),
                chance("hosidius_10", STATION_RANGE, modifierMask = HOSIDIUS_10, low = 110, high = 453)
            )
        ),
        heatCook(
            rowKey = "cooking_fat_snail",
            raw = "items.snail_corpse3",
            cooked = "items.snail_corpse_cooked3",
            burnt = "items.burnt_snail",
            level = 22, xp = 95, stopBurnFire = 56, stopBurnRange = 56,
            stationMask = STATION_FIRE,
            chances = listOf(
                chance("base_any", STATION_ANY, low = 73, high = 402),
                chance("lumbridge", STATION_RANGE, modifierMask = LUMBRIDGE, low = 83, high = 422),
                chance("hosidius_5", STATION_RANGE, modifierMask = HOSIDIUS_5, low = 85, high = 414),
                chance("hosidius_10", STATION_RANGE, modifierMask = HOSIDIUS_10, low = 98, high = 427)
            )
        )
    )
}
