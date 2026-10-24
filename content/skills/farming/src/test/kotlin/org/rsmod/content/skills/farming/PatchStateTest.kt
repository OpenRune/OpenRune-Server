package org.rsmod.content.skills.farming

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/** Pure state fixtures deliberately do not load farming cache tables. */
class PatchStateTest {
    private val crop =
        Crop(
            kind = PatchKind.ALLOTMENT,
            name = "test crop",
            seed = "obj.test_seed",
            produce = "obj.test_produce",
            level = 1,
            plantXp = 8.0,
            harvestXp = 9.0,
            growBase = 6,
            stages = 4,
            stageMinutes = 10,
            wateredBase = 23,
            diseasedBase = 42,
            deadBase = 63,
        )

    private val none = CompostDef(Compost.NONE, null, 0.0, 3, 0.35)
    private val never: (Double) -> Boolean = { false }
    private val always: (Double) -> Boolean = { true }

    @Test
    fun `packs and unpacks every field`() {
        val state =
            PatchState(
                weeds = 3,
                cropIndex = 1,
                stage = 2,
                watered = true,
                health = Health.DISEASED,
                compost = Compost.SUPER,
                produce = 5,
                minutes = 200,
            )
        assertEquals(state, PatchState.unpack(state.pack()))
    }

    @Test
    fun `packed crop indices and harvest lives retain their full ranges`() {
        val state = PatchState(cropIndex = 127, stage = 15, produce = 15, minutes = 255)
        assertEquals(state, PatchState.unpack(state.pack()))
    }

    @Test
    fun `grows one stage per stage time`() {
        val after = planted().advance(crop, crop.stageMinutes, none, diseaseRoll = never)
        assertEquals(1, after.stage)
        assertEquals(crop.stageMinutes, after.minutes)
    }

    @Test
    fun `growth uses supplied compost lives rather than ordinal arithmetic`() {
        for ((kind, lives) in Compost.entries.zip(listOf(3, 4, 5, 6))) {
            val compost = none.copy(kind = kind, harvestLives = lives)
            val after =
                planted().copy(compost = kind).advance(crop, Int.MAX_VALUE, compost, diseaseRoll = never)
            assertEquals(crop.stages, after.stage)
            assertTrue(after.grown(crop))
            assertEquals(lives, after.produce)
            assertEquals(0, after.minutes)
            assertEquals(after, after.advance(crop, Int.MAX_VALUE, compost, diseaseRoll = always))
        }
        val supplied = none.copy(harvestLives = 7)
        assertEquals(7, planted().advance(crop, 40, supplied, diseaseRoll = never).produce)
    }

    @Test
    fun `flowers mature with one pending batch regardless of compost lives`() {
        val flower = crop.copy(kind = PatchKind.FLOWER, harvestAmount = 3)
        val after = planted().advance(flower, 40, none.copy(harvestLives = 6), diseaseRoll = never)
        assertEquals(1, after.produce)
        assertEquals(PatchState(weeds = 3, minutes = 5), after.harvested(saveLife = false))
    }

    @Test
    fun `an uncured disease kills the crop on the next stage`() {
        val diseased = planted().advance(crop, 10, none, diseaseRoll = always)
        assertEquals(Health.DISEASED, diseased.health)
        val dead = diseased.advance(crop, 10, none, diseaseRoll = never)
        assertEquals(Health.DEAD, dead.health)
        assertEquals(0, dead.minutes)
        assertEquals(dead, dead.advance(crop, Int.MAX_VALUE, none, diseaseRoll = never))
    }

    @Test
    fun `watering suppresses the roll for only its current stage`() {
        var rolls = 0
        val roll: (Double) -> Boolean = { rolls++; false }
        val after = planted().copy(watered = true).advance(crop, 10, none, diseaseRoll = roll)
        assertEquals(0, rolls)
        assertFalse(after.watered)
        after.advance(crop, 10, none, diseaseRoll = roll)
        assertEquals(1, rolls)
    }

    @Test
    fun `protection suppresses every disease roll without curing existing disease`() {
        val fail: (Double) -> Boolean = { error("Protected growth must not roll disease") }
        val grown = planted().advance(crop, Int.MAX_VALUE, none, `protected` = true, diseaseRoll = fail)
        assertEquals(crop.stages, grown.stage)
        assertEquals(Health.HEALTHY, grown.health)
        val diseased = planted().copy(health = Health.DISEASED)
        assertEquals(
            Health.DEAD,
            diseased.advance(crop, 10, none, `protected` = true, diseaseRoll = fail).health,
        )
    }

    @Test
    fun `unprotected growth rolls supplied chance per susceptible stage including ultra`() {
        for (chance in listOf(0.35, 0.175, 0.07, 0.035, 0.123)) {
            val seen = mutableListOf<Double>()
            val compost = none.copy(kind = Compost.ULTRA, diseaseChance = chance)
            planted().advance(crop, Int.MAX_VALUE, compost) { seen += it; false }
            assertEquals(listOf(chance / 4, chance / 4, chance / 4), seen)
        }
        val ultra = none.copy(kind = Compost.ULTRA, diseaseChance = 0.035)
        assertEquals(Health.DISEASED, planted().advance(crop, 10, ultra, diseaseRoll = always).health)
    }

    @Test
    fun `interrupted packed growth matches uninterrupted elapsed growth`() {
        val partial = planted().advance(crop, 17, none, diseaseRoll = never)
        assertEquals(3, partial.minutes)
        val resumed = PatchState.unpack(partial.pack()).advance(crop, 23, none, diseaseRoll = never)
        assertEquals(planted().advance(crop, 40, none, diseaseRoll = never), resumed)
    }

    @Test
    fun `zero elapsed does nothing and zero duration growth terminates`() {
        val planted = planted().copy(minutes = 0)
        assertEquals(planted, planted.advance(crop, 0, none, diseaseRoll = always))
        val instant = crop.copy(stageMinutes = 0)
        assertEquals(crop.stages, planted.advance(instant, 1, none, diseaseRoll = never).stage)
    }

    @Test
    fun `transmit selects supplied bank and clamps stage to maturity`() {
        val banked = crop.copy(transmit = (100..119).toList())
        val growing = planted().copy(stage = 2)
        assertEquals(102, growing.transmit(banked))
        assertEquals(107, growing.copy(watered = true).transmit(banked))
        assertEquals(112, growing.copy(health = Health.DISEASED, watered = true).transmit(banked))
        assertEquals(117, growing.copy(health = Health.DEAD, watered = true).transmit(banked))
        assertEquals(104, growing.copy(stage = 15).transmit(banked))
    }

    @Test
    fun `regular transmit uses configured bases and health takes precedence over watering`() {
        val growing = planted().copy(stage = 1)
        assertEquals(7, growing.transmit(crop))
        assertEquals(24, growing.copy(watered = true).transmit(crop))
        assertEquals(42, growing.copy(health = Health.DISEASED, watered = true).transmit(crop))
        assertEquals(63, growing.copy(health = Health.DEAD, watered = true).transmit(crop))
    }

    @Test
    fun `herb disease and death select configured art and clamp the shared stages`() {
        val herb = crop.copy(kind = PatchKind.HERB, diseasedBase = 81, deadBase = 93)
        val diseased = listOf(81, 81, 82, 83, 83)
        val dead = listOf(93, 93, 94, 95, 95)
        for (stage in 0..herb.stages) {
            val state = planted().copy(stage = stage, watered = true)
            assertEquals(herb.growBase + stage, state.transmit(herb))
            assertEquals(diseased[stage], state.copy(health = Health.DISEASED).transmit(herb))
            assertEquals(dead[stage], state.copy(health = Health.DEAD).transmit(herb))
        }
    }

    @Test
    fun `empty resets planted fields and leaves cleared soil for five minutes`() {
        val state =
            planted().copy(stage = 3, compost = Compost.ULTRA, health = Health.DEAD, produce = 6, watered = true)
        assertEquals(PatchState(weeds = 3, minutes = 5), state.empty())
    }

    @Test
    fun `weed regrowth respects five minute boundaries`() {
        val empty = planted().empty()
        assertEquals(PatchState(weeds = 3, minutes = 1), empty.advance(null, 4, none, diseaseRoll = never))
        assertEquals(PatchState(weeds = 2, minutes = 5), empty.advance(null, 5, none, diseaseRoll = never))
        assertEquals(PatchState(weeds = 1, minutes = 1), empty.advance(null, 14, none, diseaseRoll = never))
        assertEquals(PatchState(), empty.advance(null, 15, none, diseaseRoll = never))
        assertEquals(PatchState(), empty.advance(null, Int.MAX_VALUE, none, diseaseRoll = never))
    }

    @Test
    fun `partially raked legacy patches with zero timer regrow without looping`() {
        for (weeds in 1..3) {
            val partial = PatchState(weeds = weeds)
            assertEquals(partial, partial.advance(null, 0, none, diseaseRoll = never))
            assertEquals(partial.copy(minutes = 1), partial.advance(null, 4, none, diseaseRoll = never))
            assertEquals(PatchState(), partial.advance(null, Int.MAX_VALUE, none, diseaseRoll = never))
        }
        assertEquals(PatchState(), PatchState().advance(null, Int.MAX_VALUE, none, diseaseRoll = never))
    }

    @Test
    fun `packed weed countdown resumes at the same boundary`() {
        val partial = planted().empty().advance(null, 4, none, diseaseRoll = never)
        val resumed = PatchState.unpack(partial.pack()).advance(null, 1, none, diseaseRoll = never)
        assertEquals(PatchState(weeds = 2, minutes = 5), resumed)
    }

    @Test
    fun `an empty patch transmits its weed level`() {
        for (weeds in 0..3) assertEquals(weeds, PatchState(weeds = weeds).transmit(null))
    }

    private fun planted() = PatchState(weeds = PatchState.CLEARED, cropIndex = 1, minutes = crop.stageMinutes)
}
