package org.rsmod.content.skills.farming

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class FarmingHarvestTest {
    private val herb =
        Crop(
            kind = PatchKind.HERB,
            name = "test herb",
            seed = "obj.test_seed",
            produce = "obj.test_produce",
            level = 1,
            plantXp = 1.0,
            harvestXp = 1.0,
            growBase = 4,
            stages = 4,
            stageMinutes = 20,
            wateredBase = 4,
            deadBase = 90,
            saveLow = 25,
            saveHigh = 80,
        )

    @Test
    fun `save thresholds include one and clamp the effective farming level`() {
        assertEquals(26, herb.saveThreshold(1, false))
        assertEquals(81, herb.saveThreshold(99, false))
        assertEquals(26, herb.saveThreshold(-1, false))
        assertEquals(81, herb.saveThreshold(120, false))
        assertEquals(28, herb.saveThreshold(1, true))
        assertEquals(89, herb.saveThreshold(99, true))
        assertEquals(28, herb.saveThreshold(0, true))
        assertEquals(89, herb.saveThreshold(120, true))
    }

    @Test
    fun `interpolation rounds to nearest with ties rounded up`() {
        assertEquals(53, herb.saveThreshold(49, false))
        assertEquals(54, herb.saveThreshold(50, false))
        val half = herb.copy(saveLow = 0, saveHigh = 1)
        assertEquals(1, half.saveThreshold(49, false))
        assertEquals(2, half.saveThreshold(50, false))
    }

    @Test
    fun `secateurs floor each boosted endpoint before interpolation`() {
        assertEquals(32, herb.saveThreshold(7, true))
        assertEquals(58, herb.saveThreshold(49, true))
        assertEquals(59, herb.saveThreshold(50, true))
        val allotment = herb.copy(kind = PatchKind.ALLOTMENT, saveLow = 101, saveHigh = 180)
        assertEquals(102, allotment.saveThreshold(1, false))
        assertEquals(181, allotment.saveThreshold(99, false))
        assertEquals(112, allotment.saveThreshold(1, true))
        assertEquals(199, allotment.saveThreshold(99, true))
    }

    @Test
    fun `saving preserves a life while spending decrements one and empties the last`() {
        val mature = PatchState(weeds = 3, cropIndex = 1, stage = 4, compost = Compost.SUPER, produce = 5)
        assertEquals(mature, mature.harvested(saveLife = true))
        assertEquals(mature.copy(produce = 4), mature.harvested(saveLife = false))
        assertEquals(PatchState(weeds = 3, minutes = 5), mature.copy(produce = 1).harvested(saveLife = false))
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
        assertEquals(PatchState(weeds = 3, minutes = 5), state.harvested(saveLife = false))
    }

    @Test
    fun `ordinary crops and fixed flower batches do not roll quantity`() {
        val fail: (Int) -> Int = { error("Fixed quantities must not roll") }
        assertEquals(1, herb.harvestAmount(99, true, fail))
        assertEquals(1, herb.copy(kind = PatchKind.ALLOTMENT).harvestAmount(99, true, fail))
        val flower = herb.copy(kind = PatchKind.FLOWER)
        assertEquals(1, flower.harvestAmount(99, false, fail))
        assertEquals(3, flower.copy(harvestAmount = 3).harvestAmount(99, true, fail))
    }

    @Test
    fun `limpwurt at drained zero or level one rolls a valid bound and yields the base batch`() {
        val limpwurt = herb.copy(kind = PatchKind.FLOWER, harvestAmount = 3, harvestLevelDivisor = 10)
        val bounds = mutableListOf<Int>()
        val maximum: (Int) -> Int = {
            assertEquals(1, it)
            bounds += it
            it - 1
        }
        assertEquals(3, limpwurt.harvestAmount(0, false, maximum))
        assertEquals(3, limpwurt.harvestAmount(0, true, maximum))
        assertEquals(3, limpwurt.harvestAmount(1, false, maximum))
        assertEquals(3, limpwurt.harvestAmount(1, true, maximum))
        assertEquals(listOf(1, 1, 1, 1), bounds)
    }

    @Test
    fun `limpwurt rolls once with boosted level uncapped and uses integer division`() {
        val limpwurt = herb.copy(kind = PatchKind.FLOWER, harvestAmount = 3, harvestLevelDivisor = 10)
        val bounds = mutableListOf<Int>()
        val maximum: (Int) -> Int = { bounds += it; it - 1 }
        assertEquals(12, limpwurt.harvestAmount(99, false, maximum))
        assertEquals(13, limpwurt.harvestAmount(99, true, maximum))
        assertEquals(14, limpwurt.harvestAmount(120, false, maximum))
        assertEquals(16, limpwurt.harvestAmount(120, true, maximum))
        assertEquals(listOf(99, 108, 120, 132), bounds)
        assertEquals(3, limpwurt.harvestAmount(99, true) { 0 })
        assertEquals(3, limpwurt.harvestAmount(99, true) { 9 })
        assertEquals(4, limpwurt.harvestAmount(99, true) { 10 })
        val floor: (Int) -> Int = { assertEquals(20, it); 19 }
        assertEquals(4, limpwurt.harvestAmount(19, true, floor))
    }
}
