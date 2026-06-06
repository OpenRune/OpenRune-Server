package dtx.impl.exhaustive

import dtx.core.ArgMap
import dtx.core.RollResult
import dtx.impl.weighted.WeightedRollable

public interface ExhaustiveRollable<T, R>: WeightedRollable<T, R>, ExhaustiveRollableHooks<T, R> {

    public val initialRolls: Int

    public var rolls: Int

    public override val weight: Double
        get() = rolls.toDouble()

    public override fun includeInRoll(onTarget: T, otherArgs: ArgMap): Boolean {
        return isExhausted()
    }

    public override fun roll(target: T, otherArgs: ArgMap): RollResult<R> {

        if (isExhausted(this)) {
            return RollResult.Nothing()
        }

        if (vetoRoll(target, otherArgs)) {
            return onRollVetoed(target)
        }

        val result = selectResult(target, otherArgs)
        val transformed = transformResult(target, result)
        rolls -= 1
        if (isExhausted(this)) {
            onExhaust(target)
        }

        return transformed
    }
}

public fun <T, R> ExhaustiveRollable<T, R>.isExhausted(): Boolean {
    return isExhausted(this)
}