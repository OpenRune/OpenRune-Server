package org.rsmod.content.areas.misc.motherlode

import org.rsmod.map.CoordGrid

internal object MotherlodeMine {
    const val AREA = "area.motherlode_mine"
    const val HUD = "interface.motherlode_hud"
    const val PAYDIRT = "obj.paydirt"
    const val NUGGET = "obj.motherlode_nugget"

    const val SACK_CAPACITY = 108
    const val LARGER_SACK_CAPACITY = 189
    const val PAYDIRT_MINING_LEVEL = 30
    const val UPPER_LEVEL_MINING_LEVEL = 57

    val MAP_SQUARE_SOUTH_WEST = CoordGrid(3712, 5632, 0)
    const val MAP_SQUARE_LENGTH = 64

    val LOWER_HOPPER = CoordGrid(3748, 5672, 0)
    val UPPER_HOPPER = CoordGrid(3755, 5677, 0)

    val LADDER_BOTTOM_DEST = CoordGrid(3755, 5675, 0)
    val LADDER_TOP_DEST = CoordGrid(3755, 5672, 0)

    val FALADOR_ENTRANCE_DEST = CoordGrid(3728, 5692, 0)
    val FALADOR_EXIT_DEST = CoordGrid(3060, 9766, 0)
    val GUILD_ENTRANCE_DEST = CoordGrid(3718, 5678, 0)
    val GUILD_EXIT_DEST = CoordGrid(3054, 9744, 0)

    val PAYDIRT_CHANNEL_START = CoordGrid(3748, 5671, 0)
    val PAYDIRT_CHANNEL_END = CoordGrid(3748, 5660, 0)

    fun isUpperFloor(coords: CoordGrid): Boolean =
        coords.level == 0 && UPPER_FLOOR_ROWS[coords.z]?.any { coords.x in it } == true

    /** Bridged map tiles that form the upper floor, keyed by z, excluding the washing machine. */
    private val UPPER_FLOOR_ROWS: Map<Int, List<IntRange>> =
        buildMap {
            put(5687, listOf(3743..3744))
            put(5686, listOf(3733..3736, 3740..3747, 3757..3758))
            put(5685, listOf(3733..3748, 3750..3761))
            put(5684, listOf(3733..3763))
            put(5683, listOf(3733..3764))
            put(5682, listOf(3734..3764))
            put(5681, listOf(3735..3739, 3745..3764))
            put(5680, listOf(3747..3764))
            put(5679, listOf(3750..3765))
            put(5678, listOf(3751..3766))
            put(5677, listOf(3752..3766))
            put(5676, listOf(3752..3766))
            put(5675, listOf(3754..3766))
            put(5674, listOf(3755..3765))
            put(5673, listOf(3757..3764))
            put(5672, listOf(3759..3763))
            put(5671, listOf(3760..3763))
            for (z in 5668..5670) put(z, listOf(3761..3764))
            put(5667, listOf(3762..3764))
            put(5666, listOf(3763..3765))
            put(5665, listOf(3762..3765))
            for (z in 5663..5664) put(z, listOf(3761..3765))
            for (z in 5661..5662) put(z, listOf(3761..3764))
            put(5660, listOf(3761..3763))
            put(5659, listOf(3761..3764))
            put(5658, listOf(3761..3765))
            for (z in 5655..5657) put(z, listOf(3760..3766))
            put(5654, listOf(3763..3765))
        }

    /** Veins stored on the raised (bridged) level of the map; they deplete slower. */
    val UPPER_LEVEL_VEINS: Set<CoordGrid> =
        setOf(
            CoordGrid(3763, 5656, 0), CoordGrid(3764, 5656, 0), CoordGrid(3765, 5656, 0),
            CoordGrid(3762, 5657, 0), CoordGrid(3762, 5658, 0), CoordGrid(3763, 5662, 0),
            CoordGrid(3763, 5663, 0), CoordGrid(3764, 5665, 0), CoordGrid(3762, 5670, 0),
            CoordGrid(3763, 5670, 0), CoordGrid(3762, 5671, 0), CoordGrid(3762, 5672, 0),
            CoordGrid(3762, 5673, 0), CoordGrid(3759, 5674, 0), CoordGrid(3760, 5674, 0),
            CoordGrid(3761, 5674, 0), CoordGrid(3758, 5675, 0), CoordGrid(3761, 5675, 0),
            CoordGrid(3762, 5675, 0), CoordGrid(3759, 5676, 0), CoordGrid(3764, 5677, 0),
            CoordGrid(3765, 5677, 0), CoordGrid(3756, 5679, 0), CoordGrid(3751, 5680, 0),
            CoordGrid(3753, 5680, 0), CoordGrid(3758, 5680, 0), CoordGrid(3760, 5680, 0),
            CoordGrid(3761, 5680, 0), CoordGrid(3762, 5680, 0), CoordGrid(3751, 5681, 0),
            CoordGrid(3755, 5681, 0), CoordGrid(3758, 5681, 0), CoordGrid(3761, 5681, 0),
            CoordGrid(3763, 5681, 0), CoordGrid(3737, 5682, 0), CoordGrid(3738, 5682, 0),
            CoordGrid(3748, 5682, 0), CoordGrid(3749, 5682, 0), CoordGrid(3750, 5682, 0),
            CoordGrid(3754, 5682, 0), CoordGrid(3755, 5682, 0), CoordGrid(3759, 5682, 0),
            CoordGrid(3762, 5682, 0), CoordGrid(3763, 5682, 0), CoordGrid(3741, 5683, 0),
            CoordGrid(3742, 5683, 0), CoordGrid(3747, 5683, 0), CoordGrid(3754, 5683, 0),
            CoordGrid(3755, 5683, 0), CoordGrid(3762, 5683, 0), CoordGrid(3763, 5683, 0),
            CoordGrid(3735, 5684, 0), CoordGrid(3743, 5684, 0), CoordGrid(3745, 5684, 0),
            CoordGrid(3746, 5684, 0), CoordGrid(3752, 5684, 0), CoordGrid(3753, 5684, 0),
            CoordGrid(3756, 5684, 0), CoordGrid(3757, 5684, 0), CoordGrid(3734, 5685, 0),
            CoordGrid(3758, 5685, 0),
        )
}
