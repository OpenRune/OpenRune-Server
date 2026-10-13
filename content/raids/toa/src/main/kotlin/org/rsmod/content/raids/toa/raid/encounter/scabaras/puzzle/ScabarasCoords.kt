package org.rsmod.content.raids.toa.raid.encounter.scabaras.puzzle

import org.rsmod.map.CoordGrid

internal enum class PuzzleType {
    PILLAR,
    SUM,
    MEMORY,
    LIGHT,
}

internal object ScabarasCoords {
    private const val LEVEL = 0

    const val SLOT_SIZE = 7
    const val SLOT_COUNT = 4

    val BASES =
        listOf(
            CoordGrid(3539, 5283, LEVEL),
            CoordGrid(3556, 5283, LEVEL),
            CoordGrid(3539, 5271, LEVEL),
            CoordGrid(3556, 5271, LEVEL),
        )

    val GATES =
        listOf(
            CoordGrid(3549, 5274, LEVEL),
            CoordGrid(3564, 5274, LEVEL),
            CoordGrid(3549, 5286, LEVEL),
            CoordGrid(3564, 5286, LEVEL),
        )

    val LABELS = intArrayOf(1, 3, 2, 4)
    const val FINAL_LABEL = 5

    val PREREQUISITE = intArrayOf(-1, 2, -1, 0)

    val EXIT_BARRIER = (5272..5288).map { CoordGrid(3575, it, LEVEL) }

    val BUTTON_OFFSET = -1 to 5

    val MEMORY_PLATES =
        listOf(2 to 4, 1 to 3, 2 to 2, 3 to 5, 3 to 3, 3 to 1, 4 to 4, 4 to 2, 5 to 3)

    val SUM_PLATES =
        mapOf(
            1 to listOf(1 to 1, 4 to 4, 5 to 4),
            2 to listOf(2 to 2, 4 to 2, 4 to 5),
            3 to listOf(2 to 5, 3 to 3, 5 to 1),
            4 to listOf(4 to 1, 5 to 2, 3 to 4),
            5 to listOf(3 to 1, 5 to 3),
            6 to listOf(1 to 2, 2 to 3, 2 to 4),
            7 to listOf(1 to 4, 3 to 2, 3 to 5),
            8 to listOf(1 to 3, 4 to 3),
            9 to listOf(1 to 5, 2 to 1, 5 to 5),
        )

    val LIGHT_RING =
        listOf(1 to 1, 1 to 3, 1 to 5, 3 to 5, 5 to 5, 5 to 3, 5 to 1, 3 to 1)

    val OBELISKS = listOf(1 to 6, 3 to 6, 5 to 6, 1 to 0, 3 to 0)
    val OBELISK_PRELIT = 5 to 0

    val FINAL_SOUTH = CoordGrid(3568, 5271, LEVEL)
    val FINAL_NORTH = CoordGrid(3568, 5283, LEVEL)
    val FINAL_PLATES =
        listOf(1 to 1, 3 to 1, 5 to 1, 1 to 3, 3 to 3, 5 to 3, 1 to 5, 3 to 5, 5 to 5)

    val SOLO_SOLVED_SYMBOLS = setOf(1, 3, 5, 7)

    val SCARAB_SPAWNS_NORTH = CoordGrid(3534, 5286, LEVEL)
    val SCARAB_SPAWNS_SOUTH = CoordGrid(3534, 5274, LEVEL)

    val HOLE_LOWER = CoordGrid(3548, 5277, LEVEL)
    val HOLE_UPPER = CoordGrid(3548, 5283, LEVEL)
    val HOLE_LOWER_EXIT = CoordGrid(3548, 5284, LEVEL)
    val HOLE_UPPER_EXIT = CoordGrid(3548, 5276, LEVEL)

    val STONE = CoordGrid(3560, 5280, LEVEL)
    val STONE_NORTH_SIDE = CoordGrid(3560, 5283, LEVEL)
    val STONE_SOUTH_SIDE = CoordGrid(3560, 5277, LEVEL)

    fun CoordGrid.plus(offset: Pair<Int, Int>): CoordGrid = translate(offset.first, offset.second)

    fun slotOf(static: CoordGrid): Int {
        for (slot in 0 until SLOT_COUNT) {
            val base = BASES[slot]
            val dx = static.x - base.x
            val dz = static.z - base.z
            if (dx in -1 until SLOT_SIZE && dz in 0 until SLOT_SIZE) return slot
        }
        return -1
    }
}
