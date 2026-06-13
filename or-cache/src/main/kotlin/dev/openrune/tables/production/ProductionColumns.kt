package dev.openrune.tables.production

import dev.openrune.definition.dbtables.DBTableBuilder
import dev.openrune.definition.util.VarType

object ProductionColumns {
    const val COL_INPUT = 0
    const val COL_STAT_REQ = 1
    const val COL_XP = 2
    const val COL_OUTPUT = 3
    const val COL_CATEGORY = 4
    const val COL_INPUT_AMOUNT = 5
    const val COL_OUTPUT_AMOUNT = 6

    internal fun register(table: DBTableBuilder, includeCategory: Boolean = true) {
        table.column("input", COL_INPUT, VarType.OBJ)
        table.column("stat_req", COL_STAT_REQ, VarType.STAT, VarType.INT)
        table.column("xp", COL_XP, VarType.INT)
        table.column("output", COL_OUTPUT, VarType.OBJ)
        table.column("category", COL_CATEGORY, VarType.STRING)
        table.column("input_amount", COL_INPUT_AMOUNT, VarType.INT)
        table.column("output_amount", COL_OUTPUT_AMOUNT, VarType.INT)
    }
}
