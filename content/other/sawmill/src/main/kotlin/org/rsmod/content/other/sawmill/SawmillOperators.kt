package org.rsmod.content.other.sawmill

import org.rsmod.api.table.SawmillOperatorsRow

object SawmillOperators {
    val all: List<SawmillOperatorsRow>
        get() = SawmillOperatorsRow.all()

    val lumberYard: SawmillOperatorsRow
        get() = SawmillOperatorsRow.getRow("dbrow.sawmill_operator_lumber_yard")

    val prifddinas: SawmillOperatorsRow
        get() = SawmillOperatorsRow.getRow("dbrow.sawmill_operator_prifddinas")

    val auburnvale: SawmillOperatorsRow
        get() = SawmillOperatorsRow.getRow("dbrow.sawmill_operator_auburnvale")
}

fun SawmillOperatorsRow.isSameOperator(other: SawmillOperatorsRow): Boolean = rowId == other.rowId
