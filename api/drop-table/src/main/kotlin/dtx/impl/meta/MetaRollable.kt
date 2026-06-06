package dtx.impl.meta

import dtx.core.ArgMap
import dtx.core.Rollable
import dtx.core.RollResult
import dtx.table.Table

public interface MetaEntryFilter<T, R> {

    public fun filterEntry(modifier: String): Boolean = false

    public fun modifyEntry(entry: MetaRollable<T, R>)
}

internal class MetaEntryFilterImpl<T, R>(
    val filterFunc: (String) -> Boolean,
    val modifyFunc: (MetaRollable<T, R>) -> Unit
): MetaEntryFilter<T, R> {

    override fun filterEntry(modifier: String): Boolean {
        return filterFunc(modifier)
    }

    override fun modifyEntry(entry: MetaRollable<T, R>) {
        return modifyFunc(entry)
    }
}


public class MetaEntryFilterBuilder<T, R> {

    public var filterFunc: (String) -> Boolean = { false }

    public var modifyFunc: (MetaRollable<T, R>) -> Unit = { }

    public fun filter(func: (String) -> Boolean): MetaEntryFilterBuilder<T, R> {

        filterFunc = func

        return this
    }


    public fun modify(func: (MetaRollable<T, R>) -> Unit): MetaEntryFilterBuilder<T, R> {

        modifyFunc = func

        return this
    }


    public fun build(): MetaEntryFilter<T, R> {
        return MetaEntryFilterImpl(filterFunc) { modifyFunc(it) }
    }
}


public interface MetaRollable<T, R>: Rollable<T, R> {

    public val rollable: Rollable<T, R>

    public val identifier: String

    public val metaEntryFilters: MutableSet<MetaEntryFilter<T, R>>

    public val parentTable: MetaTable<T, R>

    override fun includeInRoll(onTarget: T, otherArgs: ArgMap): Boolean {
        return rollable.includeInRoll(onTarget, otherArgs)
    }

    override fun vetoRoll(onTarget: T, otherArgs: ArgMap): Boolean {
        return rollable.vetoRoll(onTarget, otherArgs)
    }

    override fun selectResult(target: T, otherArgs: ArgMap): RollResult<R> {
        return rollable.selectResult(target, otherArgs)
    }

    override fun onRollVetoed(onTarget: T): RollResult<R> {
        return rollable.onRollVetoed(onTarget)
    }

    override fun transformResult(withTarget: T, result: RollResult<R>): RollResult<R> {
        return rollable.transformResult(withTarget, result)
    }

    override fun onRollCompleted(target: T, otherArgs: ArgMap, result: RollResult<R>) {
        return rollable.onRollCompleted(target, otherArgs, result)
    }

    public override fun roll(target: T, otherArgs: ArgMap): RollResult<R> {

        val result = rollable.roll(target, otherArgs)

        parentTable.selectEntries(target, otherArgs).forEach { otherEntry ->

            metaEntryFilters.forEach { metaFilter ->

                if (metaFilter.filterEntry(otherEntry.identifier)) {
                    metaFilter.modifyEntry(otherEntry)
                }
            }
        }

        return result
    }
}

public interface MetaTable<T, R>: Table<T, R> {

    override val tableEntries: Collection<MetaRollable<T, R>>

    override fun selectEntries(byTarget: T, otherArgs: ArgMap): Collection<MetaRollable<T, R>> {
        return super.selectEntries(byTarget, otherArgs) as Collection<MetaRollable<T, R>>
    }
}
