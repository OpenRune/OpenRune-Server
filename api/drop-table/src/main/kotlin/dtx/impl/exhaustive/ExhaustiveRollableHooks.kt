package dtx.impl.exhaustive

import dtx.core.DefaultRollableHooks
import dtx.core.RollableHooks

public interface ExhaustiveRollableHooks<T, R>: RollableHooks<T, R> {

    public fun onExhaust(target: T): Unit

    public fun isExhausted(rollable: ExhaustiveRollable<T, R>): Boolean

    public fun resetExhaustible(rollable: ExhaustiveRollable<T, R>): Unit

    public fun incrementExhaustible(rollable: ExhaustiveRollable<T, R>): Unit

    public companion object {
        public fun <T, R> Default(): ExhaustiveRollableHooks<T, R> {
            return DefaultExhaustiveRollableHooks as ExhaustiveRollableHooks<T, R>
        }
    }
}

internal data object DefaultExhaustiveRollableHooks: ExhaustiveRollableHooks<Any?, Any?>, RollableHooks<Any?, Any?> by DefaultRollableHooks {

    override fun onExhaust(target: Any?): Unit {
        return Unit
    }

    override fun isExhausted(rollable: ExhaustiveRollable<Any?, Any?>): Boolean {
        return rollable.rolls < 1
    }

    override fun resetExhaustible(rollable: ExhaustiveRollable<Any?, Any?>): Unit {
        rollable.rolls = rollable.initialRolls
    }

    override fun incrementExhaustible(rollable: ExhaustiveRollable<Any?, Any?>) {
        rollable.rolls -= 1
    }

}