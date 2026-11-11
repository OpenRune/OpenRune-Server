package org.rsmod.content.travel.jewellery

import org.rsmod.game.inv.Inventory

internal class ChargeChain(
    val charged: List<Int>,
    val spent: Int?,
    val remains: Int?,
    val eternal: Boolean,
) {
    fun charges(obj: Int): Int? {
        val index = charged.indexOf(obj)
        return if (index < 0) null else charged.size - index
    }

    fun spend(container: Inventory, slot: Int, expected: Int): SpendResult {
        val current = container[slot]
        if (current == null || current.id != expected) {
            return SpendResult.Vanished
        }
        if (eternal) {
            return SpendResult.Unlimited
        }
        val index = charged.indexOf(expected)
        if (index < 0) {
            return SpendResult.Vanished
        }
        val remaining = charged.size - index - 1
        if (remaining == 0) {
            container[slot] = (spent ?: remains)?.let { current.copy(id = it) }
            return SpendResult.Depleted
        }
        container[slot] = current.copy(id = charged[index + 1])
        return SpendResult.Remaining(remaining)
    }
}

internal sealed interface SpendResult {
    data object Vanished : SpendResult

    data object Unlimited : SpendResult

    data object Depleted : SpendResult

    data class Remaining(val charges: Int) : SpendResult
}
