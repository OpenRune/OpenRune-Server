package org.alter.api.ext

import org.alter.api.Skills
import org.alter.game.model.attr.INTERACTING_ITEM_SLOT
import org.alter.game.model.attr.OTHER_ITEM_SLOT_ATTR
import org.alter.game.model.container.ItemContainer
import org.alter.game.model.entity.Player
import org.alter.game.model.inv.Inventory
import org.alter.game.model.item.Item

/**
 * As the "inventory" itself is a specific [ItemContainer] of a [Player], extension functions
 * here are applied by way of the [Player] to allow for more context to these [Item] operations.
 *   Note| the functions provided herein are designed loosely "allowing" flexibility over slot
 *   awareness and even the container to use in operations and can be used in very unintended
 *   degrees, such as an [ItemContainer] which isn't the [Player]'s or maintaining presumed [getInteractingItemSlot]
 */

fun Player.maxPossible(
    vararg items: Int,
    container: Inventory = inventory,
): Int {
    val counts = mutableListOf<Int>()
    items.forEach { item ->
        counts.add(container.getItemCount(item))
    }
    return counts.maxOrNull()!!.coerceAtMost(container.size)
}

// not slot-aware (uses first available) defaults to inventory
fun Player.replaceItem(
    oldItem: Int,
    newItem: Int,
    container: Inventory = inventory,
): Boolean {
    return replaceItemInSlot(oldItem, newItem, -1, container)
}

// slot-aware defaulting to interacting slot and inventory
fun Player.replaceItemInSlot(
    oldItem: Int,
    newItem: Int,
    slot: Int = getInteractingItemSlot(),
    container: Inventory = inventory,
): Boolean {
    return container.replace(oldItem, newItem, slot)
}

