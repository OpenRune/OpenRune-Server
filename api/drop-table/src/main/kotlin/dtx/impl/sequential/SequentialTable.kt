package dtx.impl.sequential

import dtx.core.ArgMap
import dtx.core.ModifyRoll
import dtx.core.Rollable
import dtx.core.RollResult
import dtx.core.Single
import dtx.table.AbstractTableBuilder
import dtx.table.AbstractTableHooksBuilder
import dtx.table.DefaultTableHooks
import dtx.table.Table
import dtx.table.TableHooks

internal class WrappingInt(
    var currentValue: Int,
    val wrapFloor: Int,
    val wrapCeil: Int,
    val onWrapFunc: () -> Unit,
) {

    init {
        currentValue = currentValue.coerceIn(wrapFloor, wrapCeil)
    }

    fun onWrap() {
        onWrapFunc()
    }


    fun inc() {

        currentValue += 1

        if (currentValue > wrapCeil) {
            onWrapFunc()
            currentValue = wrapFloor
        }
    }
}

public interface SequentialTableHooks<T, R>: TableHooks<T, R> {

    public fun tableIsActive(table: SequentialTable<T, R>): Boolean

    public fun resetTable(table: SequentialTable<T, R>): Unit

    public companion object {
        public fun <T, R> Default(): SequentialTableHooks<T, R> {
            return DefaultSequentialTableHooks as SequentialTableHooks<T, R>
        }
    }
}


internal data object DefaultSequentialTableHooks: SequentialTableHooks<Any?, Any?>, TableHooks<Any?, Any?> by DefaultTableHooks {

    override fun tableIsActive(table: SequentialTable<Any?, Any?>): Boolean {
        return true
    }

    override fun resetTable(table: SequentialTable<Any?, Any?>): Unit {

    }
}

internal data class SequentialTableHooksImpl<T, R>(
    val baseTableHooks: TableHooks<T, R> = TableHooks.Default<T, R>(),
    val modifyRollFunc: ModifyRoll<T> = TableHooks.Default<T, R>()::modifyRoll,
    val tableIsActiveFunc: SequentialTable<T, R>.() -> Boolean = SequentialTableHooks.Default<T, R>()::tableIsActive,
    val resetTableFunc: SequentialTable<T, R>.() -> Unit = SequentialTableHooks.Default<T, R>()::resetTable
): SequentialTableHooks<T, R>, TableHooks<T, R> by baseTableHooks {

    override fun tableIsActive(table: SequentialTable<T, R>): Boolean = with(table) {
        return tableIsActiveFunc()
    }

    override fun resetTable(table: SequentialTable<T, R>): Unit = with(table) {
        return resetTableFunc()
    }
}

public class SequentialTable<T, R>(
    public override val tableIdentifier: String,
    public override val tableEntries: List<Rollable<T, R>>,
    private val hooks: SequentialTableHooks<T, R>,
): Table<T, R>, SequentialTableHooks<T, R> by hooks {

    private val pointer = WrappingInt(0, 0, tableEntries.size) {
        resetTable(this)
    }

    override fun vetoRoll(onTarget: T, otherArgs: ArgMap): Boolean {
        return tableIsActive(this) || hooks.vetoRoll(onTarget, otherArgs)
    }

    override fun selectResult(target: T, otherArgs: ArgMap): RollResult<R> {

        val selected = tableEntries[pointer.currentValue]
        pointer.inc()
        val result = selected.roll(target, otherArgs)
        onRollCompleted(target, otherArgs, result)

        return result
    }
}

public open class SequentialTableHooksBuilder<T, R>: AbstractTableHooksBuilder<T, R, SequentialTableHooks<T, R>, SequentialTableHooksBuilder<T, R>>() {

    public var tableIsActiveFunc: SequentialTable<T, R>.() -> Boolean = SequentialTableHooks.Default<T, R>()::tableIsActive
    public var resetTableFunc: SequentialTable<T, R>.() -> Unit = SequentialTableHooks.Default<T, R>()::resetTable

    public fun tableIsActive(block: SequentialTable<T, R>.() -> Boolean): SequentialTableHooksBuilder<T, R> {

        tableIsActiveFunc = block

        return this
    }

    public fun resetTable(block: SequentialTable<T, R>.() -> Unit): SequentialTableHooksBuilder<T, R> {

        resetTableFunc = block

        return this
    }

    init {
        construct {
            SequentialTableHooksImpl(
                baseTableHooks = buildBaseTableHooks(),
                tableIsActiveFunc = tableIsActiveFunc,
                resetTableFunc = resetTableFunc,
            )
        }
    }
}

public open class SequentialTableBuilder<T, R>(
    createHookBuilder: () -> SequentialTableHooksBuilder<T, R> = { SequentialTableHooksBuilder() }
): AbstractTableBuilder<
        T,
        R,
        Rollable<T, R>,
        SequentialTable<T, R>,
        SequentialTableHooks<T, R>,
        SequentialTableHooksBuilder<T, R>,
        SequentialTableBuilder<T, R>
>(createHookBuilder = createHookBuilder) {

    override val entries: MutableList<Rollable<T, R>> = mutableListOf()

    public open fun tableIsActive(block: SequentialTable<T, R>.() -> Boolean): SequentialTableBuilder<T, R> {

        hooks.tableIsActive(block)

        return this
    }

    public open fun resetTable(block: SequentialTable<T, R>.() -> Unit): SequentialTableBuilder<T, R> {

        hooks.resetTable(block)

        return this
    }

    public open fun addEntry(result: R): SequentialTableBuilder<T, R> {

        addEntry(Single(result))

        return this
    }

    init {
        construct {
            SequentialTable(
                tableIdentifier = this.tableIdentifier,
                tableEntries = entries,
                hooks = hooks.build()
            )
        }
    }
}

public fun <T, R> sequentialTable(
    tableName: String = "Unnamed Sequential Table",
    block: SequentialTableBuilder<T, R>.() -> Unit
): SequentialTable<T, R> {

    val builder = SequentialTableBuilder<T, R>()
    builder.apply { name(tableName) }
    builder.block()

    return builder.build()
}
