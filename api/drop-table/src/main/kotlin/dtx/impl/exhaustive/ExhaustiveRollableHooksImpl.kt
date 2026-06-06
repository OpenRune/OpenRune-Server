package dtx.impl.exhaustive

import dtx.core.RollableHooks

public class ExhaustiveRollableHooksImpl<T, R>(
    private val baseRollableHooks: RollableHooks<T, R> = RollableHooks.Default<T, R>(),
    private val onExhaustFunc: (T) -> Unit = ExhaustiveRollableHooks.Default<T, R>()::onExhaust,
    private val isExhaustedFunc: ExhaustiveRollable<T, R>.() -> Boolean = ExhaustiveRollableHooks.Default<T, R>()::isExhausted,
    private val resetExhaustibleFunc: ExhaustiveRollable<T, R>.() -> Unit = ExhaustiveRollableHooks.Default<T, R>()::resetExhaustible,
    private val incrementExhaustibleFunc: ExhaustiveRollable<T, R>.() -> Unit = ExhaustiveRollableHooks.Default<T, R>()::incrementExhaustible,
): ExhaustiveRollableHooks<T, R>, RollableHooks<T, R> by baseRollableHooks {

    public override fun onExhaust(target: T): Unit {
        return onExhaustFunc(target)
    }

    public override fun isExhausted(rollable: ExhaustiveRollable<T, R>): Boolean {
        return isExhaustedFunc(rollable)
    }

    public override fun resetExhaustible(rollable: ExhaustiveRollable<T, R>): Unit {
        return resetExhaustibleFunc(rollable)
    }

    public override fun incrementExhaustible(rollable: ExhaustiveRollable<T, R>): Unit {
        return incrementExhaustibleFunc(rollable)
    }
}