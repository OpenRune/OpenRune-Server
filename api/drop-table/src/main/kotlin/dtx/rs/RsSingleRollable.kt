package dtx.rs

import dtx.core.ArgMap
import dtx.core.RollResult
import dtx.core.Rollable
import dtx.core.RollableHooks

public class RsSingleRollable<T, R>(
    override val tableIdentifier: String,
    public val wrapped: Rollable<T, R>,
) : RSTable<T, R>, RollableHooks<T, R> by wrapped {

    override val tableEntries: Collection<Rollable<T, R>> = listOf(wrapped)

    override fun selectResult(target: T, otherArgs: ArgMap): RollResult<R> {
        return wrapped.roll(target, otherArgs)
    }

    override fun includeInRoll(onTarget: T, otherArgs: ArgMap): Boolean {
        return wrapped.includeInRoll(onTarget, otherArgs)
    }

    override fun vetoRoll(onTarget: T, otherArgs: ArgMap): Boolean {
        return wrapped.vetoRoll(onTarget, otherArgs)
    }

    override fun onRollVetoed(onTarget: T): RollResult<R> {
        return wrapped.onRollVetoed(onTarget)
    }

    override fun transformResult(withTarget: T, result: RollResult<R>): RollResult<R> {
        return wrapped.transformResult(withTarget, result)
    }

    override fun onRollCompleted(target: T, otherArgs: ArgMap, result: RollResult<R>) {
        return wrapped.onRollCompleted(target, otherArgs, result)
    }
}
