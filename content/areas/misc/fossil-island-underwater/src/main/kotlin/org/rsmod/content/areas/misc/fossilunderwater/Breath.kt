package org.rsmod.content.areas.misc.fossilunderwater

/**
 * The oxygen meter, `fossil_underwater_oxygen`, kept in tenths of a percent. Every [CYCLE] ticks
 * underwater it drains, or fills while the player floats in an air pocket.
 */
internal object Breath {
    const val FULL = 1000
    const val CYCLE = 3
    const val NIBBLE = 20
    const val PUFFERFISH = 150

    private const val DRAIN = 10
    private const val AIR_POCKET = 50

    val TEARS_TAKEN = 10..20

    fun breathe(oxygen: Int, inAirPocket: Boolean): Int =
        if (inAirPocket) restore(oxygen, AIR_POCKET) else lose(oxygen, DRAIN)

    fun lose(oxygen: Int, amount: Int): Int = (oxygen - amount).coerceAtLeast(0)

    fun restore(oxygen: Int, amount: Int): Int = (oxygen + amount).coerceAtMost(FULL)

    fun tearsTaken(carried: Int, roll: Int): Int = minOf(carried, roll)
}
