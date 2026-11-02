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

    /** The odds that at least one fish is caught when each is rolled in turn. */
    fun attemptChance(rates: Collection<Double>): Double =
        1.0 - rates.fold(1.0) { miss, rate -> miss * (1.0 - rate.coerceIn(0.0, 1.0)) }

    /**
     * The fish for an attempt once its outcome is settled. [unlocked] is highest level first, so a
     * success forced with nothing rolled gives the lowest level fish rather than the best one.
     */
    fun settleCatch(unlocked: List<FishRow>, rolled: FishRow?, success: Boolean): FishRow? =
        when {
            !success -> null
            rolled != null -> rolled
            else -> unlocked.lastOrNull()
        }

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
