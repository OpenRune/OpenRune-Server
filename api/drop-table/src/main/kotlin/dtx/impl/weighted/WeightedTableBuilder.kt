package dtx.impl.weighted

import dtx.core.ResultSelector
import dtx.core.Rollable
import dtx.core.Single
import dtx.core.SingleByFun
import dtx.core.SingleRollableBuilder
import dtx.core.singleRollable
import dtx.table.DefaultTableBuilder
import dtx.table.TableHooks

public open class WeightedTableBuilder<T, R, TT: WeightedTable<T, R>>(
    internal val impl: (String, Collection<WeightedRollable<T, R>>, TableHooks<T, R>) -> TT
): DefaultTableBuilder<T, R, WeightedRollable<T, R>, TT>() {

    init {
        construct {
            impl(
                tableIdentifier,
                weightedEntries,
                hooks.build()
            )
        }
    }

    override val entries: MutableCollection<WeightedRollable<T, R>> = mutableListOf()
    protected val weightedEntries: MutableList<WeightedRollable<T, R>> = mutableListOf()

    public infix fun Double.weight(rollable: Rollable<T, R>): WeightedTableBuilder<T, R, TT> {
        val weightedRollable = WeightedRollableImpl(this, rollable)
        weightedEntries.add(weightedRollable)
        return addEntry(weightedRollable) as WeightedTableBuilder<T, R, TT>
    }

    public inline infix fun Double.weight(entry: R): WeightedTableBuilder<T, R, TT> {
        return weight(Single(entry))
    }

    public infix fun Double.weightBy(selector: ResultSelector<T, R>): WeightedTableBuilder<T, R, TT> {
        return weight(SingleByFun(selector))
    }

    public infix fun Double.weight(block: SingleRollableBuilder<T, R>.() -> Unit): WeightedTableBuilder<T, R, TT> {
        return weight(singleRollable(block))
    }

    public inline infix fun Int.weight(rollable: Rollable<T, R>): WeightedTableBuilder<T, R, TT>{
        return toDouble().weight(rollable)
    }

    public inline infix fun Int.weight(item: R): WeightedTableBuilder<T, R, TT> {
        return weight(Single(item))
    }

    public infix fun Int.weightBy(selector: ResultSelector<T, R>): WeightedTableBuilder<T, R, TT> {
        return toDouble().weightBy(selector)
    }

    public infix fun Int.weight(block: SingleRollableBuilder<T, R>.() -> Unit): WeightedTableBuilder<T, R, TT> {
        return toDouble().weight(block)
    }

    internal fun absorb(other: WeightedTableBuilder<T, R, TT>) {
        weightedEntries.addAll(other.weightedEntries)
        entries.addAll(other.entries)
    }
}

public fun <T, R> weightedTable(
    tableName: String = "Unnamed Weighted Table",
    block: WeightedTableBuilder<T, R, WeightedTable<T, R>>.() -> Unit
): WeightedTable<T, R> {

    val builder = WeightedTableBuilder<T, R, WeightedTable<T, R>> { identifier, entries, hooks -> WeightedTableImpl(identifier, entries.toList(), hooks) }
    builder.apply { name(tableName) }
    builder.apply(block)

    return builder.build()
}
