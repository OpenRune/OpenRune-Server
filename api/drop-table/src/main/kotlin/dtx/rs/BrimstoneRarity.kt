package dtx.rs

public fun brimstoneRarityDenominator(combatLevel: Int, konarTaskBonus: Boolean): Int {
    val base =
        when {
            combatLevel >= 350 -> 50
            combatLevel >= 100 -> 120 - kotlin.math.floor(combatLevel * 0.2).toInt()
            else -> 100 + kotlin.math.floor(0.2 * (combatLevel - 100) * (combatLevel - 100)).toInt()
        }
    val adjusted = kotlin.math.floor(base * if (konarTaskBonus) 0.8 else 1.0).toInt()
    return adjusted.coerceAtLeast(1)
}
