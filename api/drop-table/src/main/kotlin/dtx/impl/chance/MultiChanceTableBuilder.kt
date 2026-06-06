package dtx.impl.chance

import dtx.core.ResultSelector
import dtx.core.Rollable
import dtx.core.Single
import dtx.core.SingleByFun
import dtx.impl.misc.Percent
import dtx.table.AbstractTableBuilder
import dtx.table.DefaultTableHooksBuilder
import dtx.table.TableHooks

public open class MultiChanceTableBuilder<T, R>: AbstractTableBuilder<
        T,
        R,
        ChanceRollable<T, R>,
        MultiChanceTable<T, R>,
        TableHooks<T, R>,
        DefaultTableHooksBuilder<T, R>,
        MultiChanceTableBuilder<T, R>
>(createHookBuilder = DefaultTableHooksBuilder.new()) {

    init {
        construct {
            MultiChanceTableImpl(
                tableIdentifier,
                entries,
                hooks.build(),
            )
        }
    }

    override val entries: MutableList<ChanceRollable<T, R>> = mutableListOf()

    public infix fun Percent.chance(rollable: Rollable<T, R>): MultiChanceTableBuilder<T, R> {

        addEntry(ChanceRollableImpl(value, rollable))

        return this@MultiChanceTableBuilder
    }

    public infix fun Percent.chance(entry: R): MultiChanceTableBuilder<T, R> {
        return chance(Single(entry))
    }

    public infix fun Percent.chance(entryBlock: ResultSelector<T, R>): MultiChanceTableBuilder<T, R> {
        return chance(SingleByFun(entryBlock))
    }

    public infix fun Int.chance(rollable: Rollable<T, R>): MultiChanceTableBuilder<T, R> {
        return chance(rollable = rollable)
    }

    public infix fun Int.chance(entry: R): MultiChanceTableBuilder<T, R> {
        return chance(Single(entry))
    }

    public infix fun Int.chance(entryBlock: ResultSelector<T, R>): MultiChanceTableBuilder<T, R> {
        return chance(SingleByFun(entryBlock))
    }
}

public inline fun <T, R> multiChanceTable(
    tableName: String = "Unnamed Multi Chance Table",
    block: MultiChanceTableBuilder<T, R>.() -> Unit
): MultiChanceTable<T, R> {

    val builder = MultiChanceTableBuilder<T, R>()
    builder.apply { name(tableName) }
    builder.apply(block)

    return builder.build()
}
