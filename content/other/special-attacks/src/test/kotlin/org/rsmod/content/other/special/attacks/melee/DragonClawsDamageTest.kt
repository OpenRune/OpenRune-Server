package org.rsmod.content.other.special.attacks.melee

import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class DragonClawsDamageTest {
    /** Wiki worked example: first hit connects at 35, cascading 35-17-8-9. */
    @Test
    fun `first hit connects and cascades down`() {
        val hits =
            DragonClawsDamage.rollHits(
                maxHit = 46,
                rollAccuracy = accuracySequence(true),
                rollRange = { 35 },
                rollSympathyTriggers = { fail() },
                rollSympathyPattern = { fail() },
            )
        assertArrayEquals(intArrayOf(35, 17, 8, 9), hits)
    }

    /** Wiki worked example: first hit misses, second connects at 30, cascading 0-30-15-16. */
    @Test
    fun `second hit connects and cascades down`() {
        val hits =
            DragonClawsDamage.rollHits(
                maxHit = 46,
                rollAccuracy = accuracySequence(false, true),
                rollRange = { 30 },
                rollSympathyTriggers = { fail() },
                rollSympathyPattern = { fail() },
            )
        assertArrayEquals(intArrayOf(0, 30, 15, 16), hits)
    }

    /** Wiki worked example: third hit connects at 22, cascading 0-0-22-23. */
    @Test
    fun `third hit connects and cascades down`() {
        val hits =
            DragonClawsDamage.rollHits(
                maxHit = 46,
                rollAccuracy = accuracySequence(false, false, true),
                rollRange = { 22 },
                rollSympathyTriggers = { fail() },
                rollSympathyPattern = { fail() },
            )
        assertArrayEquals(intArrayOf(0, 0, 22, 23), hits)
    }

    /** Wiki worked example: fourth hit connects at 46 with no further cascade. */
    @Test
    fun `fourth hit connects with no cascade after it`() {
        val hits =
            DragonClawsDamage.rollHits(
                maxHit = 46,
                rollAccuracy = accuracySequence(false, false, false, true),
                rollRange = { 46 },
                rollSympathyTriggers = { fail() },
                rollSympathyPattern = { fail() },
            )
        assertArrayEquals(intArrayOf(0, 0, 0, 46), hits)
    }

    @Test
    fun `all four miss with no sympathy damage`() {
        val hits =
            DragonClawsDamage.rollHits(
                maxHit = 46,
                rollAccuracy = accuracySequence(false, false, false, false),
                rollRange = { fail() },
                rollSympathyTriggers = { false },
                rollSympathyPattern = { fail() },
            )
        assertArrayEquals(intArrayOf(0, 0, 0, 0), hits)
    }

    @Test
    fun `all four miss with sympathy damage on hits 1 and 2`() {
        val hits =
            DragonClawsDamage.rollHits(
                maxHit = 46,
                rollAccuracy = accuracySequence(false, false, false, false),
                rollRange = { fail() },
                rollSympathyTriggers = { true },
                rollSympathyPattern = { 0 },
            )
        assertArrayEquals(intArrayOf(1, 1, 0, 0), hits)
    }

    @Test
    fun `all four miss with sympathy damage on hits 3 and 4`() {
        val hits =
            DragonClawsDamage.rollHits(
                maxHit = 46,
                rollAccuracy = accuracySequence(false, false, false, false),
                rollRange = { fail() },
                rollSympathyTriggers = { true },
                rollSympathyPattern = { 1 },
            )
        assertArrayEquals(intArrayOf(0, 0, 1, 1), hits)
    }

    @Test
    fun `all four miss with sympathy damage on hits 1 and 3`() {
        val hits =
            DragonClawsDamage.rollHits(
                maxHit = 46,
                rollAccuracy = accuracySequence(false, false, false, false),
                rollRange = { fail() },
                rollSympathyTriggers = { true },
                rollSympathyPattern = { 2 },
            )
        assertArrayEquals(intArrayOf(1, 0, 1, 0), hits)
    }

    @Test
    fun `all four miss with sympathy damage on hits 2 and 4`() {
        val hits =
            DragonClawsDamage.rollHits(
                maxHit = 46,
                rollAccuracy = accuracySequence(false, false, false, false),
                rollRange = { fail() },
                rollSympathyTriggers = { true },
                rollSympathyPattern = { 3 },
            )
        assertArrayEquals(intArrayOf(0, 1, 0, 1), hits)
    }

    @Test
    fun `first hit range is half max to max minus one`() {
        var capturedRange: IntRange? = null
        DragonClawsDamage.rollHits(
            maxHit = 46,
            rollAccuracy = accuracySequence(true),
            rollRange = { range ->
                capturedRange = range
                range.first
            },
            rollSympathyTriggers = { fail() },
            rollSympathyPattern = { fail() },
        )
        assertEquals(23..45, capturedRange)
    }

    @Test
    fun `second hit range is three eighths to seven eighths of max`() {
        var capturedRange: IntRange? = null
        DragonClawsDamage.rollHits(
            maxHit = 46,
            rollAccuracy = accuracySequence(false, true),
            rollRange = { range ->
                capturedRange = range
                range.first
            },
            rollSympathyTriggers = { fail() },
            rollSympathyPattern = { fail() },
        )
        assertEquals(17..40, capturedRange)
    }

    @Test
    fun `third hit range is a quarter to three quarters of max`() {
        var capturedRange: IntRange? = null
        DragonClawsDamage.rollHits(
            maxHit = 46,
            rollAccuracy = accuracySequence(false, false, true),
            rollRange = { range ->
                capturedRange = range
                range.first
            },
            rollSympathyTriggers = { fail() },
            rollSympathyPattern = { fail() },
        )
        assertEquals(11..34, capturedRange)
    }

    @Test
    fun `fourth hit range is a quarter to five quarters of max`() {
        var capturedRange: IntRange? = null
        DragonClawsDamage.rollHits(
            maxHit = 46,
            rollAccuracy = accuracySequence(false, false, false, true),
            rollRange = { range ->
                capturedRange = range
                range.first
            },
            rollSympathyTriggers = { fail() },
            rollSympathyPattern = { fail() },
        )
        assertEquals(11..57, capturedRange)
    }

    @Test
    fun `resolveRange forces the top of the range when maxhit is active`() {
        val result =
            DragonClawsDamage.resolveRange(
                min = 5,
                max = 20,
                isMaxHit = true,
                rollInclusive = { _, _ -> fail() },
                rollUpTo = { fail() },
            )
        assertEquals(20, result)
    }

    @Test
    fun `resolveRange rolls inclusive between min and max when min is positive`() {
        val result =
            DragonClawsDamage.resolveRange(
                min = 5,
                max = 20,
                isMaxHit = false,
                rollInclusive = { lo, hi -> lo + hi },
                rollUpTo = { fail() },
            )
        assertEquals(25, result)
    }

    @Test
    fun `resolveRange rolls up to max plus one when min is zero or below`() {
        val result =
            DragonClawsDamage.resolveRange(
                min = -3,
                max = 20,
                isMaxHit = false,
                rollInclusive = { _, _ -> fail() },
                rollUpTo = { exclusiveBound -> exclusiveBound },
            )
        assertEquals(21, result)
    }

    @Test
    fun `resolveRange clamps a negative max down to zero`() {
        val result =
            DragonClawsDamage.resolveRange(
                min = -3,
                max = -10,
                isMaxHit = true,
                rollInclusive = { _, _ -> fail() },
                rollUpTo = { fail() },
            )
        assertEquals(0, result)
    }

    /** Returns a lambda that yields the given booleans in order, one per call. */
    private fun accuracySequence(vararg results: Boolean): () -> Boolean {
        var index = 0
        return { results[index++] }
    }

    private fun fail(): Nothing = throw AssertionError("unexpected call")
}
