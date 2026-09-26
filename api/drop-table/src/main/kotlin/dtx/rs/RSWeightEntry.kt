package dtx.rs

import dtx.core.Rollable
import dtx.impl.weighted.WeightedRollable
import dtx.impl.weighted.WeightedRollableImpl

public class RSWeightEntry<T, R>(
    public val rangeStart: Int,
    public val rangeEnd: Int,
    rollable: Rollable<T, R>,
    boosted: Boolean = false,
) : WeightedRollable<T, R> by WeightedRollableImpl(
    weight = (rangeEnd - rangeStart).toDouble(),
    rollable = rollable,
    boosted = boosted,
) {
    public infix fun checkWeight(value: Int): Boolean = value in rangeStart..<rangeEnd
}
