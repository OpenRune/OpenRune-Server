package dtx.rs

import dtx.core.Rollable

public data class InlineSeparateRoll<T, R>(
    val numerator: Int,
    val denominator: Int,
    val rollable: Rollable<T, R>,
    val boosted: Boolean = false,
)

public data class SeparateRollAccess(
    public val numerator: Int,
    public val denominator: Int,
)
