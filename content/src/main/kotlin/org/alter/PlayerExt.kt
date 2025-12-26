package org.alter

import org.alter.game.model.entity.Player
import org.alter.game.model.inv.Inventory
import org.alter.game.model.inv.invtx.invSwap
import org.alter.game.model.inv.objtx.TransactionResultList
import org.alter.game.model.item.Item

public fun Player.invMoveToSlot(
    from: Inventory,
    into: Inventory,
    fromSlot: Int,
    intoSlot: Int,
    strict: Boolean = true,
): TransactionResultList<Item> {
    val resolvedInto = if (from === into) null else into
    return invSwap(
        from = from,
        into = resolvedInto,
        fromSlot = fromSlot,
        intoSlot = intoSlot,
        strict = strict,
    )
}