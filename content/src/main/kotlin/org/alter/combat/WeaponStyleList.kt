package org.alter.combat

import org.alter.game.model.combat.AttackStyle

data class WeaponStyleList(
    val one: AttackStyle?,
    val two: AttackStyle?,
    val three: AttackStyle?,
    val four: AttackStyle?,
) {
    operator fun get(index: Int): AttackStyle? =
        when (index) {
            0 -> one
            1 -> two
            2 -> three
            3 -> four
            else -> throw IndexOutOfBoundsException("Invalid index: $index")
        }
}