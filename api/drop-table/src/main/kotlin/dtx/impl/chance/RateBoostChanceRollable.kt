package dtx.impl.chance

import dtx.core.ArgMap
import dtx.core.RollResult
import dtx.core.Rollable
import dtx.core.RollableHooks
import kotlin.math.floor

public object RateBoosts {
    @Volatile public var multiplier: (Any?, ArgMap) -> Double = { _, _ -> 1.0 }

    public fun multiplierFor(target: Any?, otherArgs: ArgMap): Double =
        multiplier(target, otherArgs).coerceAtLeast(MIN_MULTIPLIER)

    private const val MIN_MULTIPLIER: Double = 0.0001
}

/** Effective chance is `numerator / floor(denominator / multiplier)`. */
public class RateBoostChanceRollable<T, R>(
    private val numerator: Int,
    private val denominator: Int,
    override val rollable: Rollable<T, R>,
    private val multiplier: (T, ArgMap) -> Double = { target, args ->
        RateBoosts.multiplierFor(target, args)
    },
    private val hooks: RollableHooks<T, R> = RollableHooks.Default(),
) : ChanceRollable<T, R>, RollableHooks<T, R> by hooks {

    override val chance: Double = numerator.toDouble() / denominator.toDouble() * 100.0

    override fun chanceFor(target: T, otherArgs: ArgMap): Double {
        val boost = multiplier(target, otherArgs)
        if (boost == 1.0) return chance
        val adjustedDenom = floor(denominator / boost).toInt().coerceAtLeast(1)
        return (numerator.toDouble() / adjustedDenom.toDouble() * 100.0).coerceAtMost(100.0)
    }

    override fun selectResult(target: T, otherArgs: ArgMap): RollResult<R> =
        rollable.roll(target, otherArgs)
}
