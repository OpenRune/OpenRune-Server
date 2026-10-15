package org.rsmod.content.raids.toa.raid.loot

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.rsmod.api.random.GameRandom

class ToaLootRollerTest {
    @Test
    fun `unique level is piecewise by raid level`() {
        assertEquals(300.0, ToaLootMath.uniqueLevel(300))
        assertEquals(340.0, ToaLootMath.uniqueLevel(400))
        assertEquals(360.0, ToaLootMath.uniqueLevel(490))
    }

    @Test
    fun `unique chance is one percent per divisor and never exceeds one`() {
        assertEquals(0.01, ToaLootMath.uniqueChance(3_700, 400), 1e-9)
        assertEquals(0.1, ToaLootMath.uniqueChance(37_000, 400), 1e-9)
        assertEquals(1.0, ToaLootMath.uniqueChance(64_000 * 8, 400), 1e-9)
    }

    @Test
    fun `unique boost multiplies the chance and never exceeds one`() {
        assertEquals(0.02, ToaLootMath.uniqueChance(3_700, 400, 2.0), 1e-9)
        assertEquals(0.15, ToaLootMath.uniqueChance(37_000, 400, 1.5), 1e-9)
        assertEquals(1.0, ToaLootMath.uniqueChance(64_000 * 8, 400, 4.0), 1e-9)
    }

    @Test
    fun `common quantity scales by points and raid level boost`() {
        assertEquals(10_000, ToaLootMath.commonQuantity(10_000, 1, 150))
        assertEquals(13_500, ToaLootMath.commonQuantity(10_000, 1, 400))
        assertEquals(1, ToaLootMath.commonQuantity(10_000, 0, 500))
        assertEquals(1, ToaLootMath.commonQuantity(10, 7_500, 150))
    }

    @Test
    fun `thread chance scales to triple at fifteen kills and drops once owned`() {
        assertEquals(0.1, ToaLootMath.threadChance(0, false), 1e-9)
        assertEquals(0.3, ToaLootMath.threadChance(15, false), 1e-9)
        assertEquals(0.3, ToaLootMath.threadChance(400, false), 1e-9)
        assertEquals(0.02, ToaLootMath.threadChance(15, true), 1e-9)
    }

    @Test
    fun `jewel chance is four fiftieths scaling to triple at seventy five kills`() {
        assertEquals(0.08, ToaLootMath.jewelChance(0), 1e-9)
        assertEquals(0.24, ToaLootMath.jewelChance(75), 1e-9)
    }

    @Test
    fun `elite clue chance caps and applies the combat achievement bonus after`() {
        assertEquals(0.05, ToaLootMath.eliteClueChance(10_000, false), 1e-9)
        assertEquals(0.25, ToaLootMath.eliteClueChance(1_000_000, false), 1e-9)
        assertEquals(0.2625, ToaLootMath.eliteClueChance(1_000_000, true), 1e-9)
    }

    @Test
    fun `pet chance uses the scaled level`() {
        assertEquals(0.01 * 35_000 / (350_000 - 700 * 400.0), ToaLootMath.petChance(35_000, 400), 1e-9)
        assertEquals(
            ToaLootMath.petChance(35_000, 550),
            ToaLootMath.petChance(35_000, 600),
            1e-9,
        )
    }

    @Test
    fun `no unique when the chance roll fails`() {
        val roller = roller(doubles = listOf(0.99))
        assertNull(roller.rollUnique("a", 20_000, 400) { true })
    }

    @Test
    fun `every player rolls their own unique chance`() {
        val points = linkedMapOf("a" to 20_000, "b" to 20_000)
        val roll = roller(doubles = listOf(0.0, 0.99)).rollRaid(points, 400) { true }
        assertEquals(setOf("a"), roll.uniques.keys)
        assertEquals(setOf("b"), roll.commons.keys)
    }

    @Test
    fun `several players can win a unique in the same raid`() {
        val points = linkedMapOf("a" to 20_000, "b" to 20_000)
        val roll = roller(doubles = listOf(0.0, 0.0)).rollRaid(points, 400) { true }
        assertEquals(setOf("a", "b"), roll.uniques.keys)
        assertTrue(roll.commons.isEmpty())
    }

    @Test
    fun `boost raises the win threshold`() {
        val boosted = roller(doubles = listOf(0.1), boost = 4.0)
        assertEquals("a", boosted.rollUnique("a", 20_000, 400) { true }?.winner)
        val plain = roller(doubles = listOf(0.1))
        assertNull(plain.rollUnique("a", 20_000, 400) { true })
    }

    @Test
    fun `out of level unique is kept only on the one in fifty roll`() {
        val kept = roller(doubles = listOf(0.0), ints = listOf(0))
        assertFalse(requireNotNull(kept.rollUnique("a", 20_000, 500) { false }).consolation)
        val lost = roller(doubles = listOf(0.0), ints = listOf(1))
        assertTrue(requireNotNull(lost.rollUnique("a", 20_000, 500) { false }).consolation)
    }

    @Test
    fun `consolation winners still get commons`() {
        val roll =
            roller(doubles = listOf(0.0), ints = listOf(1))
                .rollRaid(mapOf("a" to 20_000), 500) { false }
        assertTrue(requireNotNull(roll.uniques["a"]).consolation)
        assertEquals(setOf("a"), roll.commons.keys)
    }

    @Test
    fun `unique winner gets no commons and low points get dung instead`() {
        val points = linkedMapOf("winner" to 20_000, "dung" to 1_000, "common" to 20_000)
        val roll = roller(doubles = listOf(0.0, 0.99)).rollRaid(points, 150) { true }
        assertEquals(setOf("winner"), roll.uniques.keys)
        assertEquals(setOf("dung"), roll.dung)
        assertEquals(setOf("common"), roll.commons.keys)
        assertEquals(ToaLootMath.COMMON_ROLLS, roll.commons.getValue("common").size)
    }

    private fun roller(
        doubles: List<Double> = emptyList(),
        ints: List<Int> = emptyList(),
        boost: Double = 1.0,
    ) =
        ToaLootRoller<String>(
            ScriptedRandom(doubles, ints),
            pickCommon = { CommonDrop("coins", 1) },
            pickUnique = { _, _ -> "fang" },
            uniqueBoost = { boost },
        )

    private class ScriptedRandom(doubles: List<Double>, ints: List<Int>) : GameRandom {
        private val doubles = doubles.iterator()
        private val ints = ints.iterator()

        override fun of(maxExclusive: Int): Int = ints.next()

        override fun of(minInclusive: Int, maxInclusive: Int): Int = ints.next()

        override fun randomDouble(): Double = doubles.next()
    }
}
