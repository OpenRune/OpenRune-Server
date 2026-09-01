package org.rsmod.content.other.special.attacks.melee

import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class BurningClawDamageTest {
    @Test
    fun firstSuccessTierUsesCorrectRangeAndSplit() {
        val result =
            BurningClawDamage.roll(
                maxHit = 39,
                rollAccuracy = { true },
                rollInclusive = { range ->
                    assertEquals(29..68, range)
                    29
                },
                rollExclusive = { error("A successful roll must not use the miss fallback.") },
            )

        assertArrayEquals(intArrayOf(7, 7, 14), result.hits)
        assertEquals(15, result.burnChancePercent)
    }

    @Test
    fun secondSuccessTierUsesCorrectRangeAndSplit() {
        val accuracy = listOf(false, true).iterator()
        val result =
            BurningClawDamage.roll(
                maxHit = 39,
                rollAccuracy = { accuracy.next() },
                rollInclusive = { range ->
                    assertEquals(19..58, range)
                    16
                },
                rollExclusive = { error("A successful roll must not use the miss fallback.") },
            )

        assertArrayEquals(intArrayOf(7, 7, 2), result.hits)
        assertEquals(30, result.burnChancePercent)
    }

    @Test
    fun thirdSuccessTierUsesCorrectRangeAndSplit() {
        val accuracy = listOf(false, false, true).iterator()
        val result =
            BurningClawDamage.roll(
                maxHit = 39,
                rollAccuracy = { accuracy.next() },
                rollInclusive = { range ->
                    assertEquals(9..48, range)
                    9
                },
                rollExclusive = { error("A successful roll must not use the miss fallback.") },
            )

        assertArrayEquals(intArrayOf(1, 1, 7), result.hits)
        assertEquals(45, result.burnChancePercent)
    }

    @Test
    fun failedAccuracyUsesExactFallbackAndCannotBurn() {
        fun misses(roll: Int): BurningClawDamage.Result {
            val accuracy = listOf(false, false, false).iterator()
            return BurningClawDamage.roll(
                maxHit = 39,
                rollAccuracy = { accuracy.next() },
                rollInclusive = { error("A failed roll must not use a damage range.") },
                rollExclusive = {
                    assertEquals(5, it)
                    roll
                },
            )
        }

        assertArrayEquals(intArrayOf(0, 0, 0), misses(0).hits)
        assertArrayEquals(intArrayOf(0, 0, 1), misses(1).hits)
        assertArrayEquals(intArrayOf(0, 0, 1), misses(2).hits)
        assertArrayEquals(intArrayOf(0, 0, 2), misses(3).hits)
        assertArrayEquals(intArrayOf(0, 0, 2), misses(4).hits)
        assertEquals(0, misses(0).burnChancePercent)
    }
}
