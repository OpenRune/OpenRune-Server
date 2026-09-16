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
        coords.level == 0 && UpperFloorRow.forZ(coords.z)?.contains(coords.x) == true

    /**
     * The upper floor is a bridge deck drawn one plane above the mine, so the tiles it covers are
     * the only way to tell the two floors apart at runtime. One entry per map row, taken from the
     * raised tiles of the map square, excluding the washing machine beside the lower hopper.
     */
    internal enum class UpperFloorRow(val z: Int, private vararg val columns: IntRange) {
        Row5654(5654, 3763..3765),
        Row5655(5655, 3760..3766),
        Row5656(5656, 3760..3766),
        Row5657(5657, 3760..3766),
        Row5658(5658, 3761..3765),
        Row5659(5659, 3761..3764),
        Row5660(5660, 3761..3763),
        Row5661(5661, 3761..3764),
        Row5662(5662, 3761..3764),
        Row5663(5663, 3761..3765),
        Row5664(5664, 3761..3765),
        Row5665(5665, 3762..3765),
        Row5666(5666, 3763..3765),
        Row5667(5667, 3762..3764),
        Row5668(5668, 3761..3764),
        Row5669(5669, 3761..3764),
        Row5670(5670, 3761..3764),
        Row5671(5671, 3760..3763),
        Row5672(5672, 3759..3763),
        Row5673(5673, 3757..3764),
        Row5674(5674, 3755..3765),
        Row5675(5675, 3754..3766),
        Row5676(5676, 3752..3766),
        Row5677(5677, 3752..3766),
        Row5678(5678, 3751..3766),
        Row5679(5679, 3750..3765),
        Row5680(5680, 3747..3764),
        Row5681(5681, 3735..3739, 3745..3764),
        Row5682(5682, 3734..3764),
        Row5683(5683, 3733..3764),
        Row5684(5684, 3733..3763),
        Row5685(5685, 3733..3748, 3750..3761),
        Row5686(5686, 3733..3736, 3740..3747, 3757..3758),
        Row5687(5687, 3743..3744);

        fun contains(x: Int): Boolean = columns.any { x in it }

        companion object {
            private val byZ = entries.associateBy(UpperFloorRow::z)

            fun forZ(z: Int): UpperFloorRow? = byZ[z]
        }
    }
}
