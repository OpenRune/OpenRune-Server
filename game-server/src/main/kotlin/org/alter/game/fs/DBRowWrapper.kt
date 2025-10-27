package org.alter.game.fs

import dev.openrune.definition.type.DBRowType
import org.alter.rscm.RSCM.asRSCM

class DBRowWrapper(private val row: DBRowType) {

    val id: Int get() = row.id
    val tableId: Int get() = row.tableId

    fun column(column: String): DBColumnWrapper {
        require(column.startsWith("columns")) { "Invalid RSCM table: expected a column from 'columns'" }
        val packed = column.asRSCM()
        val columnId = packed and 0xFFFF

        return column(columnId)
    }


    fun column(columnId: Int): DBColumnWrapper {
        val col = row.columns[columnId] ?: throw NoSuchElementException("Column $columnId not found in row $id")
        return DBColumnWrapper(col, row.id, columnId)
    }

    private inline fun forEachColumn(action: (columnId: Int, col: DBColumnWrapper) -> Unit) {
        row.columns.forEach { (cid, col) ->
            action(cid, DBColumnWrapper(col, row.id, cid))
        }
    }
}