package dtx.impl.weighted

import dtx.core.ArgMap
import dtx.core.Rollable
import dtx.core.RollResult
import dtx.table.DefaultTableBuilder
import dtx.table.TableHooks
import dtx.impl.meta.AbstractMetaRollableBuilder
import dtx.impl.meta.MetaEntryFilter
import dtx.impl.meta.MetaEntryFilterBuilder
import dtx.impl.meta.MetaRollable
import dtx.impl.meta.MetaTable
import dtx.util.NoTransform
import dtx.util.isSortedBy

public class MetaWeightedRollable<T, R>(
    public override val rollable: Rollable<T, R>,
    public override val identifier: String,
    public val initialWeight: Double,
    public val minimumWeight: Double,
    public val maximumWeight: Double,
    public var currentWeight: Double = initialWeight.coerceIn(minimumWeight, maximumWeight),
    public override val metaEntryFilters: MutableSet<MetaEntryFilter<T, R>> = mutableSetOf()
): MetaRollable<T, R>, WeightedRollable<T, R> {

    override fun selectResult(target: T, otherArgs: ArgMap): RollResult<R> {
        return rollable.selectResult(target, otherArgs)
    }

    public override val weight: Double by ::currentWeight


    public override lateinit var parentTable: MetaWeightedTable<T, R>


    public fun increaseCurrentWeightBy(amount: Double) {
        currentWeight = (currentWeight + amount).coerceIn(minimumWeight, maximumWeight)
    }


    public fun decreaseCurrentWeightBy(amount: Double) {
        currentWeight = currentWeight - amount.coerceIn(minimumWeight, maximumWeight)
    }
}


public fun <T, R> entryFilter(block: MetaEntryFilterBuilder<T, R>.() -> Unit): MetaEntryFilter<T, R> {
    return MetaEntryFilterBuilder<T, R>().apply(block).build()
}


public class MetaWeightedRollableBuilder<T, R>: AbstractMetaRollableBuilder<T, R, MetaWeightedRollable<T, R>, MetaWeightedRollableBuilder<T, R>>() {

    public var weight: Double by ::initialValue
    public var minWeight: Double by ::minValue
    public var maxWeight: Double by ::maxValue

    public fun weight(weight: Double): MetaWeightedRollableBuilder<T, R> {
        return value(weight)
    }

    public fun minWeight(weight: Double): MetaWeightedRollableBuilder<T, R> {
        return minimum(weight)
    }

    public fun maxWeight(weight: Double): MetaWeightedRollableBuilder<T, R> {
        return maximum(weight)
    }

    public override fun build(): MetaWeightedRollable<T, R> {
        return MetaWeightedRollable<T, R>(
            rollable = rollable,
            identifier = identifier,
            minimumWeight = minWeight,
            maximumWeight = maxWeight,
            initialWeight = weight,
            metaEntryFilters = filters
        )
    }
}


public class MetaWeightedTable<T, R>(
    tableName: String,
    entries: List<MetaWeightedRollable<T, R>>,
    hooks: TableHooks<T, R>,
): MetaTable<T, R>, WeightedTableImpl<T, R>(
    tableName, entries, hooks
) {
    public override val tableEntries: MutableList<MetaWeightedRollable<T, R>> = entries
        .map(NoTransform())
        .sortedBy { it.currentWeight }
        .toMutableList()

    override fun roll(target: T, otherArgs: ArgMap): RollResult<R> {

        val result = super<WeightedTableImpl>.roll(target, otherArgs)

        reSortWeights()

        return result
    }

    private fun reSortWeights(): Unit {

        if (tableEntries.isSortedBy(MetaWeightedRollable<T, R>::currentWeight)) {
            return
        }

        tableEntries.sortBy(MetaWeightedRollable<T, R>::currentWeight)
        maxRoll = tableEntries.sumOf(MetaWeightedRollable<T, R>::currentWeight)
    }

    init {
        tableEntries.forEach { it.parentTable = this }
    }
}

public class MetaWeightedTableBuilder<T, R>: DefaultTableBuilder<
        T,
        R,
        MetaWeightedRollable<T, R>,
        MetaWeightedTable<T, R>,
>() {

    override val entries: MutableList<MetaWeightedRollable<T, R>> = mutableListOf()

    public infix fun Double.weight(rollable: MetaWeightedRollable<T, R>): MetaWeightedTableBuilder<T, R> {

        addEntry(rollable)

        return this@MetaWeightedTableBuilder
    }

    public inline infix fun Double.weight(
        builder: MetaWeightedRollableBuilder<T, R>.() -> Unit
    ): MetaWeightedTableBuilder<T, R> {
        val builder = MetaWeightedRollableBuilder<T, R>()
        builder.builder()
        builder.apply { weight = this@weight }
        return weight(builder.build())
    }

    public infix fun Int.weight(rollable: MetaWeightedRollable<T, R>): MetaWeightedTableBuilder<T, R> {
        return toDouble().weight(rollable)
    }

    public inline infix fun Int.weight(builder: MetaWeightedRollableBuilder<T, R>.() -> Unit): MetaWeightedTableBuilder<T, R> {
        return toDouble().weight(builder)
    }
}

public inline fun <T, R> metaWeightedTable(
    tableName: String = "Unnamed Meta Weighted Table",
    block: MetaWeightedTableBuilder<T, R>.() -> Unit
): MetaWeightedTable<T, R> {

    val builder = MetaWeightedTableBuilder<T, R>()
    builder.apply { name(tableName) }
    builder.apply(block)

    return builder.build()
}
