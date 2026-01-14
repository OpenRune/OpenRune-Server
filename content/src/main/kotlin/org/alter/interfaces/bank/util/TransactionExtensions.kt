package org.alter.interfaces.bank.util

import org.alter.game.model.inv.objtx.Transaction
import org.alter.game.model.item.Item

internal inline fun Transaction<Item>.leftShift(
    init: Transaction<Item>.LeftShiftQuery.() -> Unit
) {
    val query = LeftShiftQuery().apply(init)
    execute(query)
}

internal inline fun Transaction<Item>.rightShift(
    init: Transaction<Item>.RightShiftQuery.() -> Unit
) {
    val query = RightShiftQuery().apply(init)
    execute(query)
}

internal inline fun Transaction<Item>.bulkShift(
    init: Transaction<Item>.BulkShiftQuery.() -> Unit
) {
    val query = BulkShiftQuery().apply(init)
    execute(query)
}

internal inline fun Transaction<Item>.shiftInsert(
    init: Transaction<Item>.ShiftInsertQuery.() -> Unit
) {
    val query = ShiftInsertQuery().apply(init)
    execute(query)
}
