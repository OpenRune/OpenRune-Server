package org.rsmod.content.skills.farming

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * Crops are declared here rather than read from `dbtable.farming_crop` so the growth and packing
 * rules are checked without a loaded cache. The values mirror the potato and guam rows.
 */
class PatchStateTest {
    private val potato =
        Crop(
            kind = PatchKind.ALLOTMENT,
            name = "potato",
            seed = "obj.potato_seed",
            produce = "obj.potato",
            level = 1,
            plantXp = 8.0,
            harvestXp = 9.0,
            growBase = 6,
            stages = 4,
            stageMinutes = 10,
            seedsPerPlant = 3,
        )

    private val guam =
        Crop(
            kind = PatchKind.HERB,
            name = "guam",
            seed = "obj.guam_seed",
            produce = "obj.unidentified_guam",
            level = 9,
            plantXp = 11.0,
            harvestXp = 12.5,
            growBase = 4,
            stages = 4,
            stageMinutes = 20,
            diseasedBase = 128,
        )

    private val snapeGrass =
        Crop(
            kind = PatchKind.ALLOTMENT,
            name = "snape grass",
            seed = "obj.snape_grass_seed",
            produce = "obj.snape_grass",
            level = 61,
            plantXp = 82.0,
            harvestXp = 82.0,
            growBase = 128,
            stages = 7,
            stageMinutes = 10,
            seedsPerPlant = 3,
            transmit =
                listOf(
                    128, 129, 130, 131, 132, 133, 134, 138,
                    63, 64, 65, 66, 67, 68, 69, 138,
                    196, 196, 197, 198, 202, 203, 204, 204,
                    193, 193, 194, 195, 209, 210, 211, 211,
                ),
        )

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
    fun `grows one stage per stage time`() {
        val planted = planted(potato)
        val after = planted.advance(potato, potato.stageMinutes, never)
        assertEquals(1, after.stage)
        assertEquals(potato.stageMinutes, after.minutes)
    }

    @Test
    fun `stops at full growth and sets a harvest count`() {
        val planted = planted(potato)
        val after = planted.advance(potato, potato.stageMinutes * 10, never)
        assertEquals(potato.stages, after.stage)
        assertTrue(after.grown(potato))
        assertTrue(after.produce > 0)
        assertEquals(0, after.minutes)
    }

    @Test
    fun `an uncured disease kills the crop on the next stage`() {
        val diseased = planted(guam).advance(guam, guam.stageMinutes, always)
        assertEquals(Health.DISEASED, diseased.health)
        val dead = diseased.advance(guam, guam.stageMinutes, never)
        assertEquals(Health.DEAD, dead.health)
    }

    @Test
    fun `watering blocks the disease roll for that stage`() {
        val watered = planted(potato).copy(watered = true)
        assertEquals(Health.HEALTHY, watered.advance(potato, potato.stageMinutes, always).health)
    }

    @Test
    fun `transmit offsets match the cache transform layout`() {
        val growing = planted(potato).advance(potato, potato.stageMinutes, never)
        assertEquals(potato.growBase + 1, growing.transmit(potato))
        assertEquals(potato.growBase + 1 + 64, growing.copy(watered = true).transmit(potato))
        assertEquals(
            potato.growBase + 1 + 128,
            growing.copy(health = Health.DISEASED).transmit(potato),
        )
        assertEquals(potato.growBase + 1 + 192, growing.copy(health = Health.DEAD).transmit(potato))

        val herb = planted(guam).advance(guam, guam.stageMinutes, never)
        assertEquals(guam.growBase + 1, herb.transmit(guam))
        assertEquals(guam.diseasedBase, herb.copy(health = Health.DISEASED).transmit(guam))
        assertEquals(170, herb.copy(health = Health.DEAD).transmit(guam))
    }

    @Test
    fun `a scattered crop transmits the slot the cache gave that state`() {
        val growing = planted(snapeGrass).advance(snapeGrass, snapeGrass.stageMinutes, never)
        assertEquals(129, growing.transmit(snapeGrass))
        assertEquals(64, growing.copy(watered = true).transmit(snapeGrass))
        assertEquals(196, growing.copy(health = Health.DISEASED).transmit(snapeGrass))
        assertEquals(193, growing.copy(health = Health.DEAD).transmit(snapeGrass))

        val late = growing.copy(stage = 5)
        assertEquals(203, late.copy(health = Health.DISEASED).transmit(snapeGrass))
        assertEquals(210, late.copy(health = Health.DEAD).transmit(snapeGrass))

        assertEquals(138, growing.copy(stage = snapeGrass.stages).transmit(snapeGrass))
    }

    @Test
    fun `an empty patch transmits its weed level`() {
        assertEquals(0, PatchState().transmit(null))
        assertEquals(PatchState.CLEARED, PatchState(weeds = PatchState.CLEARED).transmit(null))
    }

    private fun planted(crop: Crop) =
        PatchState(weeds = PatchState.CLEARED, cropIndex = 1, minutes = crop.stageMinutes)
}
