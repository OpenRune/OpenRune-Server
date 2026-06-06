package dtx.impl.exhaustive

import dtx.core.AbstractRollableBuilder
import dtx.core.ArgMap
import dtx.core.OnExhaust
import dtx.core.OnSelect
import dtx.core.RollResult
import dtx.core.Rollable
import dtx.core.ShouldInclude
import dtx.core.SingleRollableBuilder
import dtx.core.TransformResult
import dtx.core.VetoRoll

public open class ExhaustiveRollableBuilder<T, R>: AbstractRollableBuilder<
        T,
        R,
        ExhaustiveRollable<T, R>,
        ExhaustiveRollableHooks<T, R>,
        ExhaustiveRollableHooksBuilder<T, R>,
        ExhaustiveRollableBuilder<T, R>
>(
    createHookBuilder = { ExhaustiveRollableHooksBuilder<T, R>() }
) {

    private val wrappedRollableBuilder = SingleRollableBuilder<T, R>()

    public open var rolls: Int = 1

    init {
        construct {
            ExhaustiveRollableImpl(
                wrappedRollableBuilder.build(),
                hooks.build(),
                rolls
            )
        }
    }

    public override fun shouldInclude(block: ShouldInclude<T>): ExhaustiveRollableBuilder<T, R> {

        wrappedRollableBuilder.shouldInclude(block)

        return this
    }

    public override fun vetoRoll(block: VetoRoll<T>): ExhaustiveRollableBuilder<T, R> {

        wrappedRollableBuilder.vetoRoll(block)

        return this
    }

    public override fun transform(block: TransformResult<T, R>): ExhaustiveRollableBuilder<T, R> {

        wrappedRollableBuilder.transform(block)

        return this
    }

    public override fun onRollCompleted(block: OnSelect<T, R>): ExhaustiveRollableBuilder<T, R> {

        wrappedRollableBuilder.onRollCompleted(block)

        return this
    }

    public override fun selectResult(block: ExhaustiveRollable<T, R>.(T, ArgMap) -> RollResult<R>): ExhaustiveRollableBuilder<T, R> {

        wrappedRollableBuilder.selectResult(block as (Rollable<T, R>.(T, ArgMap) -> RollResult<R>))

        return this
    }

    public open fun onExhaust(block: OnExhaust<T>): ExhaustiveRollableBuilder<T, R> {

        hooks.onExhaust(block)

        return this
    }

    public open fun rolls(amount: Int): ExhaustiveRollableBuilder<T, R> {

        rolls = amount

        return this
    }
}