package dtx.impl.chain

import dtx.core.*
import dtx.table.*

public interface ChainRollableHooks<T, R> : RollableHooks<T, R> {
    public fun adjustChance(target: T, base: Int, rollChance: Int): Pair<Int, Int>
    public fun skipLink(target: T): Boolean
    public fun nextOverride(target: T, defaultNext: ChainRollable<T, R>): ChainRollable<T, R>
    public fun onLinkEvaluated(target: T, rolled: Int, threshold: Int, passed: Boolean)

    public companion object {
        public fun <T, R> Default(): ChainRollableHooks<T, R> = DefaultChainRollableHooks as ChainRollableHooks<T, R>
    }
}

public data object DefaultChainRollableHooks : ChainRollableHooks<Any?, Any?>, RollableHooks<Any?, Any?> by DefaultRollableHooks {
    override fun adjustChance(target: Any?, base: Int, rollChance: Int): Pair<Int, Int> = base to rollChance
    override fun skipLink(target: Any?): Boolean = false
    override fun nextOverride(target: Any?, defaultNext: ChainRollable<Any?, Any?>): ChainRollable<Any?, Any?> = defaultNext
    override fun onLinkEvaluated(target: Any?, rolled: Int, threshold: Int, passed: Boolean) {}
}

internal data class ChainRollableHooksImpl<T, R>(
    val baseHooks: RollableHooks<T, R> = RollableHooksImpl(),
    val adjustChanceFunc: (T, Int, Int) -> Pair<Int, Int> = ChainRollableHooks.Default<T, R>()::adjustChance,
    val skipLinkFunc: (T) -> Boolean = ChainRollableHooks.Default<T, R>()::skipLink,
    val nextOverrideFunc: (T, ChainRollable<T, R>) -> ChainRollable<T, R> = ChainRollableHooks.Default<T, R>()::nextOverride,
    val onLinkEvaluatedFunc: (T, Int, Int, Boolean) -> Unit = ChainRollableHooks.Default<T, R>()::onLinkEvaluated,
) : ChainRollableHooks<T, R>, RollableHooks<T, R> by baseHooks {
    override fun adjustChance(target: T, base: Int, rollChance: Int): Pair<Int, Int> = adjustChanceFunc(target, base, rollChance)
    override fun skipLink(target: T): Boolean = skipLinkFunc(target)
    override fun nextOverride(target: T, defaultNext: ChainRollable<T, R>): ChainRollable<T, R> = nextOverrideFunc(target, defaultNext)
    override fun onLinkEvaluated(target: T, rolled: Int, threshold: Int, passed: Boolean) = onLinkEvaluatedFunc(target, rolled, threshold, passed)
}

public open class ChainRollableHooksBuilder<T, R> : AbstractRollableHooksBuilder<T, R, ChainRollableHooks<T, R>, ChainRollableHooksBuilder<T, R>>() {

    public var adjustChanceFunc: (T, Int, Int) -> Pair<Int, Int> = ChainRollableHooks.Default<T, R>()::adjustChance
    public var skipLinkFunc: (T) -> Boolean = ChainRollableHooks.Default<T, R>()::skipLink
    public var nextOverrideFunc: (T, ChainRollable<T, R>) -> ChainRollable<T, R> = ChainRollableHooks.Default<T, R>()::nextOverride
    public var onLinkEvaluatedFunc: (T, Int, Int, Boolean) -> Unit = ChainRollableHooks.Default<T, R>()::onLinkEvaluated

    public fun adjustChance(block: (T, Int, Int) -> Pair<Int, Int>): ChainRollableHooksBuilder<T, R> { adjustChanceFunc = block; return this }
    public fun skipLink(block: (T) -> Boolean): ChainRollableHooksBuilder<T, R> { skipLinkFunc = block; return this }
    public fun nextOverride(block: (T, ChainRollable<T, R>) -> ChainRollable<T, R>): ChainRollableHooksBuilder<T, R> { nextOverrideFunc = block; return this }
    public fun onLinkEvaluated(block: (T, Int, Int, Boolean) -> Unit): ChainRollableHooksBuilder<T, R> { onLinkEvaluatedFunc = block; return this }

    init {
        construct {
            ChainRollableHooksImpl(
                baseHooks = buildBaseRollableHooks(),
                adjustChanceFunc = adjustChanceFunc,
                skipLinkFunc = skipLinkFunc,
                nextOverrideFunc = nextOverrideFunc,
                onLinkEvaluatedFunc = onLinkEvaluatedFunc
            )
        }
    }

    internal companion object { internal fun <T, R> new() = { ChainRollableHooksBuilder<T, R>() } }
}

public interface ChainedTableHooks<T, R> : TableHooks<T, R> {
    public fun onChainStart(target: T)
    public fun onChainEnd(target: T, result: RollResult<R>)
    public fun onEachLink(target: T, link: ChainRollable<T, R>, rolled: Int, threshold: Int, passed: Boolean)

    public companion object {
        public fun <T, R> Default(): ChainedTableHooks<T, R> = DefaultChainedTableHooks as ChainedTableHooks<T, R>
    }
}

public data object DefaultChainedTableHooks : ChainedTableHooks<Any?, Any?>, TableHooks<Any?, Any?> by DefaultTableHooks {
    override fun onChainStart(target: Any?) {}
    override fun onChainEnd(target: Any?, result: RollResult<Any?>) {}
    override fun onEachLink(target: Any?, link: ChainRollable<Any?, Any?>, rolled: Int, threshold: Int, passed: Boolean) {}
}

internal data class ChainedTableHooksImpl<T, R>(
    val baseHooks: TableHooks<T, R> = DefaultTableHooksBuilder<T, R>().build(),
    val onChainStartFunc: (T) -> Unit = ChainedTableHooks.Default<T, R>()::onChainStart,
    val onChainEndFunc: (T, RollResult<R>) -> Unit = ChainedTableHooks.Default<T, R>()::onChainEnd,
    val onEachLinkFunc: (T, ChainRollable<T, R>, Int, Int, Boolean) -> Unit = ChainedTableHooks.Default<T, R>()::onEachLink,
) : ChainedTableHooks<T, R>, TableHooks<T, R> by baseHooks {
    override fun onChainStart(target: T) = onChainStartFunc(target)
    override fun onChainEnd(target: T, result: RollResult<R>) = onChainEndFunc(target, result)
    override fun onEachLink(target: T, link: ChainRollable<T, R>, rolled: Int, threshold: Int, passed: Boolean) = onEachLinkFunc(target, link, rolled, threshold, passed)
}

public open class ChainedTableHooksBuilder<T, R> : AbstractTableHooksBuilder<T, R, ChainedTableHooks<T, R>, ChainedTableHooksBuilder<T, R>>() {

    public var onChainStartFunc: (T) -> Unit = ChainedTableHooks.Default<T, R>()::onChainStart
    public var onChainEndFunc: (T, RollResult<R>) -> Unit = ChainedTableHooks.Default<T, R>()::onChainEnd
    public var onEachLinkFunc: (T, ChainRollable<T, R>, Int, Int, Boolean) -> Unit = ChainedTableHooks.Default<T, R>()::onEachLink

    public fun onChainStart(block: (T) -> Unit): ChainedTableHooksBuilder<T, R> { onChainStartFunc = block; return this }
    public fun onChainEnd(block: (T, RollResult<R>) -> Unit): ChainedTableHooksBuilder<T, R> { onChainEndFunc = block; return this }
    public fun onEachLink(block: (T, ChainRollable<T, R>, Int, Int, Boolean) -> Unit): ChainedTableHooksBuilder<T, R> { onEachLinkFunc = block; return this }

    init {
        construct {
            ChainedTableHooksImpl(
                baseHooks = buildBaseTableHooks(),
                onChainStartFunc = onChainStartFunc,
                onChainEndFunc = onChainEndFunc,
                onEachLinkFunc = onEachLinkFunc
            )
        }
    }

    internal companion object { internal fun <T, R> new() = { ChainedTableHooksBuilder<T, R>() } }
}
