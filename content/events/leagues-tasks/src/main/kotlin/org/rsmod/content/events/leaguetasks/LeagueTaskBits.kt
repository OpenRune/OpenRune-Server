package org.rsmod.content.events.leaguetasks

/**
 * A task's completion flag is bit `index % 32` of `varp.league_task_completed_{index / 32}`, where
 * `index` is the task's key in the league's task enum (`[proc,script9470]`).
 */
internal object LeagueTaskBits {
    const val BITS_PER_VARP = 32
    const val VARP_COUNT = 62
    const val CAPACITY = BITS_PER_VARP * VARP_COUNT

    fun varp(index: Int): Int = index / BITS_PER_VARP

    fun bit(index: Int): Int = index % BITS_PER_VARP

    fun isSet(packed: Int, index: Int): Boolean = packed and mask(index) != 0

    fun set(packed: Int, index: Int): Int = packed or mask(index)

    fun clear(packed: Int, index: Int): Int = packed and mask(index).inv()

    private fun mask(index: Int): Int = 1 shl bit(index)
}
