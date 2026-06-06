package dtx.impl.weighted

import dtx.core.ArgMap
import dtx.core.RollResult
import dtx.table.Table
import dtx.table.TableHooks
import kotlin.random.Random

public interface WeightedTable<T, R>: Table<T, R> {

    public override val tableEntries: Collection<WeightedRollable<T, R>>

    public val maxRoll: Double

    public fun <E: WeightedRollable<T, R>> getWeightedEntry(
        modifier: Double,
        rolled: Double,
        byEntries: Collection<E>,
        selector: (E) -> Double = { it.weight }
    ): WeightedRollable<T, R> {

        var rolledWeight = rolled

        val reWeighted = byEntries.map { entry ->
            val modified = (selector(entry) * (1 + modifier)) to entry
            modified
        }

        for ((weight, entry) in reWeighted) {

            rolledWeight -= weight
            val select = rolledWeight <= 0.0

            if (select) {
                return entry
            }
        }

        return byEntries.last()
    }
}

public open class WeightedTableImpl<T, R>(
    public override val tableIdentifier: String,
    entries: List<WeightedRollable<T, R>>,
    private val hooks: TableHooks<T, R> = TableHooks.Default()
): WeightedTable<T, R>, TableHooks<T, R> by hooks {

    override var maxRoll: Double = entries.sumOf { it.weight }
        protected set

    public override val tableEntries: List<WeightedRollable<T, R>> = entries
        .groupBy { it.weight }
        .map { (weight, entries) ->
            if (entries.size == 1) {
                entries.first()
            } else {
                WeightedCollectionRollable(weight, entries)
            }
        }

    override fun selectResult(target: T, otherArgs: ArgMap): RollResult<R> {
        val entries = tableEntries.filter { it.includeInRoll(target, otherArgs) }
        println("selecting from $entries")
        when (entries.size) {
            0 -> { return RollResult.Nothing() }
            1 -> { return entries.first().roll(target, otherArgs) }
            else -> {

                val rollMod = modifyRoll(target, baseRollFor(target))
                val rolledWeight = Random.nextDouble(0.0, maxRoll)
                val pickedEntry = getWeightedEntry(rollMod, rolledWeight, entries)

                return pickedEntry.roll(target, otherArgs)
            }
        }
    }

    public override fun toString(): String = "WeightedTable[$tableIdentifier]"

    init {
        require(entries.isNotEmpty()) {
            "WeightedTable[$tableIdentifier] entries must not be empty"
        }
    }
}
