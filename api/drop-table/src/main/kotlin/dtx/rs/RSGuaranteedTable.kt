package dtx.rs

import dtx.core.Rollable
import dtx.core.Single
import dtx.core.SingleRollableBuilder
import dtx.core.singleRollable
import dtx.impl.chance.ChanceRollableImpl
import dtx.impl.chance.MultiChanceTable
import dtx.impl.chance.MultiChanceTableImpl
import dtx.table.AbstractTableBuilder
import dtx.table.DefaultTableHooksBuilder
import dtx.table.TableHooks

public class RSGuaranteedTable<T, R>(
    tableIdentifier: String,
    tableEntries: Collection<Rollable<T, R>>,
    tableHooks: TableHooks<T, R> = TableHooks.Default(),
) : RSTable<T, R>, MultiChanceTable<T, R> by MultiChanceTableImpl(
    tableIdentifier,
    tableEntries.map { ChanceRollableImpl(100.0, it) },
    tableHooks,
) {
    public companion object {
        private val EmptyTable = RSGuaranteedTable<Any?, Any?>("", emptyList())

        @Suppress("UNCHECKED_CAST")
        public fun <T, R> Empty(): RSGuaranteedTable<T, R> = EmptyTable as RSGuaranteedTable<T, R>
    }
}

public class RSGuaranteedTableBuilder<T, R> :
    AbstractTableBuilder<
        T,
        R,
        Rollable<T, R>,
        RSGuaranteedTable<T, R>,
        TableHooks<T, R>,
        DefaultTableHooksBuilder<T, R>,
        RSGuaranteedTableBuilder<T, R>,
        >(DefaultTableHooksBuilder.new()) {

    override val entries: MutableCollection<Rollable<T, R>> = mutableListOf()

    public fun add(rollable: Rollable<T, R>): RSGuaranteedTableBuilder<T, R> {
        addEntry(rollable)
        return this
    }

    public fun add(block: SingleRollableBuilder<T, R>.() -> Unit): RSGuaranteedTableBuilder<T, R> {
        val rollable = singleRollable(block)
        addEntry(rollable)
        return this
    }

    public fun add(item: R): RSGuaranteedTableBuilder<T, R> {
        add(Single(item))
        return this
    }

    init {
        construct {
            RSGuaranteedTable(tableIdentifier, entries, hooks.build())
        }
    }
}

public fun <T, R> rsGuaranteedTable(block: RSGuaranteedTableBuilder<T, R>.() -> Unit): RSGuaranteedTable<T, R> {
    val builder = RSGuaranteedTableBuilder<T, R>()
    builder.apply(block)
    return builder.build()
}

public fun <T, R> guaranteedSingle(result: R): RSGuaranteedTable<T, R> = rsGuaranteedTable {
    add(result)
}
