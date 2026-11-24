package org.rsmod.content.generic.locs.ladders

/**
 * The floor a "Top-Floor" or "Bottom-Floor" ladder op lands on: the furthest level, walking one
 * floor at a time from [current], that still has a ladder on the climber's tile. Stops at the first
 * gap so a ladder never skips past a floor it does not reach.
 */
object QuickFloor {
    private const val MAX_LEVEL = 3

    fun top(current: Int, hasLadder: (Int) -> Boolean): Int {
        var level = current
        while (level < MAX_LEVEL && hasLadder(level + 1)) level++
        return level
    }

    fun bottom(current: Int, hasLadder: (Int) -> Boolean): Int {
        var level = current
        while (level > 0 && hasLadder(level - 1)) level--
        return level
    }
}
