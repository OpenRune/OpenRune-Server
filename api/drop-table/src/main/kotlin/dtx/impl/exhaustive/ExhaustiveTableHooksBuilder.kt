package dtx.impl.exhaustive

import dtx.table.AbstractTableHooksBuilder

public open class ExhaustiveTableHooksBuilder<T, R>: AbstractTableHooksBuilder<T, R, ExhaustiveTableHooks<T, R>, ExhaustiveTableHooksBuilder<T, R>>() {

    protected val internalHooks: ExhaustiveRollableHooksBuilder<T, R> = ExhaustiveRollableHooksBuilder<T, R>()

    public open fun onExhaust(block: (T) -> Unit): ExhaustiveTableHooksBuilder<T, R> {

        internalHooks.onExhaust(block)

        return this
    }

    public open fun isExhausted(block: ExhaustiveRollable<T, R>.() -> Boolean): ExhaustiveTableHooksBuilder<T, R> {

        internalHooks.isExhausted(block)

        return this
    }

    public open fun resetExhaustible(block: ExhaustiveRollable<T, R>.() -> Unit): ExhaustiveTableHooksBuilder<T, R> {

        internalHooks.resetExhaustible(block)

        return this
    }

    public open fun incrementExhaustible(block: ExhaustiveRollable<T, R>.() -> Unit): ExhaustiveTableHooksBuilder<T, R> {

        internalHooks.incrementExhaustible(block)

        return this
    }

    init {
        construct {
            ExhaustiveTableHooksImpl(
                baseTableHooks = buildBaseTableHooks(),
                baseRollableHooks = internalHooks.build()
            )
        }
    }
}