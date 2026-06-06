package dtx.impl.chain

import dtx.core.ArgMap
import dtx.core.RollResult
import dtx.core.Rollable
import kotlin.random.Random

public interface ChainRollable<T, R> : Rollable<T, R>, ChainRollableHooks<T, R>, Collection<ChainRollable<T, R>> {
    public val base: Int
    public val rollChance: Int
    public val next: ChainRollable<T, R>

    public fun collect(): List<ChainRollable<T, R>>

    override val size: Int get() = collect().size

    override fun isEmpty(): Boolean {
        return this === ChainEnd
    }

    override fun contains(element: ChainRollable<T, R>): Boolean {
        if (this === element) return true
        return collect().contains(element)
    }

    override fun iterator(): Iterator<ChainRollable<T, R>> = collect().iterator()

    override fun containsAll(elements: Collection<ChainRollable<T, R>>): Boolean = collect().containsAll(elements)
}

public data object ChainEnd : ChainRollable<Any?, Any?>, ChainRollableHooks<Any?, Any?> by DefaultChainRollableHooks {
    override val base: Int = 0
    override val rollChance: Int = 0
    override val next: ChainRollable<Any?, Any?> = this

    override fun collect(): List<ChainRollable<Any?, Any?>> = emptyList()

    override fun selectResult(target: Any?, otherArgs: ArgMap): RollResult<Any?> {
        return RollResult.Nothing()
    }

    @Suppress("UNCHECKED_CAST")
    public operator fun <T, R> invoke(): ChainRollable<T, R> = this as ChainRollable<T, R>
}

public open class ChainRollableImpl<T, R>(
    public override val base: Int,
    public override val rollChance: Int,
    public override val next: ChainRollable<T, R>,
    public val rollable: Rollable<T, R>,
    public val hooks: ChainRollableHooks<T, R> = ChainRollableHooks.Default()
) : ChainRollable<T, R>, ChainRollableHooks<T, R> by hooks {

    private val collected: List<ChainRollable<T, R>> = buildList {
        var current: ChainRollable<T, R> = this@ChainRollableImpl
        while (current !== ChainEnd) {
            add(current)
            current = current.next
        }
    }

    override fun collect(): List<ChainRollable<T, R>> = collected

    override fun selectResult(target: T, otherArgs: ArgMap): RollResult<R> {
        if (rollChance <= 0) {
            return next.roll(target, otherArgs)
        }
        val rolled = Random.nextInt(0, rollChance)
        val threshold = base.coerceIn(0, rollChance)
        return if (rolled < threshold) {
            rollable.roll(target, otherArgs)
        } else {
            next.roll(target, otherArgs)
        }
    }

    override fun toString(): String = "ChainRollable[$base/$rollChance -> $next]"
}