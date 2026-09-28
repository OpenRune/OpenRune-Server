package org.rsmod.content.bosses.barrows

import kotlin.random.Random
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class BarrowsLootTest {
    @Test
    fun `no loot without reward potential`() {
        assertTrue(BarrowsLoot.roll(BarrowsBrother.entries, 0, runeBonus = false).isEmpty())
    }

    @Test
    fun `one roll per slain brother plus one`() {
        val random = Random(1)
        repeat(500) {
            val loot =
                BarrowsLoot.roll(
                    BarrowsBrother.entries,
                    MAX_REWARD_POTENTIAL,
                    runeBonus = false,
                    clueDenominator = Int.MAX_VALUE,
                    random = random,
                )
            assertEquals(BarrowsBrother.entries.size + 1, loot.size)
        }
    }

    @Test
    fun `quantities stay within wiki ranges`() {
        val expected =
            mapOf(
                "obj.coins" to 2..760,
                "obj.mindrune" to 254..336,
                "obj.chaosrune" to 112..140,
                "obj.deathrune" to 70..83,
                "obj.bloodrune" to 37..44,
                "obj.barrows_karil_ammo" to 35..40,
                "obj.keyhalf1" to 1..1,
                "obj.keyhalf2" to 1..1,
                "obj.dragon_med_helm" to 1..1,
            )
        val random = Random(7)
        val seen = mutableSetOf<String>()
        repeat(20_000) {
            val loot = BarrowsLoot.roll(emptyList(), MAX_REWARD_POTENTIAL, false, Int.MAX_VALUE, random)
            for (item in loot) {
                val range = expected.getValue(item.obj)
                assertTrue(item.count in range, "${item.obj} x${item.count} outside $range")
                seen += item.obj
            }
        }
        assertEquals(expected.keys, seen)
    }

    @Test
    fun `potential limits the main table`() {
        val random = Random(3)
        repeat(2_000) {
            val loot = BarrowsLoot.roll(emptyList(), 380, false, Int.MAX_VALUE, random)
            assertTrue(loot.all { it.obj == "obj.coins" })
        }
    }

    @Test
    fun `morytania hard diary gives half again as many runes`() {
        val random = Random(11)
        repeat(5_000) {
            val loot = BarrowsLoot.roll(emptyList(), 880, true, Int.MAX_VALUE, random)
            for (item in loot.filter { it.obj == "obj.bloodrune" }) {
                assertTrue(item.count in 55..66, "blood rune x${item.count}")
            }
        }
    }

    @Test
    fun `equipment only comes from slain brothers and never duplicates`() {
        val slain = listOf(BarrowsBrother.DHAROK, BarrowsBrother.VERAC)
        val allowed = slain.flatMap { it.armour }.toSet()
        val every = BarrowsBrother.entries.flatMap { it.armour }.toSet()
        val random = Random(5)
        repeat(50_000) {
            val loot = BarrowsLoot.roll(slain, 500, false, Int.MAX_VALUE, random)
            val equipment = loot.filter { it.obj in every }
            assertTrue(equipment.all { it.obj in allowed })
            assertEquals(equipment.size, equipment.map { it.obj }.toSet().size)
        }
    }

    @Test
    fun `at most one clue per chest`() {
        val random = Random(9)
        repeat(1_000) {
            val loot = BarrowsLoot.roll(BarrowsBrother.entries, 1012, false, 1, random)
            assertEquals(1, loot.count { it.obj == BarrowsLoot.CLUE_OBJ })
        }
    }

    @Test
    fun `drop rate boost scales the equipment chance`() {
        val every = BarrowsBrother.entries.flatMap { it.armour }.toSet()
        fun pieces(boost: Double): Int {
            val random = Random(13)
            var count = 0
            repeat(20_000) {
                val loot =
                    BarrowsLoot.roll(
                        BarrowsBrother.entries,
                        MAX_REWARD_POTENTIAL,
                        false,
                        Int.MAX_VALUE,
                        random,
                        equipmentBoost = boost,
                    )
                count += loot.count { it.obj in every }
            }
            return count
        }
        val base = pieces(1.0)
        val boosted = pieces(2.0)
        assertTrue(boosted > base * 17 / 10, "base=$base boosted=$boosted")
    }
}
