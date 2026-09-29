package dtx.rs

import dtx.core.ArgMap
import dtx.core.RollResult
import dtx.core.Single
import dtx.core.flatten
import dtx.impl.chance.RateBoostChanceRollable
import dtx.impl.chance.RateBoosts
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.junit.jupiter.api.parallel.ResourceLock

@Execution(ExecutionMode.SAME_THREAD)
@ResourceLock("RateBoosts")
class RateBoostTest {
    @AfterEach
    fun reset() {
        RateBoosts.multiplier = { _, _ -> 1.0 }
    }

    @Test
    fun `chance denominator is floored like the wiki boosted-drops table`() {
        fun chance(denominator: Int, multiplier: Double): Double {
            RateBoosts.multiplier = { _, _ -> multiplier }
            val roll = RateBoostChanceRollable<String, String>(1, denominator, Single("x"))
            return roll.chanceFor("p", ArgMap.Empty)
        }

        assertEquals(100.0 / 512, chance(512, 1.0), 1e-9)
        assertEquals(100.0 / 256, chance(512, 2.0), 1e-9)
        assertEquals(100.0 / 102, chance(512, 5.0), 1e-9)
        assertEquals(100.0 / 12, chance(25, 2.0), 1e-9)
        assertEquals(100.0 / 6553, chance(32768, 5.0), 1e-9)
        assertEquals(100.0, chance(2, 10.0), 1e-9)
    }

    @Test
    fun `boosted weighted entry hits the floored rate and others keep their ratio`() {
        val table = weightedTable(boostedFirst = true)
        val samples = 400_000

        fun rates(multiplier: Double): Map<String, Double> {
            RateBoosts.multiplier = { _, _ -> multiplier }
            return sample(table, samples).mapValues { it.value.toDouble() / samples }
        }

        assertRate(1.0 / 128, rates(1.0)["rare"])
        assertRate(1.0 / 64, rates(2.0)["rare"])
        assertRate(1.0 / 25, rates(5.0)["rare"])

        val boosted = rates(5.0)
        val unboosted = boosted.getValue("a") / (boosted.getValue("a") + boosted.getValue("b"))
        assertRate(100.0 / 127, unboosted, tolerance = 0.005)
    }

    @Test
    fun `unflagged weighted entry ignores the multiplier`() {
        val table = weightedTable(boostedFirst = false)
        RateBoosts.multiplier = { _, _ -> 5.0 }
        val samples = 400_000
        assertRate(1.0 / 128, sample(table, samples)["rare"]?.let { it.toDouble() / samples })
    }

    @Test
    fun `boosted separate roll uses the multiplier`() {
        val table =
            rsWeightedTable<String, String>(total = 2) {
                1 weight "a"
                1 weight "b"
                boostScope = true
                (1 outOf 512) separate "rare"
            }
        val drop =
            RSDropTable(
                tableIdentifier = "test",
                mainTable = table,
            )
        RateBoosts.multiplier = { _, _ -> 5.0 }
        val samples = 400_000
        var hits = 0
        repeat(samples) {
            val result = drop.roll("p", ArgMap.Empty).flatten()
            val results =
                when (result) {
                    is RollResult.ListOf -> result.results
                    is RollResult.Single -> listOf(result.result)
                    else -> emptyList()
                }
            if ("rare" in results) hits++
        }
        assertRate(1.0 / 102, hits.toDouble() / samples)
    }

    private fun weightedTable(boostedFirst: Boolean): RSWeightedTable<String, String> =
        rsWeightedTable(total = 128) {
            boostScope = boostedFirst
            1 weight "rare"
            boostScope = false
            100 weight "a"
            27 weight "b"
        }

    private fun sample(table: RSWeightedTable<String, String>, samples: Int): Map<String, Int> {
        val counts = HashMap<String, Int>()
        repeat(samples) {
            val result = table.roll("p", ArgMap.Empty).flatten()
            if (result is RollResult.Single) {
                counts.merge(result.result, 1, Int::plus)
            }
        }
        return counts
    }

    private fun assertRate(expected: Double, actual: Double?, tolerance: Double = 0.0015) {
        assertTrue(
            actual != null && kotlin.math.abs(actual - expected) <= tolerance,
            "expected ~$expected but was $actual",
        )
    }
}
