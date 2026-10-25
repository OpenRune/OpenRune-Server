package org.rsmod.content.skills.farming

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class FarmingHarvestTest {
    private val herb =
        Crop(
            index = 1,
            kind = PatchKind.HERB,
            name = "test herb",
            seed = "obj.test_seed",
            produce = "obj.test_produce",
            level = 1,
            plantXp = 1.0,
            harvestXp = 1.0,
            stages = 4,
            cycleMinutes = 20,
            art = CropArt(healthy = listOf(4, 5, 6, 7, 8)),
            saveLow = 25,
            saveHigh = 80,
            magicSecateurs = true,
        )

    @Test
    fun `save thresholds include one and clamp the effective farming level`() {
        assertEquals(26, herb.saveThreshold(1))
        assertEquals(81, herb.saveThreshold(99))
        assertEquals(26, herb.saveThreshold(-1))
        assertEquals(81, herb.saveThreshold(120))
        assertEquals(28, herb.saveThreshold(1, itemPercent = 10))
        assertEquals(89, herb.saveThreshold(99, itemPercent = 10))
        assertEquals(28, herb.saveThreshold(0, itemPercent = 10))
        assertEquals(89, herb.saveThreshold(120, itemPercent = 10))
    }

    @Test
    fun `interpolation rounds to nearest with ties rounded up`() {
        assertEquals(53, herb.saveThreshold(49))
        assertEquals(54, herb.saveThreshold(50))
        val half = herb.copy(saveLow = 0, saveHigh = 1)
        assertEquals(1, half.saveThreshold(49))
        assertEquals(2, half.saveThreshold(50))
    }

    @Test
    fun `item boosts add then floor each endpoint once before interpolation`() {
        assertEquals(32, herb.saveThreshold(7, itemPercent = 10))
        assertEquals(58, herb.saveThreshold(49, itemPercent = 10))
        assertEquals(59, herb.saveThreshold(50, itemPercent = 10))
        // secateurs + cape: floor(25 * 1.15) = 28, floor(80 * 1.15) = 92
        assertEquals(29, herb.saveThreshold(1, itemPercent = 15))
        assertEquals(93, herb.saveThreshold(99, itemPercent = 15))
        val allotment = herb.copy(kind = PatchKind.ALLOTMENT, saveLow = 101, saveHigh = 180)
        assertEquals(102, allotment.saveThreshold(1))
        assertEquals(181, allotment.saveThreshold(99))
        assertEquals(112, allotment.saveThreshold(1, itemPercent = 10))
        assertEquals(199, allotment.saveThreshold(99, itemPercent = 10))
    }

    @Test
    fun `diary bonus adds flat to both boosted endpoints`() {
        assertEquals(36, herb.saveThreshold(1, bonus = 10))
        assertEquals(91, herb.saveThreshold(99, bonus = 10))
        assertEquals(54, herb.saveThreshold(1, itemPercent = 15, bonus = 25))
        assertEquals(118, herb.saveThreshold(99, itemPercent = 15, bonus = 25))
    }

    @Test
    fun `negative chance-to-save constants floor toward negative infinity`() {
        val grape = herb.copy(kind = PatchKind.GRAPE_VINE, saveLow = -50, saveHigh = 170)
        assertEquals(-49, grape.saveThreshold(1))
        // (-50 * 89 + 170 * 9 + 49) / 98 = -29.3: floors to -30, where truncation would give -29
        assertEquals(-29, grape.saveThreshold(10))
        assertEquals(171, grape.saveThreshold(99))
    }

    @Test
    fun `saving preserves a life while spending decrements one and empties the last`() {
        val mature = PatchState(weeds = 3, cropIndex = 1, stage = 4, compost = Compost.SUPER, produce = 5)
        assertEquals(mature, mature.harvested(saveLife = true))
        assertEquals(mature.copy(produce = 4), mature.harvested(saveLife = false))
        assertEquals(PatchState(weeds = 3), mature.copy(produce = 1).harvested(saveLife = false))
    }

    @Test
    fun `saved lives have no total yield cap and survive pack interruptions`() {
        val mature = PatchState(weeds = 3, cropIndex = 127, stage = 4, compost = Compost.ULTRA, produce = 6)
        var state = mature
        repeat(100) { state = PatchState.unpack(state.harvested(saveLife = true).pack()) }
        assertEquals(mature, state)
        state = PatchState.unpack(state.harvested(saveLife = false).pack())
        assertEquals(mature.copy(produce = 5), state)
        repeat(4) { state = PatchState.unpack(state.harvested(saveLife = false).pack()) }
        assertEquals(1, state.produce)
        assertEquals(PatchState(weeds = 3), state.harvested(saveLife = false))
    }

    @Test
    fun `fixed batches do not roll quantity`() {
        val fail: (Int) -> Int = { error("Fixed quantities must not roll") }
        assertEquals(1, herb.harvestAmount(99, true, roll = fail))
        val flower = herb.copy(kind = PatchKind.FLOWER, harvest = HarvestModel.BATCH)
        assertEquals(1, flower.harvestAmount(99, false, roll = fail))
        assertEquals(3, flower.copy(harvestAmount = 3).harvestAmount(99, true, roll = fail))
    }

    @Test
    fun `limpwurt at drained zero or level one rolls a valid bound and yields the base batch`() {
        val limpwurt = limpwurt()
        val bounds = mutableListOf<Int>()
        val maximum: (Int) -> Int = {
            assertEquals(1, it)
            bounds += it
            it - 1
        }
        assertEquals(3, limpwurt.harvestAmount(0, false, roll = maximum))
        assertEquals(3, limpwurt.harvestAmount(0, true, roll = maximum))
        assertEquals(3, limpwurt.harvestAmount(1, false, roll = maximum))
        assertEquals(3, limpwurt.harvestAmount(1, true, roll = maximum))
        assertEquals(listOf(1, 1, 1, 1), bounds)
    }

    @Test
    fun `limpwurt rolls once with boosted level uncapped and uses integer division`() {
        val limpwurt = limpwurt()
        val bounds = mutableListOf<Int>()
        val maximum: (Int) -> Int = { bounds += it; it - 1 }
        assertEquals(12, limpwurt.harvestAmount(99, false, roll = maximum))
        assertEquals(13, limpwurt.harvestAmount(99, true, roll = maximum))
        assertEquals(14, limpwurt.harvestAmount(120, false, roll = maximum))
        assertEquals(16, limpwurt.harvestAmount(120, true, roll = maximum))
        assertEquals(listOf(99, 108, 120, 132), bounds)
        assertEquals(3, limpwurt.harvestAmount(99, true) { 0 })
        assertEquals(3, limpwurt.harvestAmount(99, true) { 9 })
        assertEquals(4, limpwurt.harvestAmount(99, true) { 10 })
        val floor: (Int) -> Int = { assertEquals(20, it); 19 }
        assertEquals(4, limpwurt.harvestAmount(19, true, roll = floor))
        assertEquals(3, limpwurt.copy(magicSecateurs = false).harvestAmount(99, true) { assertEquals(99, it); 9 })
    }

    @Test
    fun `belladonna rolls an inclusive level bound and ignores secateurs`() {
        val belladonna =
            herb.copy(
                kind = PatchKind.BELLADONNA,
                harvest = HarvestModel.BATCH,
                harvestAmount = 6,
                harvestLevelDivisor = 8,
                magicSecateurs = false,
            )
        val bounds = mutableListOf<Int>()
        assertEquals(18, belladonna.harvestAmount(99, true) { bounds += it; it - 1 })
        assertEquals(6, belladonna.harvestAmount(63, false) { bounds += it; 7 })
        assertEquals(listOf(100, 64), bounds)
    }

    @Test
    fun `attas adds five percent to boosted save endpoints and batch levels with floors`() {
        assertEquals(27, herb.saveThreshold(1, attas = true))
        assertEquals(85, herb.saveThreshold(99, attas = true))
        // (25 * 1.15 = 28) + 10 = 38 -> floor(39.9) = 39
        assertEquals(40, herb.saveThreshold(1, itemPercent = 15, bonus = 10, attas = true))
        val bounds = mutableListOf<Int>()
        assertEquals(14, limpwurt().harvestAmount(99, true, attas = true) { bounds += it; it - 1 })
        val belladonna =
            herb.copy(kind = PatchKind.BELLADONNA, harvest = HarvestModel.BATCH, harvestAmount = 6, harvestLevelDivisor = 8)
        assertEquals(18, belladonna.copy(magicSecateurs = false).harvestAmount(99, false, attas = true) { bounds += it; it - 1 })
        assertEquals(listOf(113, 104), bounds)
    }

    private fun limpwurt(): Crop =
        herb.copy(kind = PatchKind.FLOWER, harvest = HarvestModel.BATCH, harvestAmount = 3, harvestLevelDivisor = 10)
}
