package org.rsmod.api.bosses.spec

/** An int computed from the boss npc's varns when the owning effect runs. */
sealed interface VarExpr {
    data class Const(val value: Int) : VarExpr

    /** The current game tick, e.g. `Now + 13` to store a deadline for [Condition.VarnExpired]. */
    data object Now : VarExpr

    data class Varn(val varn: String) : VarExpr
    data class Plus(val a: VarExpr, val b: VarExpr) : VarExpr
    data class Min(val a: VarExpr, val b: VarExpr) : VarExpr
    data class Max(val a: VarExpr, val b: VarExpr) : VarExpr

    /** Jagex bearing (0..2047) from [from] to [to]. */
    data class BearingTo(val to: TargetExpr.Single, val from: TargetExpr.Single = TargetExpr.Centre) : VarExpr
}
