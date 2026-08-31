package org.rsmod.content.skills.fishing

enum class Gate {
    Ok,
    NoTool,
    NoBait,
    LevelTooLow,
}

object FishingCatchLogic {
    fun unlocked(spot: Int, method: Int, level: Int, rows: List<FishRow>): List<FishRow> =
        rows.filter { it.spot == spot && it.method == method && level >= it.level }.sortedByDescending { it.level }

    fun rollCatch(unlocked: List<FishRow>, roll: (row: FishRow) -> Boolean): FishRow? =
        unlocked.firstOrNull { roll(it) }

    fun attemptGate(
        bait: String?,
        hasTool: Boolean,
        hasBait: Boolean,
        unlocked: List<FishRow>,
    ): Gate = when {
        !hasTool -> Gate.NoTool
        bait != null && !hasBait -> Gate.NoBait
        unlocked.isEmpty() -> Gate.LevelTooLow
        else -> Gate.Ok
    }

    fun minLevel(spot: Int, method: Int, rows: List<FishRow>): Int =
        rows.filter { it.spot == spot && it.method == method }.minOf { it.level }
}
