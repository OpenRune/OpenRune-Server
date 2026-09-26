package org.rsmod.content.bosses.barrows

import kotlin.math.floor
import kotlin.random.Random

data class BarrowsLootItem(val obj: String, val count: Int)

internal object BarrowsLoot {
    private class Entry(
        val obj: String,
        val potential: IntRange,
        val divisor: Double,
        val rune: Boolean = false,
    )

    private val ENTRIES =
        listOf(
            Entry("obj.coins", 1..380, 0.5),
            Entry("obj.mindrune", 381..505, 1.5, rune = true),
            Entry("obj.chaosrune", 506..630, 4.5, rune = true),
            Entry("obj.deathrune", 631..755, 9.0, rune = true),
            Entry("obj.bloodrune", 756..880, 20.0, rune = true),
            Entry("obj.barrows_karil_ammo", 881..1005, 25.0),
        )

    private val LOOP_KEY_HALF = 1006..1008
    private val TOOTH_KEY_HALF = 1009..1011

    const val CLUE_OBJ = "obj.trail_elite_emote_exp1"
    const val CLUE_DENOMINATOR = 200

    fun roll(
        slain: List<BarrowsBrother>,
        potential: Int,
        runeBonus: Boolean,
        clueDenominator: Int = CLUE_DENOMINATOR,
        random: Random = Random,
        equipmentBoost: Double = 1.0,
    ): List<BarrowsLootItem> {
        if (potential <= 0) return emptyList()
        val equipment = slain.flatMap { it.armour }.toMutableList()
        val equipmentDenominator =
            floor((450 - 58 * slain.size) / equipmentBoost).toInt().coerceAtLeast(1)
        val loot = mutableListOf<BarrowsLootItem>()
        var clue = false
        repeat(1 + slain.size) {
            if (!clue && random.nextInt(clueDenominator) == 0) {
                clue = true
                loot += BarrowsLootItem(CLUE_OBJ, 1)
            }
            if (equipment.isNotEmpty() && random.nextInt(equipmentDenominator) == 0) {
                loot += BarrowsLootItem(equipment.removeAt(random.nextInt(equipment.size)), 1)
            } else {
                loot += rollMain(potential, runeBonus, random)
            }
        }
        return loot
    }

    private fun rollMain(potential: Int, runeBonus: Boolean, random: Random): BarrowsLootItem {
        val roll = random.nextInt(1, potential.coerceAtMost(MAX_REWARD_POTENTIAL) + 1)
        return when (roll) {
            in LOOP_KEY_HALF -> BarrowsLootItem("obj.keyhalf2", 1)
            in TOOTH_KEY_HALF -> BarrowsLootItem("obj.keyhalf1", 1)
            MAX_REWARD_POTENTIAL -> BarrowsLootItem("obj.dragon_med_helm", 1)
            else -> {
                val entry = ENTRIES.first { roll in it.potential }
                val base = (roll / entry.divisor).toInt()
                val count = if (entry.rune && runeBonus) base * 3 / 2 else base
                BarrowsLootItem(entry.obj, count)
            }
        }
    }
}
