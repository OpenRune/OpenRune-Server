package dtx.core

public abstract class AbstractRollableBuilder<
        T,
        R,
        RollableType: Rollable<T, R>,
        HookType: RollableHooks<T, R>,
        HookBuilder: AbstractRollableHooksBuilder<T, R, HookType, HookBuilder>,
        RollableBuilder: AbstractRollableBuilder<T, R, RollableType, HookType, HookBuilder, RollableBuilder>
>(
    createHookBuilder: () -> HookBuilder
) {

    protected val hooks: HookBuilder = createHookBuilder()
    public open var selectResultFunc: (RollableType.(T, ArgMap) -> RollResult<R>)? = null
    public open var rollFunc: RollableType.(T, ArgMap) -> RollResult<R> = Rollable<T, R>::roll
    protected var constructFunc: RollableBuilder.(HookType) -> RollableType = {
        error("NO ROLLABLE CONSTRUCTOR FOUND FOR $this")
    }

    public open fun construct(block: RollableBuilder.(HookType) -> RollableType): RollableBuilder {

        constructFunc = block

        return this as RollableBuilder
    }

    public open fun shouldInclude(block: ShouldInclude<T>): RollableBuilder {

        hooks.shouldInclude(block)

        return this as RollableBuilder
    }

    public open fun vetoRoll(block: VetoRoll<T>): RollableBuilder {

        hooks.vetoRoll(block)

        return this as RollableBuilder
    }

    public open fun onVeto(block: OnVetoRoll<T, R>): RollableBuilder {

        hooks.onVeto(block)

        return this as RollableBuilder
    }

    public open fun transform(block: TransformResult<T, R>): RollableBuilder {

        hooks.transform(block)

        return this as RollableBuilder
    }

    public open fun onRollCompleted(block: OnSelect<T, R>): RollableBuilder {

        hooks.onRollCompleted(block)

        return this as RollableBuilder
    }

    public open fun selectResult(block: RollableType.(T, ArgMap) -> RollResult<R>): RollableBuilder {

        selectResultFunc = block

        return this as RollableBuilder
    }

    public open fun selectResult(result: R): RollableBuilder {
        return selectResult {_, _ -> RollResult.Single(result) }
    }

    public open fun result(result: R): RollableBuilder {
        return selectResult(result)
    }

    public open fun roll(block: RollableType.(T, ArgMap) -> RollResult<R>): RollableBuilder {

        rollFunc = block

        return this as RollableBuilder
    }

    public open fun build(): RollableType {
        return constructFunc(this as RollableBuilder, hooks.build())
    }
}