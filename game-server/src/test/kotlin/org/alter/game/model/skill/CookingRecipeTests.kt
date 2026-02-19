package org.alter.game.model.skill

import kotlin.test.Test
import kotlin.test.assertTrue
import org.alter.impl.skills.cooking.CookingConstants
import org.alter.impl.skills.cooking.CookingRecipeRegistry

class CookingRecipeTests {

    @Test
    fun `heat-source recipes have chance profiles`() {
        val actions = CookingRecipeRegistry.allRecipes
            .filter { it.trigger == CookingConstants.Trigger.HEAT_SOURCE }

        actions.forEach { action ->
            val stationMask = action.stationMask
            if ((stationMask and CookingConstants.STATION_FIRE) != 0) {
                assertTrue(
                    action.chances.any { it.low > 0 && it.high > 0 && (it.stationMask and CookingConstants.STATION_FIRE) != 0 },
                    "Missing fire chance profile for action ${action.rowId}"
                )
            }
            if ((stationMask and CookingConstants.STATION_RANGE) != 0) {
                assertTrue(
                    action.chances.any { it.low > 0 && it.high > 0 && (it.stationMask and CookingConstants.STATION_RANGE) != 0 },
                    "Missing range chance profile for action ${action.rowId}"
                )
            }
        }
    }
}
