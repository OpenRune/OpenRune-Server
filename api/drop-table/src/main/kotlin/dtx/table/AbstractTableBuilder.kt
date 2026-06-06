package dtx.table

import dtx.core.AbstractRollableBuilder
import dtx.core.Rollable

/**
 * Abstract builder for Table objects.
 * 
 * @param T the type of the target
 * @param R the type of the result
 * @param TableType the type of the Table being built
 * @param HookType the type of the TableHooks to put in the TableType
 * @param HookBuilder the type of the TableHooksBuilder being used
 * @param BuilderType the type of the builder itself
 * @param createHookBuilder a function that creates a new HookBuilder
 */
public abstract class AbstractTableBuilder<
    T,
    R,
    RollableType: Rollable<T, R>,
    TableType: Table<T, R>,
    HookType: TableHooks<T, R>,
    HookBuilder: AbstractTableHooksBuilder<T, R, HookType, HookBuilder>,
    BuilderType: AbstractTableBuilder<T, R, RollableType, TableType, HookType, HookBuilder, BuilderType>
>(
    createHookBuilder: () -> HookBuilder
): AbstractRollableBuilder<
        T,
        R,
        TableType,
        HookType,
        HookBuilder,
        BuilderType
>(createHookBuilder = createHookBuilder) {

    public var tableIdentifier: String = "Unnamed Table"
    protected abstract val entries: MutableCollection<RollableType>

    public open fun name(name: String): BuilderType {

        this.tableIdentifier = name

        return this as BuilderType
    }

    public open fun addEntry(entry: RollableType): BuilderType {

        this.entries.add(entry)

        return this as BuilderType
    }

    public open fun getBaseDropRate(block: (T) -> Double): BuilderType {

        hooks.baseRollFor(block)

        return this as BuilderType
    }

    public open fun modifyRoll(block: (T, Double) -> Double): BuilderType {

        hooks.modifyRollFor(block)

        return this as BuilderType
    }
}

public abstract class DefaultTableBuilder<
    T,
    R,
    RT: Rollable<T, R>,
    TableType: Table<T, R>
> : AbstractTableBuilder<
        T,
        R,
        RT,
        TableType,
        TableHooks<T, R>,
        DefaultTableHooksBuilder<T, R>,
        DefaultTableBuilder<T, R, RT, TableType>
>({ DefaultTableHooksBuilder() })
