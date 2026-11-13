package org.rsmod.content.areas.misc.fossilunderwater

/**
 * The xp Mairin pays for a glistening tear, in tenths: quadratic in the level, half as much again
 * when the tear goes to a single skill. At 99 that is 176.4 Agility and 646.8 Thieving for both,
 * or 264.6 Agility or 970.2 Thieving alone (https://oldschool.runescape.wiki/w/Mairin).
 */
internal object MairinTraining {
    const val MAX_TEARS = 700

    private const val AGILITY_BOTH = 18
    private const val AGILITY_ALONE = 27
    private const val THIEVING_BOTH = 66
    private const val THIEVING_ALONE = 99
    private const val PER_HUNDRED = 100

    fun agilityTenths(level: Int, both: Boolean): Int =
        level * level * (if (both) AGILITY_BOTH else AGILITY_ALONE) / PER_HUNDRED

    fun thievingTenths(level: Int, both: Boolean): Int =
        level * level * (if (both) THIEVING_BOTH else THIEVING_ALONE) / PER_HUNDRED

    /** The tears of [carried] that earn xp when [requested] are handed over; the rest are lost. */
    fun paidTears(requested: Int, carried: Int): Int = minOf(requested, carried, MAX_TEARS)

    fun takenTears(requested: Int, carried: Int): Int = minOf(requested, carried)
}
