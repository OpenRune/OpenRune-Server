package dtx.impl.exhaustive

import dtx.core.ArgMap
import dtx.core.RollResult
import dtx.core.Rollable

public class ExhaustiveRollableImpl<T, R>(
    public override val rollable: Rollable<T, R>,
    public val hooks: ExhaustiveRollableHooks<T, R>,
    public override val initialRolls: Int,
    public override var rolls: Int = initialRolls
): ExhaustiveRollable<T, R>, ExhaustiveRollableHooks<T, R> by hooks {

    override fun selectResult(target: T, otherArgs: ArgMap): RollResult<R> {
        return rollable.roll(target, otherArgs)
    }

    override fun includeInRoll(onTarget: T, otherArgs: ArgMap): Boolean {
        return rollable.includeInRoll(onTarget, otherArgs) && rolls > 0
    }
}