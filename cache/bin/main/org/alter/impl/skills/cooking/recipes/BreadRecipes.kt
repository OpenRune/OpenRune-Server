package org.alter.impl.skills.cooking.recipes

import org.alter.impl.skills.cooking.ActionDef
import org.alter.impl.skills.cooking.CookingConstants.STATION_RANGE
import org.alter.impl.skills.cooking.CookingHelpers.heatCook

/**
 * Bread cooking recipes — baked on a range.
 */
object BreadRecipes {

    val recipes: List<ActionDef> = listOf(
        heatCook(
            rowKey = "cooking_bread",
            raw = "items.bread_dough",
            cooked = "items.bread",
            burnt = "items.burnt_bread",
            level = 1,
            xp = 40,
            stopBurnFire = 35,
            stopBurnRange = 35,
            stationMask = STATION_RANGE
        ),
        heatCook(
            rowKey = "cooking_pitta_bread",
            raw = "items.uncooked_pitta_bread",
            cooked = "items.pitta_bread",
            burnt = "items.burnt_pitta_bread",
            level = 58,
            xp = 40,
            stopBurnFire = 82,
            stopBurnRange = 82,
            stationMask = STATION_RANGE
        )
    )
}
