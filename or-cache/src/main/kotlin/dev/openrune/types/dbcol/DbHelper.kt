package dev.openrune.types.dbcol

import dev.openrune.ServerCacheManager
import dev.openrune.definition.type.DBColumnType
import dev.openrune.definition.type.DBRowType
import dev.openrune.definition.util.CacheVarLiteral
import dev.openrune.definition.util.VarType
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCM.requireRSCM
import dev.openrune.rscm.RSCMType
import java.util.concurrent.ConcurrentHashMap

public fun <T, R> DbHelper.column(name: String, codec: DbColumnCodec<T, R>): R {
    return getColumn(name).decodeAt(0, codec)
}

public fun <T, R> DbHelper.columnOptional(name: String, codec: DbColumnCodec<T, R>): R? =
    try {
        column(name, codec)
    } catch (_: DbException) {
        null
    } catch (_: RuntimeException) {
        null
    }

public fun row(row: String): DbHelper = DbHelper.row(row)

/**
 * Splits a single column into consecutive pairs.
 *
 * Example: column.values = [8, 35, 4, 30] returns [[8, 35], [4, 30]]
 */
@Suppress("UNCHECKED_CAST")
public fun <T, R> DbHelper.multiColumn(
    columnName: String,
    vararg codecs: DbColumnCodec<T, R>,
): List<R> {
    val column = getColumn(columnName)
    val values = column.column.values ?: return emptyList()
    require(codecs.isNotEmpty()) { "At least one DbColumnCodec must be provided" }
    return values.mapIndexed { i, _ ->
        val codec = codecs[i % codecs.size]
        column.decodeAt(i, codec)
    }
}

public fun <T, R> DbHelper.multiColumnOptional(
    columnName: String,
    vararg codecs: DbColumnCodec<T, R>,
): List<R?> {
    val column =
        try {
            getColumn(columnName)
        } catch (e: DbException.MissingColumn) {
            return emptyList()
        } catch (e: DbException) {
            throw e
        } catch (_: Exception) {
            return emptyList()
        }

    val values = column.column.values ?: return emptyList()
    require(codecs.isNotEmpty()) { "At least one DbColumnCodec must be provided" }

    return values.mapIndexed { i, _ ->
        val codec = codecs.getOrNull(i % codecs.size) ?: return@mapIndexed null
        try {
            column.decodeAt(i, codec)
        } catch (e: DbException) {
            throw e
        } catch (_: Exception) {
            null
        }
    }
}

/**
 * Mixed slot types in one column (cycled [codecs]); each slot is decoded with the codec for that
 * index. Values are returned as a flat list in column order (use [table.TupleN.toListOfTupleN]
 * helpers for pairs).
 */
@Suppress("UNCHECKED_CAST")
public fun DbHelper.multiColumnMixed(
    columnName: String,
    vararg codecs: DbColumnCodec<*, *>,
): List<Any?> {
    val column = getColumn(columnName)
    val values = column.column.values ?: return emptyList()
    require(codecs.isNotEmpty()) { "At least one DbColumnCodec must be provided" }
    return values.mapIndexed { i, _ ->
        val codec = codecs[i % codecs.size]
        column.decodeAt(i, codec as DbColumnCodec<Any?, Any?>)
    }
}

@Suppress("UNCHECKED_CAST")
public fun DbHelper.multiColumnMixedOptional(
    columnName: String,
    vararg codecs: DbColumnCodec<*, *>,
): List<Any?> {
    val column =
        try {
            getColumn(columnName)
        } catch (e: DbException.MissingColumn) {
            return emptyList()
        } catch (e: DbException) {
            throw e
        } catch (_: Exception) {
            return emptyList()
        }
    val values = column.column.values ?: return emptyList()
    require(codecs.isNotEmpty()) { "At least one DbColumnCodec must be provided" }
    return values.mapIndexed { i, _ ->
        val codec = codecs.getOrNull(i % codecs.size) ?: return@mapIndexed null
        try {
            column.decodeAt(i, codec as DbColumnCodec<Any?, Any?>)
        } catch (e: DbException) {
            throw e
        } catch (_: Exception) {
            null
        }
    }
}

@Suppress("UNCHECKED_CAST")
public class DbHelper(private val row: DBRowType) {

    public val id: Int
        get() = row.id

    public val tableId: Int
        get() = row.tableId

    override fun toString(): String =
        "DbHelper(id=$id, table=$tableId, columns=${row.columns.keys.joinToString()})"

    public fun getColumn(name: String): Column {
        requireRSCM(RSCMType.DBCOL, name)
        return getColumn(name.asRSCM() and 0xFFFF)
    }

    public fun getColumn(id: Int): Column {
        val col = row.columns[id] ?: throw DbException.MissingColumn(tableId, id, id)
        return Column(col, rowId = id, columnId = id, tableId = tableId)
    }

    public class Column(
        public val column: DBColumnType,
        private val rowId: Int,
        public val columnId: Int,
        public val tableId: Int,
    ) {
        public val types: Array<VarType>
            get() = column.types

        public val size: Int
            get() = column.values?.size ?: 0

        public fun <T, R> decodeAt(startIndex: Int, codec: DbColumnCodec<T, R>): R {
            val vals =
                column.values ?: throw DbException.EmptyColumnValues(tableId, rowId, columnId)

            val slotCount = codec.types.size
            if (startIndex + slotCount > vals.size) {
                throw DbException.IndexOutOfRange(tableId, rowId, columnId, startIndex, vals.size)
            }

            for (i in 0 until slotCount) {
                val colType =
                    types.getOrNull((startIndex + i) % types.size)
                        ?: throw DbException.MissingVarType(
                            tableId,
                            rowId,
                            columnId,
                            startIndex + i,
                        )
                val expected = codec.types[i]
                val actualLit = cacheLiteralForOpenRuneVarType(colType)
                if (actualLit != expected) {
                    throw DbException.TypeLiteralMismatch(
                        tableId,
                        rowId,
                        columnId,
                        expected = expected,
                        actual = actualLit,
                    )
                }
            }

            val slice = List(slotCount) { vals[startIndex + it] }
            val inputs = slice.asCodecRawValues(codec)
            return DbColumnCodec.Iterator(codec, inputs).single()
        }

        override fun toString(): String {
            val vals = column.values?.joinToString(", ") ?: "empty"
            return "Column(id=$columnId, row=$rowId, size=$size, values=[$vals])"
        }
    }

    public companion object {
        public fun table(table: String): List<DbHelper> {
            requireRSCM(RSCMType.DBTABLE, table)

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

        private fun load(rowId: Int): DbHelper =
            ServerCacheManager.getDbrow(rowId)?.let(::DbHelper)
                ?: throw DbException.MissingRow(rowId)

        public fun row(ref: String): DbHelper = load(ref.asRSCM())

        public fun row(rowId: Int): DbHelper = load(rowId)
    }
}

public object DbQueryCache {
    private val tableCache = ConcurrentHashMap<String, List<DbHelper>>()

    public fun getTable(table: String, supplier: () -> List<DbHelper>): List<DbHelper> {
        return tableCache.computeIfAbsent(table) { supplier() }
    }

    public fun clear() {
        tableCache.clear()
    }
}

public sealed class DbException(message: String) : RuntimeException(message) {

    public class MissingColumn(tableId: Int, rowId: Int, columnId: Int) :
        DbException("Column $columnId not found in row $rowId (table $tableId)")

    public class EmptyColumnValues(tableId: Int, rowId: Int, columnId: Int) :
        DbException("No values found in column $columnId (row $rowId, table $tableId)")

    public class IndexOutOfRange(tableId: Int, rowId: Int, columnId: Int, index: Int, max: Int) :
        DbException(
            "Index $index out of bounds (size=$max) in column $columnId (row $rowId, table $tableId)"
        )

    public class MissingVarType(tableId: Int, rowId: Int, columnId: Int, index: Int) :
        DbException(
            "No VarType available at index $index in column $columnId (row $rowId, table $tableId)"
        )

    public class TypeMismatch(
        tableId: Int,
        rowId: Int,
        columnId: Int,
        expected: VarType,
        actual: VarType,
    ) :
        DbException(
            "Type mismatch in table $tableId, row $rowId, column $columnId: expected $expected but found $actual"
        )

    public class TypeLiteralMismatch(
        tableId: Int,
        rowId: Int,
        columnId: Int,
        expected: CacheVarLiteral,
        actual: CacheVarLiteral,
    ) :
        DbException(
            "Type mismatch in table $tableId, row $rowId, column $columnId: " +
                "expected literal $expected but found $actual"
        )

    public class MissingRow(rowId: Int) : DbException("DBRow $rowId not found")
}
