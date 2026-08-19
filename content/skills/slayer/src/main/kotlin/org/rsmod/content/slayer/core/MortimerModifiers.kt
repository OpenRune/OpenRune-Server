package org.rsmod.content.slayer.core

import kotlin.math.abs
import kotlin.random.Random
import org.rsmod.api.table.slayer.SlayerMasterTaskRow
import org.rsmod.api.table.slayer.SlayerModifiersRow

object MortimerModifiers {

    const val MODIFIER_POINTS = 1
    const val MODIFIER_QUANTITY = 2
    const val MODIFIER_CLUES = 3
    const val MODIFIER_SUPERIOR = 4
    const val MODIFIER_XP = 5
    const val MODIFIER_THIRD_CHOICE = 6

    data class ModifierRange(val id: Int, val min: Int, val max: Int)

    data class RolledModifier(val id: Int, val value: Int) {
        val negative: Boolean
            get() = value < 0

        val absoluteValue: Int
            get() = abs(value)
    }

    fun parseRanges(masterTask: SlayerMasterTaskRow): List<ModifierRange> =
        masterTask.modifier.chunked(3).mapNotNull { chunk ->
            if (chunk.size != 3) return@mapNotNull null
            ModifierRange(id = chunk[0], min = chunk[1], max = chunk[2])
        }

    fun unlockedModifierIds(tasksCompleted: Int): Set<Int> {
        val fromCache =
            SlayerModifiersRow.all()
                .filter { it.requirement <= tasksCompleted }
                .map { it.id }
                .toSet()
        if (fromCache.isNotEmpty()) return fromCache
        return buildSet {
            add(MODIFIER_POINTS)
            add(MODIFIER_QUANTITY)
            if (tasksCompleted >= 15) add(MODIFIER_CLUES)
            if (tasksCompleted >= 25) add(MODIFIER_SUPERIOR)
            if (tasksCompleted >= 40) add(MODIFIER_XP)
            if (tasksCompleted >= 50) add(MODIFIER_THIRD_CHOICE)
        }
    }

    fun choiceCount(tasksCompleted: Int): Int =
        if (MODIFIER_THIRD_CHOICE in unlockedModifierIds(tasksCompleted)) 3 else 2

    fun rollModifier(masterTask: SlayerMasterTaskRow, tasksCompleted: Int): RolledModifier? {
        val unlocked = unlockedModifierIds(tasksCompleted)
        val eligible =
            parseRanges(masterTask).filter { it.id != MODIFIER_THIRD_CHOICE && it.id in unlocked }
        if (eligible.isEmpty()) return null
        val range = eligible.random()
        return RolledModifier(id = range.id, value = rollSteppedValue(range.min, range.max))
    }

    fun applyQuantityModifier(baseAmount: Int, modifier: RolledModifier?): Int {
        if (modifier == null || modifier.id != MODIFIER_QUANTITY) return baseAmount
        val adjusted = (baseAmount * (100 + modifier.value)) / 100
        return adjusted.coerceAtLeast(1)
    }

    fun overallBenefitMessage(modifier: RolledModifier?): String? {
        if (modifier == null) return null
        return when (modifier.id) {
            MODIFIER_POINTS -> "Your task will award additional Slayer points."
            MODIFIER_QUANTITY ->
                if (modifier.negative) {
                    "Your task has a reduced quantity."
                } else {
                    "Your task has an increased quantity."
                }
            MODIFIER_CLUES -> "Your task has an increased chance of dropping clue scrolls."
            MODIFIER_SUPERIOR -> "Your task has an increased chance of superior unique drops."
            MODIFIER_XP -> "Your task awards increased Slayer experience."
            else -> null
        }
    }

    fun label(modifier: RolledModifier?): String {
        if (modifier == null) return "No mortifier"
        val def = SlayerModifiersRow.all().find { it.id == modifier.id }
        val name = def?.name?.takeIf { it.isNotBlank() } ?: fallbackName(modifier.id)
        return when (modifier.id) {
            MODIFIER_POINTS -> "$name: +${modifier.absoluteValue} points"
            MODIFIER_QUANTITY -> {
                val sign = if (modifier.negative) "" else "+"
                "$name: $sign${modifier.value}%"
            }
            MODIFIER_CLUES,
            MODIFIER_SUPERIOR,
            MODIFIER_XP -> "$name: +${modifier.absoluteValue}%"
            else -> name
        }
    }

    fun chatOption(taskName: String, amount: Int, modifier: RolledModifier?): String {
        val mod = label(modifier)
        return "$taskName ($amount) — $mod"
    }

    private fun fallbackName(id: Int): String =
        when (id) {
            MODIFIER_POINTS -> "Slayer Points"
            MODIFIER_QUANTITY -> "Quantity"
            MODIFIER_CLUES -> "Clue scrolls"
            MODIFIER_SUPERIOR -> "Superior chance"
            MODIFIER_XP -> "Slayer XP"
            else -> "Mortifier"
        }

    private fun rollSteppedValue(min: Int, max: Int): Int {
        val lo = minOf(min, max)
        val hi = maxOf(min, max)
        val step = 5
        val span = hi - lo
        if (span <= 0) return lo
        val steps = (span / step) + 1
        return lo + Random.nextInt(steps) * step
    }
}
