package org.alter.impl.skills.cooking.recipes

import org.alter.impl.skills.cooking.ActionDef
import org.alter.impl.skills.cooking.CookingConstants.ChanceModifier.HOSIDIUS_10
import org.alter.impl.skills.cooking.CookingConstants.ChanceModifier.HOSIDIUS_5
import org.alter.impl.skills.cooking.CookingConstants.ChanceModifier.LUMBRIDGE
import org.alter.impl.skills.cooking.CookingConstants.STATION_RANGE
import org.alter.impl.skills.cooking.CookingHelpers.chance
import org.alter.impl.skills.cooking.CookingHelpers.heatCook

/**
 * Bread cooking recipes — baked on a range.
 *
 * Chance profiles sourced from OSRS Wiki skill_chances data.
 */
object BreadRecipes {

    val recipes: List<ActionDef> = listOf(
        heatCook(
            rowKey = "cooking_bread",
            raw = "items.bread_dough",
            cooked = "items.bread",
            burnt = "items.burnt_bread",
            level = 1, xp = 40, stopBurnFire = 35, stopBurnRange = 35,
            stationMask = STATION_RANGE,
            chances = listOf(
                chance("range", STATION_RANGE, low = 118, high = 492),
                chance("lumbridge", STATION_RANGE, modifierMask = LUMBRIDGE, low = 128, high = 512),
                chance("hosidius_5", STATION_RANGE, modifierMask = HOSIDIUS_5, low = 130, high = 504),
                chance("hosidius_10", STATION_RANGE, modifierMask = HOSIDIUS_10, low = 143, high = 517)
            )
        ),
        heatCook(
            rowKey = "cooking_pitta_bread",
            raw = "items.uncooked_pitta_bread",
            cooked = "items.pitta_bread",
            burnt = "items.burnt_pitta_bread",
            level = 58, xp = 40, stopBurnFire = 82, stopBurnRange = 82,
            stationMask = STATION_RANGE,
            chances = listOf(
                chance("range", STATION_RANGE, low = 118, high = 492),
                chance("hosidius_5", STATION_RANGE, modifierMask = HOSIDIUS_5, low = 130, high = 504),
                chance("hosidius_10", STATION_RANGE, modifierMask = HOSIDIUS_10, low = 143, high = 517)
            )
        )
    )
}
