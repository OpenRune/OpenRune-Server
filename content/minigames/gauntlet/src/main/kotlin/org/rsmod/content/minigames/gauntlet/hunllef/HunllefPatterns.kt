package org.rsmod.content.minigames.gauntlet.hunllef

import org.rsmod.content.minigames.gauntlet.layout.Tile

internal enum class HunllefStage(val number: Int) {
    One(1),
    Two(2),
    Three(3);

    companion object {
        fun forHp(fraction: Double): HunllefStage =
            when {
                fraction > TWO_THIRDS -> One
                fraction > ONE_THIRD -> Two
                else -> Three
            }

        const val TWO_THIRDS = 2.0 / 3.0
        const val ONE_THIRD = 1.0 / 3.0
    }
}

internal data class FloorTiming(val warning: Int, val hit: Int) {
    val cycle: Int
        get() = warning + hit
}

internal object HunllefPatterns {
    const val SIZE = 12

    private val QUADRANTS =
        listOf(rect(0, 0, 6, 6), rect(6, 0, 6, 6), rect(0, 6, 6, 6), rect(6, 6, 6, 6))

    private val OFFSET_SQUARES =
        listOf(rect(2, 2, 6, 6), rect(4, 2, 6, 6), rect(2, 4, 6, 6), rect(4, 4, 6, 6))

    private val STAGE_ONE = QUADRANTS + OFFSET_SQUARES

    private val STAGE_TWO =
        OFFSET_SQUARES +
            listOf(
                rect(0, 8, 12, 4),
                rect(0, 0, 12, 4),
                rect(0, 0, 4, 12),
                rect(8, 0, 4, 12),
                rect(0, 0, 12, 3) + rect(0, 9, 12, 3),
                rect(0, 0, 3, 12) + rect(9, 0, 3, 12),
            )

    private val STAGE_THREE =
        listOf(
            rect(0, 0, 4, 4) + rect(8, 0, 4, 4) + rect(0, 8, 4, 4) + rect(8, 8, 4, 4),
            rect(0, 0, 3, 3) +
                rect(9, 0, 3, 3) +
                rect(0, 9, 3, 3) +
                rect(9, 9, 3, 3) +
                rect(4, 4, 4, 4),
            rect(1, 1, 4, 4) + rect(7, 1, 4, 4) + rect(1, 7, 4, 4) + rect(7, 7, 4, 4),
            ring(2),
            rect(3, 3, 6, 6),
        )

    fun patterns(stage: HunllefStage): List<Set<Tile>> =
        when (stage) {
            HunllefStage.One -> STAGE_ONE
            HunllefStage.Two -> STAGE_TWO
            HunllefStage.Three -> STAGE_THREE
        }

    fun outerRing(): Set<Tile> = ring(1)

    fun timing(corrupted: Boolean, stage: HunllefStage, hpFraction: Double): FloorTiming =
        if (corrupted) {
            when (stage) {
                HunllefStage.One ->
                    if (hpFraction >= EARLY_FRACTION) FloorTiming(8, 14) else FloorTiming(7, 12)
                HunllefStage.Two -> FloorTiming(5, 10)
                HunllefStage.Three -> FloorTiming(3, 6)
            }
        } else {
            when (stage) {
                HunllefStage.One -> FloorTiming(9, 12)
                HunllefStage.Two -> FloorTiming(6, 11)
                HunllefStage.Three -> FloorTiming(4, 8)
            }
        }

    private const val EARLY_FRACTION = 0.9

    private fun rect(x: Int, z: Int, width: Int, height: Int): Set<Tile> =
        buildSet {
            for (dx in 0 until width) {
                for (dz in 0 until height) add(Tile(x + dx, z + dz))
            }
        }

    private fun ring(width: Int): Set<Tile> =
        buildSet {
            for (x in 0 until SIZE) {
                for (z in 0 until SIZE) {
                    val edge = minOf(x, z, SIZE - 1 - x, SIZE - 1 - z)
                    if (edge < width) add(Tile(x, z))
                }
            }
        }
}
