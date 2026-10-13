package org.rsmod.content.other.gravestones

import org.rsmod.game.inv.InvObj
import org.rsmod.game.inv.Inventory

/**
 * Adds [items] to [grave] and sorts it by value, most valuable first, the order the client's fee
 * bands expect. The grave stacks everything like a bank (live capture, rev 241: two 4-dose staminas
 * in one slot). Returns what doesn't fit: the part of a stack past `Int.MAX_VALUE`, and the least
 * valuable slots past the grave's size.
 */
internal fun storeInGrave(grave: Inventory, items: List<InvObj>, value: (InvObj) -> Long): List<InvObj> {
    val stacks = mutableListOf<InvObj>()
    val unheld = mutableListOf<InvObj>()
    for (obj in grave.filterNotNull { true } + items) {
        val index = stacks.indexOfFirst { it.id == obj.id && it.vars == obj.vars }
        if (index == -1) {
            stacks += obj
            continue
        }
        val stack = stacks[index]
        val total = stack.count.toLong() + obj.count
        val held = total.coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
        stacks[index] = stack.copy(count = held)
        if (total > held) {
            unheld += obj.copy(count = (total - held).toInt())
        }
    }
    val sorted = stacks.sortedByDescending(value)
    grave.fillNulls()
    for ((slot, obj) in sorted.withIndex()) {
        if (slot >= grave.size) {
            unheld += obj
            continue
        }
        grave[slot] = obj
    }
    return unheld
}
