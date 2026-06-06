package dtx.impl.exhaustive

import dtx.core.ArgMap
import dtx.core.OnExhaust
import dtx.core.Single
import dtx.table.AbstractTableBuilder
import dtx.impl.weighted.WeightedTable
import dtx.impl.weighted.WeightedTableImpl

public abstract class ExhaustiveTableBuilder<T, R, Table: ExhaustiveTable<T, R>>: AbstractTableBuilder<
        T,
        R,
        ExhaustiveRollable<T, R>,
        Table,
        ExhaustiveTableHooks<T, R>,
        ExhaustiveTableHooksBuilder<T, R>,
        ExhaustiveTableBuilder<T, R, Table>
>(createHookBuilder = { ExhaustiveTableHooksBuilder<T, R>() }) {

    override val entries: MutableCollection<ExhaustiveRollable<T, R>> = mutableListOf()

    public open infix fun Int.rolls(rollable: dtx.core.Rollable<T, R>): ExhaustiveTableBuilder<T, R, Table> {

        addEntry(ExhaustiveRollableImpl(rollable, ExhaustiveRollableHooksImpl(), this))

        return this@ExhaustiveTableBuilder
    }

    public open infix fun Int.rolls(block: ExhaustiveRollableBuilder<T, R>.() -> Unit): ExhaustiveTableBuilder<T, R, Table> {

        val builder = ExhaustiveRollableBuilder<T, R>()

        builder.apply { rolls(this@rolls) }
        builder.apply(block)

        return addEntry(builder.build())
    }

    public open infix fun Int.rolls(result: R): ExhaustiveTableBuilder<T, R, Table> {
        return rolls(Single(result))
    }

    public open fun onExhaust(block: OnExhaust<T>): ExhaustiveTableBuilder<T, R, Table> {

        hooks.onExhaust(block)

        return this
    }
}

public open class UniformExhaustiveTableBuilder<T, R>: ExhaustiveTableBuilder<T, R, ExhaustiveTableImpl<T, R>>() {

    init {
        construct {
            ExhaustiveTableImpl<T, R>(
                tableIdentifier = tableIdentifier,
                tableEntries = entries,
                hooks = hooks.build()
            )
        }
    }

    override val entries: MutableCollection<ExhaustiveRollable<T, R>> = mutableListOf()
}

public interface WeightedExhaustiveTable<T, R>: WeightedTable<T, R>, ExhaustiveTable<T, R> {

    public override val tableEntries: List<ExhaustiveRollable<T, R>>
}

public open class WeightedExhaustiveTableImpl<T, R>(
    tableIdentifier: String,
    public override val tableEntries: List<ExhaustiveRollable<T, R>>,
    internal val hooks: ExhaustiveTableHooks<T, R>
): WeightedExhaustiveTable<T, R>, WeightedTableImpl<T, R>(
    tableIdentifier, tableEntries, hooks
) {

    override val initialRolls: Int = 1

    override val rollable: dtx.core.Rollable<T, R> = this

    override fun onExhaust(target: T) {
        return hooks.onExhaust(target)
    }

    override fun isExhausted(rollable: ExhaustiveRollable<T, R>): Boolean {
        return hooks.isExhausted(rollable)
    }

    override fun resetExhaustible(rollable: ExhaustiveRollable<T, R>) {
        return hooks.resetExhaustible(rollable)
    }

    override fun incrementExhaustible(rollable: ExhaustiveRollable<T, R>) {
        return hooks.incrementExhaustible(rollable)
    }

    override fun includeInRoll(onTarget: T, otherArgs: ArgMap): Boolean {
        return isExhausted()
    }
}

public open class WeightedExhaustiveTableBuilder<T, R>: ExhaustiveTableBuilder<T, R, WeightedExhaustiveTable<T, R>>() {

    init {
        construct {
            WeightedExhaustiveTableImpl<T, R>(
                tableIdentifier = tableIdentifier,
                entries.toList(),
                hooks.build()
            )
        }
    }

}

public fun <T, R> uniformExhaustiveTable(
    tableName: String = "Unnamed Uniform Exhaustive Table",
    block: UniformExhaustiveTableBuilder<T, R>.() -> Unit
): ExhaustiveTable<T, R> {

    val builder = UniformExhaustiveTableBuilder<T, R>()

    builder.apply { name(tableName) }
    builder.apply(block)

    return builder.build()
}

public fun <T, R> weightedExhaustiveTable(
    tableName: String = "Unnamed Weighted Exhaustive Table",
    block: WeightedExhaustiveTableBuilder<T, R>.() -> Unit
): WeightedTable<T, R> {

    val builder = WeightedExhaustiveTableBuilder<T, R>()

    builder.apply { name(tableName) }
    builder.apply(block)

    return builder.build()
}