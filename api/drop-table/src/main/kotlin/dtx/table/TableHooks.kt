package dtx.table

import dtx.core.AbstractRollableHooksBuilder
import dtx.core.BaseRoll
import dtx.core.DefaultRollableHooks
import dtx.core.ModifyRoll
import dtx.core.RollableHooks
import dtx.core.RollableHooksImpl

public interface TableHooks<T, R>: RollableHooks<T, R> {

    /**
     * This is a getBaseDropRate replacement
     */
    public fun baseRollFor(target: T): Double

    /**
     * This is a rollModifier replacement
     */
    public fun modifyRoll(target: T, baseRoll: Double): Double

    public companion object {

        public fun <T, R> Default(): TableHooks<T, R> {
            return DefaultTableHooks as TableHooks<T, R>
        }
    }
}

public data object DefaultTableHooks: TableHooks<Any?, Any?>, RollableHooks<Any?, Any?> by DefaultRollableHooks {

    override fun baseRollFor(target: Any?): Double {
        return 0.0
    }

    override fun modifyRoll(target: Any?, baseRoll: Double): Double {
        return baseRoll
    }
}

internal data class TableHooksImpl<T, R>(
    val baseRollableHooks: RollableHooks<T, R> = RollableHooksImpl(),
    val baseRollFunc: BaseRoll<T> = TableHooks.Default<T, R>()::baseRollFor,
    val modifyRollFunc: ModifyRoll<T> = TableHooks.Default<T, R>()::modifyRoll,
): TableHooks<T, R>, RollableHooks<T, R> by baseRollableHooks {

    override fun baseRollFor(target: T): Double {
        return baseRollFunc(target)
    }

    override fun modifyRoll(target: T, baseRoll: Double): Double {
        return modifyRollFunc(target, baseRoll)
    }
}

public abstract class AbstractTableHooksBuilder<T, R, Hooks: TableHooks<T, R>, Builder: AbstractTableHooksBuilder<T, R, Hooks, Builder>> : AbstractRollableHooksBuilder<T, R, Hooks, Builder>() {

    public var baseRollFunc: BaseRoll<T> = TableHooks.Default<T, R>()::baseRollFor
    public var modifyRollFunc: ModifyRoll<T> = TableHooks.Default<T, R>()::modifyRoll

    public open fun baseRollFor(block: BaseRoll<T>): Builder {

        baseRollFunc = block

        return this as Builder
    }

    public open fun modifyRollFor(block: ModifyRoll<T>): Builder {

        modifyRollFunc = block

        return this as Builder
    }

    internal fun buildBaseTableHooks(): TableHooks<T, R> {
        return TableHooksImpl(
            baseRollableHooks = buildBaseRollableHooks(),
            baseRollFunc = baseRollFunc,
            modifyRollFunc = modifyRollFunc
        )
    }
}

public open class DefaultTableHooksBuilder<T, R>: AbstractTableHooksBuilder<T, R, TableHooks<T, R>, DefaultTableHooksBuilder<T, R>>() {

    init {
        construct {
            buildBaseTableHooks()
        }
    }

    internal companion object {
        internal fun <T, R> new() = {
            DefaultTableHooksBuilder<T, R>()
        }
    }
}
