package dtx.impl.chance

import dtx.core.ArgMap
import dtx.core.RollResult
import dtx.core.flattenToList
import dtx.table.Table
import dtx.table.TableHooks
import dtx.util.NoTransform
import kotlin.random.Random

public interface MultiChanceTable<T, R>: Table<T, R> {

    override val tableEntries: List<ChanceRollable<T, R>>

    public val maxRollChance: Double get() = 100.0 + Double.MIN_VALUE
}

public open class MultiChanceTableImpl<T, R>(
    public override val tableIdentifier: String,
    entries: List<ChanceRollable<T, R>>,
    internal val hooks: TableHooks<T, R>,
): MultiChanceTable<T, R>, TableHooks<T, R> by hooks {

    public override val tableEntries: List<ChanceRollable<T, R>> = entries.map(NoTransform())

    public override fun selectResult(target: T, otherArgs: ArgMap): RollResult<R> {

        val entries = tableEntries.filter { it.includeInRoll(target, otherArgs) }

        if (entries.isEmpty()) {
            return RollResult.Nothing()
        }

        val modifier = rollModifier(target, baseRollFor(target))

        val filtered = entries.filter { entry ->

            if (entry.chance >= 100.0) {
                return@filter true
            }

            val roll = Random.nextDouble(0.0, maxRollChance)
            val select = (roll * modifier) <= entry.chance

            select
        }

        val rolled = filtered.map { entry -> entry.roll(target, otherArgs) }
        val flattened = rolled.flattenToList()

        return flattened
    }
}
