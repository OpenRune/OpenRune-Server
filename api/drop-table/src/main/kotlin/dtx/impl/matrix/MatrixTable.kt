package dtx.impl.matrix

import dtx.core.ArgMap
import dtx.core.Rollable
import dtx.core.SingleRollableBuilder
import dtx.core.RollResult
import dtx.core.Single
import dtx.core.singleRollable
import dtx.table.AbstractTableBuilder
import dtx.table.AbstractTableHooksBuilder
import dtx.table.DefaultTableHooksBuilder
import dtx.table.Table
import dtx.table.TableHooks
import dtx.util.SparseMatrix
import kotlin.random.Random

public class MatrixTable<T, R>(
    public override val tableIdentifier: String,
    matrix: SparseMatrix<Rollable<T, R>>,
    private val hooks: TableHooks<T, R>
): Table<T, R>, TableHooks<T, R> by hooks {

    private val matrix = matrix.copy(defaultValue = Rollable.Empty<T, R>())

    override fun selectResult(target: T, otherArgs: ArgMap): RollResult<R> {
        val pickedRow = Random.nextLong(matrix.rows.toLong() + 1L).toInt()
        val pickedCol = Random.nextLong(matrix.columns.toLong() + 1L).toInt()
        val entry = matrix[pickedRow, pickedCol]
        val result = entry.roll(target, otherArgs)
        return result
    }

    public override val tableEntries: Collection<Rollable<T, R>>

        get() {

            val entries = mutableListOf<Rollable<T, R>>()

            matrix.nonDefaults().forEach { (_, _, value) ->
                entries.add(value)
            }

            return entries
        }
}

public class MatrixTableBuilder<T, R, RollableType: Rollable<T, R>, HookType: TableHooks<T, R>, HookBuilder: AbstractTableHooksBuilder<T, R, HookType, HookBuilder>>(
    createHookBuilder: () -> HookBuilder
): AbstractTableBuilder<
        T,
        R,
        RollableType,
        MatrixTable<T, R>,
        HookType,
        HookBuilder,
        MatrixTableBuilder<T, R, RollableType, HookType, HookBuilder>
>(createHookBuilder) {

    private var maxRow: Int = 0
    private var maxColumn: Int = 0
    private val items = mutableMapOf<Pair<Int, Int>, Rollable<T, R>>()
    override val entries: MutableCollection<RollableType> = mutableListOf()

    init {

        val matrix = SparseMatrix<Rollable<T, R>>(
            rows = maxRow,
            columns = maxColumn,
            defaultValue = Rollable.Empty<T, R>(),
            returnDefaultOnOob = true
        )

        items.forEach { (coords, value) ->

            val (row, column) = coords
            matrix[row, column] = value
        }

        construct {
            MatrixTable(tableIdentifier, matrix, hooks.build())
        }
    }

    public fun addItemAt(
        row: Int,
        column: Int,
        value: Rollable<T, R>
    ): MatrixTableBuilder<T, R, RollableType, HookType, HookBuilder> {

        if (row > maxRow) {
            maxRow = row
        }

        if (column > maxColumn) {
            maxColumn = column
        }

        items[Pair(row, column)] = value

        return this
    }

    public fun addItemAt(
        row: Int,
        column: Int,
        value: R
    ): MatrixTableBuilder<T, R, RollableType, HookType, HookBuilder> {
        return addItemAt(row, column, Single(value))
    }

    public fun addItemAt(
        row: Int,
        column: Int,
        block: SingleRollableBuilder<T, R>.() -> Unit
    ): MatrixTableBuilder<T, R, RollableType, HookType, HookBuilder> {
        return addItemAt(row, column, singleRollable(block))
    }
}

public inline fun <T, R> matrixTable(
    tableName: String = "Unnamed Matrix Table",
    block: MatrixTableBuilder<T, R, Rollable<T, R>, TableHooks<T, R>, DefaultTableHooksBuilder<T, R> >.() -> Unit
): MatrixTable<T, R> {

    val builder = MatrixTableBuilder<T, R, Rollable<T, R>, TableHooks<T, R>, DefaultTableHooksBuilder<T, R>> { DefaultTableHooksBuilder<T, R>()}
    builder.apply { name(tableName) }
    builder.apply(block)

    return builder.build()
}
