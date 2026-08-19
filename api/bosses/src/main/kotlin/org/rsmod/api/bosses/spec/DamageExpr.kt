package org.rsmod.api.bosses.spec

sealed interface DamageExpr {
    data class Fixed(val value: Int) : DamageExpr

    data class Roll(val range: IntRange) : DamageExpr

    data class Accuracy(val on: DamageExpr, val miss: DamageExpr = Fixed(0)) : DamageExpr

    data class PercentOfTargetHp(val fraction: Double) : DamageExpr

    data class Min(val a: DamageExpr, val b: DamageExpr) : DamageExpr

    data class Max(val a: DamageExpr, val b: DamageExpr) : DamageExpr
}
