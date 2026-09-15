package org.rsmod.content.skills.farming

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class PatchStateTest {
    private val potato = FarmingCrops.all.first { it.name == "potato" }
    private val guam = FarmingCrops.all.first { it.name == "guam" }

    private val never: (Double) -> Boolean = { false }
    private val always: (Double) -> Boolean = { true }

    @Test
    fun `packs and unpacks every field`() {
        val state =
            PatchState(
                weeds = 3,
                cropIndex = FarmingCrops.index(potato),
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
    fun `grows one stage per stage time`() {
        val planted = planted(potato)
        val after = planted.advance(potato.stageMinutes, never)
        assertEquals(1, after.stage)
        assertEquals(potato.stageMinutes, after.minutes)
    }

    @Test
    fun `stops at full growth and sets a harvest count`() {
        val planted = planted(potato)
        val after = planted.advance(potato.stageMinutes * 10, never)
        assertEquals(potato.stages, after.stage)
        assertTrue(after.grown(potato))
        assertTrue(after.produce > 0)
        assertEquals(0, after.minutes)
    }

    @Test
    fun `an uncured disease kills the crop on the next stage`() {
        val diseased = planted(guam).advance(guam.stageMinutes, always)
        assertEquals(Health.DISEASED, diseased.health)
        val dead = diseased.advance(guam.stageMinutes, never)
        assertEquals(Health.DEAD, dead.health)
    }

    @Test
    fun `watering blocks the disease roll for that stage`() {
        val watered = planted(potato).copy(watered = true)
        assertEquals(Health.HEALTHY, watered.advance(potato.stageMinutes, always).health)
    }

    @Test
    fun `transmit offsets match the cache transform layout`() {
        val growing = planted(potato).advance(potato.stageMinutes, never)
        assertEquals(potato.growBase + 1, growing.transmit())
        assertEquals(potato.growBase + 1 + 64, growing.copy(watered = true).transmit())
        assertEquals(
            potato.growBase + 1 + 128,
            growing.copy(health = Health.DISEASED).transmit(),
        )
        assertEquals(potato.growBase + 1 + 192, growing.copy(health = Health.DEAD).transmit())

        val herb = planted(guam).advance(guam.stageMinutes, never)
        assertEquals(guam.growBase + 1, herb.transmit())
        assertEquals(guam.diseasedBase, herb.copy(health = Health.DISEASED).transmit())
        assertEquals(170, herb.copy(health = Health.DEAD).transmit())
    }

    @Test
    fun `an empty patch transmits its weed level`() {
        assertEquals(0, PatchState().transmit())
        assertEquals(PatchState.CLEARED, PatchState(weeds = PatchState.CLEARED).transmit())
    }

    private fun planted(crop: Crop) =
        PatchState(
            weeds = PatchState.CLEARED,
            cropIndex = FarmingCrops.index(crop),
            minutes = crop.stageMinutes,
        )
}
