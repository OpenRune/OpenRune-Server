package dtx.rs

import dtx.core.ArgMap
import dtx.core.RollResult
import dtx.impl.weighted.WeightedRollable
import dtx.impl.weighted.WeightedTable
import dtx.table.TableHooks
import kotlin.random.Random

public class RSWeightedTable<T, R>(
    public override val tableIdentifier: String,
    public override val tableEntries: Collection<WeightedRollable<T, R>>,
    private val hooks: TableHooks<T, R> = TableHooks.Default(),
    public val inlineSeparateRolls: List<InlineSeparateRoll<T, R>> = emptyList(),
) : RSTable<T, R>, WeightedTable<T, R>, TableHooks<T, R> by hooks {

    override fun selectEntries(byTarget: T, otherArgs: ArgMap): List<RSWeightEntry<T, R>> = buildList {
        var total = 0

        tableEntries.forEach {
            if (it.includeInRoll(byTarget, otherArgs)) {
                val upper = total + it.weight
                val entry = RSWeightEntry(total, upper.toInt(), it.rollable)
                total = entry.rangeEnd
                add(entry)
            }
        }
    }

    public override val maxRoll: Double
        get() = tableEntries.maxOf { it.weight }

    override fun selectResult(target: T, otherArgs: ArgMap): RollResult<R> {
        val entries = selectEntries(target, otherArgs)

        if (tableEntries.isEmpty()) {
            return RollResult.Nothing()
        }

        if (tableEntries.size == 1) {
            return tableEntries.first().roll(target, otherArgs)
        }

        val localMax = entries.maxOf { it.rangeEnd }

        val baseRoll = Random.nextInt(0, localMax)
        val flatMod = rollModifier(target, 0.0)

        val roll = (baseRoll * flatMod).toInt()

        entries.forEach { entry ->
            if (entry checkWeight roll) {
                return entry.roll(target, otherArgs)
            }
        }

        return RollResult.Nothing()
    }

    public companion object {
        private val EmptyTable = RSWeightedTable<Any?, Any?>("", emptyList())

        @Suppress("UNCHECKED_CAST")
        public fun <T, R> Empty(): RSWeightedTable<T, R> = EmptyTable as RSWeightedTable<T, R>
    }
}
