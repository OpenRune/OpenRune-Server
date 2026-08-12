package org.rsmod.content.skills.fishing.configs

/**
 * MVP Draynor small-net fishing rates.
 *
 * Success rates use Mining-style low/high integers for [org.rsmod.api.player.stat.statRandom].
 * XP values are whole OSRS XP (shrimp 10, anchovies 40) — same convention as
 * [org.rsmod.content.skills.mining.configs.miningXp] / woodcutting after fine÷10.
 *
 * Catch choice at 15+: on a successful roll, if fishing level ≥ 15, pick shrimp or anchovies
 * with equal weight. Below 15, shrimp only. (Not separate success tables.)
 */
object FishingRates {
    const val NET_ANIM = "seq.human_smallnet"
    const val NET_OBJ = "obj.net"
    const val ACTION_DELAY = 5

    val shrimp =
        FishCatch(
            item = "obj.raw_shrimp",
            level = 1,
            xp = 10.0,
            successLow = 24,
            successHigh = 96,
        )

    val anchovies =
        FishCatch(
            item = "obj.raw_anchovies",
            level = 15,
            xp = 40.0,
            successLow = 24,
            successHigh = 96,
        )

    /** Spot success uses shrimp rates for the shared small-net roll. */
    fun spotSuccessRates(): Pair<Int, Int> = shrimp.successLow to shrimp.successHigh

    fun resolveCatch(fishingLevel: Int, rollAnchovies: Boolean): FishCatch {
        if (fishingLevel >= anchovies.level && rollAnchovies) {
            return anchovies
        }
        return shrimp
    }

    fun canFish(fishingLevel: Int): Boolean = fishingLevel >= shrimp.level
}

data class FishCatch(
    val item: String,
    val level: Int,
    val xp: Double,
    val successLow: Int,
    val successHigh: Int,
)
