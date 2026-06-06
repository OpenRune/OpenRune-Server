package dtx.core

import dtx.table.DefaultTableHooksBuilder

public interface RollableHooks<T, R> {

    /**
     * This should determine whether a [Rollable] will even be included as a potential draw
     */
    public fun includeInRoll(onTarget: T, otherArgs: ArgMap): Boolean

    /**
     * This is a 1.2.0 shouldRoll inverse replacement
     */
    public fun vetoRoll(onTarget: T, otherArgs: ArgMap): Boolean

    /**
     * This will be executed after a [vetoRoll] returns true
     */
    public fun onRollVetoed(onTarget: T): RollResult<R>

    /**
     * This will be used to potentially transform a [RollResult]
     */
    public fun transformResult(withTarget: T, result: RollResult<R>): RollResult<R>


    /**
     * This is a 1.2.0 onSelect replacement.
     */
    public fun onRollCompleted(target: T, otherArgs: ArgMap, result: RollResult<R>): Unit

    public companion object {

        public fun <T, R> Default(): RollableHooks<T, R> {
            return DefaultRollableHooks as RollableHooks<T, R>
        }
    }
}

public data object DefaultRollableHooks: RollableHooks<Any?, Any?> {

    override fun includeInRoll(onTarget: Any?, otherArgs: ArgMap): Boolean {
        return true
    }

    override fun vetoRoll(onTarget: Any?, otherArgs: ArgMap): Boolean {
        return false
    }

    override fun onRollVetoed(onTarget: Any?): RollResult<Any?> {
        return RollResult.Nothing()
    }

    override fun transformResult(withTarget: Any?, result: RollResult<Any?>): RollResult<Any?> {
        return result
    }

    override fun onRollCompleted(target: Any?, otherArgs: ArgMap, result: RollResult<Any?>) {
        return Unit
    }
}

internal data class RollableHooksImpl<T, R>(
    val shouldIncludeFunc: ShouldInclude<T> = RollableHooks.Default<T, R>()::includeInRoll,
    val vetoFunc: VetoRoll<T> = RollableHooks.Default<T, R>()::vetoRoll,
    val onVetoFunc: OnVetoRoll<T, R> = RollableHooks.Default<T, R>()::onRollVetoed,
    val transformFunc: TransformResult<T, R> = RollableHooks.Default<T, R>()::transformResult,
    val onRollCompleteFunc: OnSelect<T, R> = RollableHooks.Default<T, R>()::onRollCompleted,
): RollableHooks<T, R> {

    override fun includeInRoll(onTarget: T, otherArgs: ArgMap): Boolean {
        return shouldIncludeFunc(onTarget, otherArgs)
    }

    override fun vetoRoll(onTarget: T, otherArgs: ArgMap): Boolean {
        return vetoFunc(onTarget, otherArgs)
    }

    override fun onRollVetoed(onTarget: T): RollResult<R> {
        return onVetoFunc(onTarget)
    }

    override fun transformResult(withTarget: T, result: RollResult<R>): RollResult<R> {
        return transformFunc(withTarget, result)
    }

    override fun onRollCompleted(target: T, otherArgs: ArgMap, result: RollResult<R>) {
        return onRollCompleteFunc(target, otherArgs, result)
    }
}

public abstract class AbstractRollableHooksBuilder<T, R, Hooks: RollableHooks<T, R>, Builder: AbstractRollableHooksBuilder<T, R, Hooks, Builder>> {

    public var shouldIncludeFunc: ShouldInclude<T> = RollableHooks.Default<T, R>()::includeInRoll
    public var vetoFunc: VetoRoll<T> = RollableHooks.Default<T, R>()::vetoRoll
    public var onVetoFunc: OnVetoRoll<T, R> = RollableHooks.Default<T, R>()::onRollVetoed
    public var transformFunc: TransformResult<T, R> = RollableHooks.Default<T, R>()::transformResult
    public var onRollCompleteFunc: OnSelect<T, R> = RollableHooks.Default<T, R>()::onRollCompleted

    protected var constructFunc: Builder.() -> Hooks = { error("NO RollableHooks CONSTRUCTOR FOUND FOR $this") }

    public fun construct(block: Builder.() -> Hooks): Builder {

        constructFunc = block

        return this as Builder
    }

    public open fun shouldInclude(block: ShouldInclude<T>): Builder {

        shouldIncludeFunc = block

        return this as Builder
    }

    public open fun vetoRoll(block: VetoRoll<T>): Builder {

        vetoFunc = block

        return this as Builder
    }

    public open fun onVeto(block: OnVetoRoll<T, R>): Builder {

        onVetoFunc = block

        return this as Builder
    }

    public open fun transform(block: TransformResult<T, R>): Builder {

        transformFunc = block

        return this as Builder
    }

    public open fun onRollCompleted(block: OnSelect<T, R>): Builder {

        onRollCompleteFunc = block

        return this as Builder
    }

    public open fun build(): Hooks {
        return constructFunc(this as Builder)
    }

    internal fun buildBaseRollableHooks(): RollableHooks<T, R> {
        return RollableHooksImpl(
            shouldIncludeFunc = shouldIncludeFunc,
            vetoFunc = vetoFunc,
            onVetoFunc = onVetoFunc,
            transformFunc = transformFunc,
            onRollCompleteFunc = onRollCompleteFunc
        )
    }
}

public open class DefaultRollableHooksBuilder<T, R>: AbstractRollableHooksBuilder<T, R, RollableHooks<T, R>, DefaultRollableHooksBuilder<T, R>>() {

    init {
        construct {
            buildBaseRollableHooks()
        }
    }

    internal companion object {
        internal fun <T, R> new() = {
            DefaultRollableHooksBuilder<T, R>()
        }
    }
}

public fun <T, R> rollableHooks(block: DefaultRollableHooksBuilder<T, R>.() -> Unit): RollableHooks<T, R> {

    val builder = DefaultRollableHooksBuilder<T, R>()
    builder.apply(block)

    return builder.build()
}
