package dev.openrune.codegen

import dev.openrune.ServerCacheManager
import dev.openrune.definition.type.DBColumnType
import dev.openrune.definition.type.DBRowType
import dev.openrune.definition.util.VarType
import dev.openrune.rscm.RSCM.asRSCM
import java.util.concurrent.ConcurrentHashMap

public fun row(row: String): DbHelper = DbHelper.row(row)

@Suppress("UNCHECKED_CAST")
class DbHelper(val row: DBRowType) {

    val id: Int
        get() = row.id

    val tableId: Int
        get() = row.tableId

    override fun toString(): String =
        "DbHelper(id=$id, table=$tableId, columns=${row.columns.keys.joinToString()})"

    fun getColumn(id: Int): Column {
        val col = row.columns[id] ?: throw DbException.MissingColumn(tableId, id, id)
        return Column(col, rowId = id, columnId = id, tableId = tableId)
    }

    class Column(
        val column: DBColumnType,
        private val rowId: Int,
        val columnId: Int,
        val tableId: Int,
    ) {
        val types: Array<VarType>
            get() = column.types

        val size: Int
            get() = column.values?.size ?: 0

        override fun toString(): String {
            val vals = column.values?.joinToString(", ") ?: "empty"
            return "Column(id=$columnId, row=$rowId, size=$size, values=[$vals])"
        }
    }

    companion object {

        private fun load(rowId: Int): DbHelper =
            ServerCacheManager.getDbrow(rowId)?.let(::DbHelper)
                ?: throw DbException.MissingRow(rowId)

        fun row(ref: String): DbHelper = load(ref.asRSCM())

        fun row(rowId: Int): DbHelper = load(rowId)
    }
}

object DbQueryCache {
    private val tableCache = ConcurrentHashMap<String, List<DbHelper>>()

    fun getTable(table: String, supplier: () -> List<DbHelper>): List<DbHelper> {
        return tableCache.computeIfAbsent(table) { supplier() }
    }

    fun clear() {
        tableCache.clear()
    }
}

sealed class DbException(message: String) : RuntimeException(message) {

    class MissingColumn(tableId: Int, rowId: Int, columnId: Int) :
        DbException("Column $columnId not found in row $rowId (table $tableId)")

    class MissingRow(rowId: Int) : DbException("DBRow $rowId not found")
}
