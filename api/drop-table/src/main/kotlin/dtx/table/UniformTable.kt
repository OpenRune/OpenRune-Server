package dtx.table

import dtx.core.ArgMap
import dtx.core.RollResult
import dtx.core.Rollable
import dtx.core.Single
import dtx.core.SingleRollableBuilder
import dtx.core.singleRollable

public open class UniformTable<T, R>(
    public override val tableIdentifier: String,
    public override val tableEntries: MutableCollection<Rollable<T, R>>,
    protected open val hooks: TableHooks<T, R>
): Table<T, R>, TableHooks<T, R> by hooks {

    override fun selectResult(target: T, otherArgs: ArgMap): RollResult<R> {
        return selectEntries(target, otherArgs).random().roll(target, otherArgs)
    }
}

public open class UniformTableBuilder<
        T,
        R,
        HookType: TableHooks<T, R>,
        HookBuilder: AbstractTableHooksBuilder<T, R, HookType, HookBuilder>
>: DefaultTableBuilder<T, R, Rollable<T, R>, UniformTable<T, R>>() {

    override val entries: MutableCollection<Rollable<T, R>> = mutableListOf()

    public open fun add(result: R): UniformTableBuilder<T, R, HookType, HookBuilder> {
        addEntry(Single(result))
        return this
    }

    public open fun add(block: SingleRollableBuilder<T, R>.() -> Unit): UniformTableBuilder<T, R, HookType, HookBuilder> {
        val rollable = singleRollable(block)
        addEntry(rollable)
        return this
    }

    init {
        construct {
            UniformTable(tableIdentifier, entries, hooks.build())
        }
    }
}

public fun <T, R> uniformTable(
    tableName: String = "Unnamed Uniform Table",
    block: UniformTableBuilder<T, R, TableHooks<T, R>, DefaultTableHooksBuilder<T, R>>.() -> Unit
): UniformTable<T, R> {

    val builder = UniformTableBuilder<T, R, TableHooks<T, R>, DefaultTableHooksBuilder<T, R>> ()

    builder.apply { name(tableName) }
    builder.apply(block)

    return builder.build()
}
