package dtx.impl.exhaustive

import dtx.core.AbstractRollableHooksBuilder

public open class ExhaustiveRollableHooksBuilder<T, R>: AbstractRollableHooksBuilder<
        T,
        R,
        ExhaustiveRollableHooks<T, R>,
        ExhaustiveRollableHooksBuilder<T, R>
>() {

    public var onExhaustFunc: (T) -> Unit = { }
    public var isExhaustedFunc: ExhaustiveRollable<T, R>.() -> Boolean = { rolls <= 0 }
    public var resetExhaustibleFunc: ExhaustiveRollable<T, R>.() -> Unit = { error("resetExhaustible not set for $this") }
    public var incrementExhaustibleFunc: ExhaustiveRollable<T, R>.() -> Unit = { error("incrementExhaustible not set for $this") }

    public open fun onExhaust(block: (T) -> Unit): ExhaustiveRollableHooksBuilder<T, R> {

        onExhaustFunc = block

        return this
    }

    public open fun isExhausted(block: ExhaustiveRollable<T, R>.() -> Boolean): ExhaustiveRollableHooksBuilder<T, R> {

        isExhaustedFunc = block

        return this
    }

    public open fun resetExhaustible(block: ExhaustiveRollable<T, R>.() -> Unit): ExhaustiveRollableHooksBuilder<T, R> {

        resetExhaustibleFunc = block

        return this
    }

    public open fun incrementExhaustible(block: ExhaustiveRollable<T, R>.() -> Unit): ExhaustiveRollableHooksBuilder<T, R> {

        incrementExhaustibleFunc = block

        return this
    }

    init {
        construct {
            ExhaustiveRollableHooksImpl(
                baseRollableHooks = buildBaseRollableHooks(),
                onExhaustFunc = onExhaustFunc,
                isExhaustedFunc = isExhaustedFunc,
                resetExhaustibleFunc = resetExhaustibleFunc,
                incrementExhaustibleFunc = incrementExhaustibleFunc
            )
        }
    }
}