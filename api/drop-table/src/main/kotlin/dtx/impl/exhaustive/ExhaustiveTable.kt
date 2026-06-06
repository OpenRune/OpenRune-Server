package dtx.impl.exhaustive

import dtx.core.ArgMap
import dtx.core.RollResult
import dtx.core.Rollable
import dtx.table.Table

public interface ExhaustiveTable<T, R>: Table<T, R>, ExhaustiveRollable<T, R> {

    override val tableEntries: Collection<ExhaustiveRollable<T, R>>

    override var rolls: Int
        get() = tableEntries.sumOf(ExhaustiveRollable<T, R>::rolls)
        set(value) { }
}

public open class ExhaustiveTableImpl<T, R>(
    public override val tableIdentifier: String,
    public override val tableEntries: MutableCollection<ExhaustiveRollable<T, R>>,
    hooks: ExhaustiveTableHooks<T, R>
): ExhaustiveTable<T, R>, ExhaustiveTableHooks<T, R> by hooks {

    public override val rollable: Rollable<T, R> = this

    override fun selectResult(target: T, otherArgs: ArgMap): RollResult<R> {

        if (vetoRoll(target, otherArgs)) {
            return RollResult.Nothing()
        }

        if (isExhausted()) {
            return RollResult.Nothing()
        }

        val selectableEntries = tableEntries.filterNot(ExhaustiveRollable<T, R>::isExhausted)

        val selectedEntry = selectableEntries.random()

        val result = selectedEntry.roll(target, otherArgs)

        if (isExhausted()) {
            onExhaust(target)
        }

        return result
    }

    override val initialRolls: Int
        get() = tableEntries.sumOf { it.initialRolls }

    override var rolls: Int
        get() = tableEntries.sumOf { it.rolls }
        set(value) { }

}