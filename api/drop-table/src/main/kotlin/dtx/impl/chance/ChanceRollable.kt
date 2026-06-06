package dtx.impl.chance

import dtx.core.ArgMap
import dtx.core.RollResult
import dtx.core.Rollable
import dtx.core.RollableHooks

public interface ChanceRollable<T, R>: Rollable<T, R> {

    public val chance: Double
    public val rollable: Rollable<T, R>

    public operator fun component1(): Double {
        return chance
    }

    public operator fun component2(): Rollable<T, R> {
        return rollable
    }

    private data object Empty: ChanceRollable<Any?, Any?> {

        override fun includeInRoll(onTarget: Any?, otherArgs: ArgMap): Boolean {
            return false
        }

        override fun selectResult(target: Any?, otherArgs: ArgMap): RollResult<Any?> {
            return rollable.roll(target, otherArgs)
        }

        override val chance: Double = 0.0

        override val rollable: Rollable<Any?, Any?> = Rollable.Empty()

        override fun vetoRoll(onTarget: Any?, otherArgs: ArgMap): Boolean {
            return true
        }

        override fun onRollVetoed(onTarget: Any?): RollResult<Any?> {
            return RollResult.Nothing()
        }

        override fun transformResult(withTarget: Any?, result: RollResult<Any?>): RollResult<Any?> {
            return result
        }

        override fun onRollCompleted(target: Any?, otherArgs: ArgMap, result: RollResult<Any?>) {
            // Do nothing
        }
    }

    public companion object {

        public fun <T, R> Empty(): ChanceRollable<T, R> {
            return Empty as ChanceRollable<T, R>
        }
    }
}

public class ChanceRollableImpl<T, R>(
    override val chance: Double,
    override val rollable: Rollable<T, R>,
    private val hooks: RollableHooks<T, R> = RollableHooks.Default()
): ChanceRollable<T, R>, RollableHooks<T, R> by hooks {

    override fun selectResult(target: T, otherArgs: ArgMap): RollResult<R> {
        return rollable.roll(target, otherArgs)
    }
}
