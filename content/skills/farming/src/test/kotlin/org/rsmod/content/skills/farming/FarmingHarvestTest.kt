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
        )

    @Test
    fun `save thresholds include one and clamp the effective farming level`() {
        assertEquals(26, herb.saveThreshold(1))
        assertEquals(81, herb.saveThreshold(99))
        assertEquals(26, herb.saveThreshold(-1))
        assertEquals(81, herb.saveThreshold(120))
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
    fun `allotment constants interpolate over the full level range`() {
        val allotment = herb.copy(kind = PatchKind.ALLOTMENT, saveLow = 101, saveHigh = 180)
        assertEquals(102, allotment.saveThreshold(1))
        assertEquals(181, allotment.saveThreshold(99))
    }

    @Test
    fun `diary bonus adds flat to both endpoints`() {
        assertEquals(36, herb.saveThreshold(1, bonus = 10))
        assertEquals(91, herb.saveThreshold(99, bonus = 10))
    }

    @Test
    fun `saving preserves a life while spending decrements one and empties the last`() {
        val mature = PatchState(weeds = 3, cropIndex = 1, stage = 4, compost = Compost.SUPER, produce = 5)
        assertEquals(mature, mature.harvested(saveLife = true))
        assertEquals(mature.copy(produce = 4), mature.harvested(saveLife = false))
        assertEquals(PatchState(weeds = 3), mature.copy(produce = 1).harvested(saveLife = false))
    }

    @Test
    fun `fixed batches do not roll quantity`() {
        val fail: (Int) -> Int = { error("Fixed quantities must not roll") }
        assertEquals(1, herb.harvestAmount(99, fail))
        val flower = herb.copy(kind = PatchKind.FLOWER, harvest = HarvestModel.BATCH)
        assertEquals(1, flower.harvestAmount(99, fail))
        assertEquals(3, flower.copy(harvestAmount = 3).harvestAmount(99, fail))
    }

    @Test
    fun `limpwurt at drained zero or level one rolls a valid bound and yields the base batch`() {
        val bounds = mutableListOf<Int>()
        val maximum: (Int) -> Int = {
            assertEquals(1, it)
            bounds += it
            it - 1
        }
        assertEquals(3, limpwurt().harvestAmount(0, maximum))
        assertEquals(3, limpwurt().harvestAmount(1, maximum))
        assertEquals(listOf(1, 1), bounds)
    }

    @Test
    fun `limpwurt rolls once with the level uncapped and uses integer division`() {
        val limpwurt = limpwurt()
        val bounds = mutableListOf<Int>()
        val maximum: (Int) -> Int = { bounds += it; it - 1 }
        assertEquals(12, limpwurt.harvestAmount(99, maximum))
        assertEquals(14, limpwurt.harvestAmount(120, maximum))
        assertEquals(listOf(99, 120), bounds)
        assertEquals(3, limpwurt.harvestAmount(99) { 0 })
        assertEquals(3, limpwurt.harvestAmount(99) { 9 })
        assertEquals(4, limpwurt.harvestAmount(99) { 10 })
    }

    private fun limpwurt(): Crop =
        herb.copy(kind = PatchKind.FLOWER, harvest = HarvestModel.BATCH, harvestAmount = 3, harvestLevelDivisor = 10)
}
