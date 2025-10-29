package org.alter.game.util

import dev.openrune.ServerCacheManager
import dev.openrune.definition.type.DBRowType
import dev.openrune.definition.type.DBColumnType
import dev.openrune.definition.util.VarType
import org.alter.game.util.vars.BooleanVarType
import org.alter.game.util.vars.VarTypeImpl
import org.alter.rscm.RSCM.asRSCM
import java.util.concurrent.ConcurrentHashMap

fun <K, V> DbHelper.column(name: String, type: VarTypeImpl<K, V>): V =
    getNValue(name, type, 0)

fun row(row: String) = DbHelper.row(row)

/**
 * Splits a single column into consecutive pairs.
 *
 * Example:
 * column.values = [8, 35, 4, 30]
 * returns [[8, 35], [4, 30]]
 */

fun <K1, V1, K2, V2> DbHelper.multiColumn(
    columnName: String,
    type1: VarTypeImpl<K1, V1>,
    type2: VarTypeImpl<K2, V2>
): List<EnumPair<V1, V2>> {
    val column = getColumn(columnName)
    val values = column.column.values ?: return emptyList()
    val result = mutableListOf<EnumPair<V1, V2>>()

    var i = 0
    while (i < values.size - 1) {
        val v1 = column.get(i, type1) as V1
        val v2 = column.get(i + 1, type2) as V2
        result.add(EnumPair(v1, v2))
        i += 2
    }

    return result
}

@Suppress("UNCHECKED_CAST")
class DbHelper private constructor(private val row: DBRowType) {

    val id: Int get() = row.id
    val tableId: Int get() = row.tableId

    override fun toString(): String =
        "DbHelper(id=$id, table=$tableId, columns=${row.columns.keys.joinToString()})"

    fun <K, V> getNValue(name: String, type: VarTypeImpl<K, V>, index: Int): V {
        val value = getColumn(name).get(index, type)
        @Suppress("UNCHECKED_CAST")
        return if (type is BooleanVarType) type.convertToAny(value) as V else type.convertTo(value as K)
    }

    fun getColumn(name: String): Column {
        require(name.startsWith("column")) { "Expected 'column' prefix in $name" }
        return getColumn(name.asRSCM() and 0xFFFF)
    }

    fun getColumn(id: Int): Column {
        val col = row.columns[id] ?: throw NoSuchElementException("Column $id not found in row $id")
        return Column(col, rowId = id, columnId = id, tableId = tableId)
    }

    class Column(
        val column: DBColumnType,
        private val rowId: Int,
        val columnId: Int,
        val tableId: Int
    ) {
        val types: Array<VarType> get() = column.types

        val size: Int get() = column.values?.size ?: 0

        fun <K, V> get(index: Int = 0, type: VarTypeImpl<K, V>): Any {
            val values = column.values
                ?: throw NoSuchElementException("No values in column $columnId for row $rowId")

            if (index !in values.indices) {
                throw IndexOutOfBoundsException("Index $index out of bounds for column $columnId in row $rowId")
            }

            val actualType = types.getOrNull(index % types.size)
                ?: throw IllegalStateException("No VarType available at index $index (types size = ${types.size})")

            if (actualType != type.type) {
                throw IllegalArgumentException(
                    "Type mismatch in table '$tableId', column '$columnId', row '$rowId', index $index: " +
                            "expected '${actualType}', found '${type.type}'."
                )
            }

            return values[index]
        }

        override fun toString(): String {
            val vals = column.values?.joinToString(", ") ?: "empty"
            return "Column(id=$columnId, row=$rowId, size=$size, values=[$vals])"
        }
    }

    companion object {

        fun table(table: String): List<DbHelper> {
            require(table.startsWith("tables.")) { "Expected table reference to start with 'tables.', got '$table'" }

            return DbQueryCache.getTable(table) {
                val tableId = table.asRSCM()
                ServerCacheManager.getRows()
                    .asSequence()
                    .filter { it.value.tableId == tableId }
                    .map { DbHelper(it.value) }
                    .distinctBy { it.id }
                    .toList()
            }
        }

        fun <K, V> dbFind(column: String, value: V, type: VarTypeImpl<K, V>): List<DbHelper> {
            require(column.startsWith("columns.")) { "Expected column reference starting with 'columns.', got '$column'" }

            return DbQueryCache.getColumn(column, value, type) {
                val tableName = "tables." + column.removePrefix("columns.").substringBefore(':')
                val tableId = tableName.asRSCM()
                val columnId = column.asRSCM() and 0xFFFF

                ServerCacheManager.getRows()
                    .asSequence()
                    .filter { it.value.tableId == tableId }
                    .filter { (_, row) ->
                        val col = row.columns[columnId] ?: return@filter false
                        val values = col.values ?: return@filter false
                        values.any { raw -> type.convertTo(raw as K) == value }
                    }
                    .map { (_, row) -> DbHelper(row) }
                    .distinctBy { it.id }
                    .toList()
            }
        }

        private fun load(rowId: Int): DbHelper =
            ServerCacheManager.getDbrow(rowId)?.let(::DbHelper)
                ?: throw NoSuchElementException("DBRow $rowId not found")

        fun row(ref: String): DbHelper = load(ref.asRSCM())
        fun row(rowId: Int): DbHelper = load(rowId)
    }
}

object DbQueryCache {
    private val tableCache = ConcurrentHashMap<String, List<DbHelper>>()
    private val columnCache = ConcurrentHashMap<Triple<String, Any, VarTypeImpl<*, *>>, List<DbHelper>>()

    fun getTable(table: String, supplier: () -> List<DbHelper>): List<DbHelper> {
        return tableCache.computeIfAbsent(table) { supplier() }
    }

    fun <K, V> getColumn(column: String, value: V, type: VarTypeImpl<K, V>, supplier: () -> List<DbHelper>): List<DbHelper> {
        val key = Triple(column, value as Any, type as VarTypeImpl<*, *>)
        return columnCache.computeIfAbsent(key) { supplier() }
    }

    fun clear() {
        tableCache.clear()
        columnCache.clear()
    }
}