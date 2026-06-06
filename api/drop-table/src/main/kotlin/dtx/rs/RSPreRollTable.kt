package dtx.rs

import dtx.core.Rollable
import dtx.impl.chance.ChanceRollable
import dtx.impl.chance.MultiChanceTable
import dtx.impl.chance.MultiChanceTableBuilder
import dtx.impl.chance.MultiChanceTableImpl
import dtx.impl.misc.Percent
import dtx.table.TableHooks

public class RSPreRollTable<T, R>(
    tableIdentifier: String,
    tableEntries: List<ChanceRollable<T, R>>,
    tableHooks: TableHooks<T, R> = TableHooks.Default(),
) : RSTable<T, R>, MultiChanceTable<T, R> by MultiChanceTableImpl(
    tableIdentifier,
    tableEntries,
    tableHooks,
) {
    public companion object {
        private val EmptyTable = RSPreRollTable<Any?, Any?>("", emptyList())

        @Suppress("UNCHECKED_CAST")
        public fun <T, R> Empty(): RSPreRollTable<T, R> = EmptyTable as RSPreRollTable<T, R>
    }
}

public class RSPrerollTableBuilder<T, R> : MultiChanceTableBuilder<T, R>() {

    public infix fun Int.outOf(other: Int): Percent = Percent((toDouble() / other.toDouble()) * 100.0)

    public infix fun Percent.rolls(rollable: Rollable<T, R>): RSPrerollTableBuilder<T, R> {
        chance(rollable)
        return this@RSPrerollTableBuilder
    }

    public infix fun Percent.rolls(entry: R): RSPrerollTableBuilder<T, R> {
        chance(entry)
        return this@RSPrerollTableBuilder
    }

    init {
        construct {
            RSPreRollTable(
                tableIdentifier,
                entries,
                hooks.build(),
            )
        }
    }
}

public fun <T, R> rsPrerollTable(block: RSPrerollTableBuilder<T, R>.() -> Unit): RSPreRollTable<T, R> {
    val builder = RSPrerollTableBuilder<T, R>()
    builder.apply(block)
    return builder.build() as RSPreRollTable<T, R>
}

public fun <T, R> rsTertiaryTable(block: RSPrerollTableBuilder<T, R>.() -> Unit): RSPreRollTable<T, R> {
    return rsPrerollTable(block)
}
