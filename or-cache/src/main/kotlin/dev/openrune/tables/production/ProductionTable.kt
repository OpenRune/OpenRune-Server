package dev.openrune.tables.production

import dev.openrune.definition.dbtables.DBRowBuilder
import dev.openrune.definition.dbtables.DBTable
import dev.openrune.definition.dbtables.DBTableBuilder
import dev.openrune.definition.dbtables.dbTable

class ProductionTableRowScope(
    private val row: DBRowBuilder,
    private val includeCategory: Boolean,
    private val requireOutput: Boolean,
    private val defaultCategory: String?,
) {
    fun production(block: ProductionRowBuilder.() -> Unit) {
        ProductionRowBuilder(row, includeCategory, requireOutput, defaultCategory).apply(block).apply()
    }

    fun column(col: Int, vararg values: Any) {
        row.column(col, *values)
    }

    fun columnRSCM(col: Int, vararg ids: String) {
        row.columnRSCM(col, *ids)
    }
}

class ProductionTableScope(
    private val table: DBTableBuilder,
    private val includeCategory: Boolean,
    private val requireOutput: Boolean,
    private val defaultCategory: String?,
) {
    fun row(rowId: String, block: ProductionTableRowScope.() -> Unit) {
        table.row(rowId) {
            ProductionTableRowScope(this, includeCategory, requireOutput, defaultCategory).block()
        }
    }

    fun row(rowId: Int, block: ProductionTableRowScope.() -> Unit) {
        table.row(rowId) {
            ProductionTableRowScope(this, includeCategory, requireOutput, defaultCategory).block()
        }
    }
}

fun productionTable(
    tableId: String,
    serverOnly: Boolean = false,
    includeCategory: Boolean = true,
    requireOutput: Boolean = true,
    defaultCategory: String? = null,
    extraColumns: DBTableBuilder.() -> Unit = {},
    block: ProductionTableScope.() -> Unit,
): DBTable =
    dbTable(tableId, serverOnly = serverOnly) {
        rowType(
            "production",
            buildMap {
                put("production_include_category", includeCategory.toString())
                put("production_require_output", requireOutput.toString())
                if (defaultCategory != null) put("production_default_category", defaultCategory)
            },
        )
        ProductionColumns.register(this, includeCategory)
        extraColumns()
        ProductionTableScope(this, includeCategory, requireOutput, defaultCategory).block()
    }

fun productionTable(
    tableId: Int,
    serverOnly: Boolean = false,
    includeCategory: Boolean = true,
    requireOutput: Boolean = true,
    defaultCategory: String? = null,
    extraColumns: DBTableBuilder.() -> Unit = {},
    block: ProductionTableScope.() -> Unit,
): DBTable =
    dbTable(tableId, serverOnly = serverOnly) {
        rowType(
            "production",
            buildMap {
                put("production_include_category", includeCategory.toString())
                put("production_require_output", requireOutput.toString())
                if (defaultCategory != null) put("production_default_category", defaultCategory)
            },
        )
        ProductionColumns.register(this, includeCategory)
        extraColumns()
        ProductionTableScope(this, includeCategory, requireOutput, defaultCategory).block()
    }
