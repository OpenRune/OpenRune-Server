package org.rsmod.content.other.consumables.food.stew

import jakarta.inject.Singleton
import org.rsmod.api.player.protect.ProtectedAccess

@Singleton
class SpicyStewEffect {
    /**
     * Capture the stew composition before the inventory transaction
     * removes the consumed stew.
     */
    fun snapshot(
        access: ProtectedAccess,
    ): SpicyStewState {
        return with(access) {
            SpicyStewState(
                red =
                    vars[SpicyStewVars.RED]
                        .coerceIn(0, MAX_SPICE_DOSES),
                yellow =
                    vars[SpicyStewVars.YELLOW]
                        .coerceIn(0, MAX_SPICE_DOSES),
                orange =
                    vars[SpicyStewVars.ORANGE]
                        .coerceIn(0, MAX_SPICE_DOSES),
                brown =
                    vars[SpicyStewVars.BROWN]
                        .coerceIn(0, MAX_SPICE_DOSES),
            )
        }
    }

    /**
     * Apply every colour independently, then clear the composition
     * belonging to the consumed stew.
     */
    fun apply(
        access: ProtectedAccess,
        state: SpicyStewState,
    ) {
        with(access) {
            try {
                applyColour(
                    doses = state.red,
                    stats = RED_STATS,
                )
                applyColour(
                    doses = state.yellow,
                    stats = YELLOW_STATS,
                )
                applyColour(
                    doses = state.orange,
                    stats = ORANGE_STATS,
                )
                applyColour(
                    doses = state.brown,
                    stats = BROWN_STATS,
                )
            } finally {
                clearCurrentStew()
            }
        }
    }

    private fun ProtectedAccess.applyColour(
        doses: Int,
        stats: Array<String>,
    ) {
        val magnitude =
            spiceMagnitude(doses)

        if (magnitude <= 0) {
            return
        }

        stats.forEach { stat ->

             // Every affected stat rolls independently within the
             // range supplied by the number of spice doses.

            val change =
                random.of(
                    minInclusive = -magnitude,
                    maxInclusive = magnitude,
                )

            when {
                change > 0 ->
                    statBoost(
                        stat = stat,
                        constant = change,
                        percent = 0,
                    )

                change < 0 ->
                    statSub(
                        stat = stat,
                        constant = -change,
                        percent = 0,
                    )
            }
        }
    }

    private fun spiceMagnitude(
        doses: Int,
    ): Int {
        return when (doses.coerceIn(0, MAX_SPICE_DOSES)) {
            1 -> 1
            2 -> 3
            3 -> 5
            else -> 0
        }
    }

    private fun ProtectedAccess.clearCurrentStew() {
        vars[SpicyStewVars.RED] = 0
        vars[SpicyStewVars.YELLOW] = 0
        vars[SpicyStewVars.ORANGE] = 0
        vars[SpicyStewVars.BROWN] = 0
    }

    private companion object {
        const val MAX_SPICE_DOSES: Int =
            3

        val RED_STATS: Array<String> =
            arrayOf(
                "stat.attack",
                "stat.strength",
                "stat.defence",
                "stat.ranged",
                "stat.magic",
            )

        val YELLOW_STATS: Array<String> =
            arrayOf(
                "stat.prayer",
                "stat.agility",
                "stat.thieving",
                "stat.slayer",
                "stat.hunter",
                "stat.sailing",
            )

        val ORANGE_STATS: Array<String> =
            arrayOf(
                "stat.smithing",
                "stat.cooking",
                "stat.crafting",
                "stat.firemaking",
                "stat.fletching",
                "stat.runecrafting",
                "stat.construction",
            )

        val BROWN_STATS: Array<String> =
            arrayOf(
                "stat.mining",
                "stat.herblore",
                "stat.fishing",
                "stat.woodcutting",
                "stat.farming",
            )
    }
}

data class SpicyStewState(
    val red: Int = 0,
    val yellow: Int = 0,
    val orange: Int = 0,
    val brown: Int = 0,
) {
    companion object {
        val EMPTY: SpicyStewState =
            SpicyStewState()
    }
}

/**
 * Shared with the future Recipe for Disaster implementation.
 *
 * CURRENT values describe the prepared stew.
 * TARGET values describe Evil Dave's requested recipe.
 */
object SpicyStewVars {
    const val RED: String =
        "varbit.hundred_dave_red"

    const val YELLOW: String =
        "varbit.hundred_dave_yellow"

    const val BROWN: String =
        "varbit.hundred_dave_brown"

    const val ORANGE: String =
        "varbit.hundred_dave_orange"

    const val RED_TARGET: String =
        "varbit.hundred_dave_red_target"

    const val YELLOW_TARGET: String =
        "varbit.hundred_dave_yellow_target"

    const val BROWN_TARGET: String =
        "varbit.hundred_dave_brown_target"

    const val ORANGE_TARGET: String =
        "varbit.hundred_dave_orange_target"
}
