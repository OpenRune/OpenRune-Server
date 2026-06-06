package dtx.impl.chain

import dtx.core.Rollable
import dtx.core.SingleRollableBuilder
import dtx.core.singleRollable


public open class ChainRollableBuilder<T, R, TT : ChainRollable<T, R>>(
    private val impl: (base: Int, rollChance: Int, next: ChainRollable<T, R>, rollable: Rollable<T, R>, hooks: ChainRollableHooks<T, R>) -> TT
) : dtx.core.AbstractRollableBuilder<
        T,
        R,
        TT,
        ChainRollableHooks<T, R>,
        ChainRollableHooksBuilder<T, R>,
        ChainRollableBuilder<T, R, TT>
        >(createHookBuilder = ChainRollableHooksBuilder.new()) {

    public var baseChance: Int = 1
    public var rollChance: Int = 100
    public var rollable: Rollable<T, R> = Rollable.Empty()

    public fun baseChance(baseChance: Int): ChainRollableBuilder<T, R, TT> {
        this.baseChance = baseChance
        return this
    }

    public fun rollChance(rollChance: Int): ChainRollableBuilder<T, R, TT> {
        this.rollChance = rollChance
        return this
    }

    public infix fun Int.outOf(rollChance: Int): ChainRollableBuilder<T, R, TT> {
        baseChance(this)
        rollChance(rollChance)
        return this@ChainRollableBuilder
    }

    public fun rollable(rollable: Rollable<T, R>): ChainRollableBuilder<T, R, TT> {
        this.rollable = rollable
        return this
    }

    public fun roll(block: SingleRollableBuilder<T, R>.() -> Unit): ChainRollableBuilder<T, R, TT> {
        return rollable(singleRollable(block))
    }

    init {
        construct { hooks ->
            impl(baseChance, rollChance, ChainEnd(), rollable, hooks)
        }
    }
}

public fun <T, R> chainRollable(
    baseChance: Int = 1,
    rollChance: Int = 100,
    rollable: Rollable<T, R> = Rollable.Empty(),
    block: ChainRollableBuilder<T, R, ChainRollable<T, R>>.() -> Unit = {}
): ChainRollable<T, R> {
    val builder = ChainRollableBuilder<T, R, ChainRollable<T, R>> { base, max, next, roll, hooks ->
        ChainRollableImpl(base, max, next, roll, hooks)
    }
    builder.baseChance = baseChance
    builder.rollChance = rollChance
    builder.rollable = rollable
    builder.apply(block)
    return builder.build()
}
