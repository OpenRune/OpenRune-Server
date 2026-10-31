package org.rsmod.content.skills.farming

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class PatchStateTest {
    private val herb =
        Crop(
            index = 9,
            kind = PatchKind.HERB,
            name = "test herb",
            seed = "obj.test_seed",
            produce = "obj.test_produce",
            level = 1,
            plantXp = 1.0,
            harvestXp = 1.0,
            stages = 4,
            cycleMinutes = 20,
            art =
                CropArt(
                    healthy = listOf(4, 5, 6, 7, 8),
                    watered = listOf(14, 15, 16, 17, 8),
                    diseased = listOf(24, 24, 25, 26, 26),
                    dead = listOf(34, 34, 35, 36, 37),
                ),
        )

    @Test
    fun `every field survives a pack round trip`() {
        val state =
            PatchState(
                weeds = 3,
                cropIndex = 127,
                stage = 15,
                watered = true,
                health = Health.DEAD,
                compost = Compost.ULTRA,
                produce = 15,
                phase = Phase.STUMP,
            )
        assertEquals(state, PatchState.unpack(state.pack()))
        assertEquals(PatchState(), PatchState.unpack(PatchState().pack()))
    }

    @Test
    fun `packed layout is stable`() {
        val state =
            PatchState(
                weeds = 2,
                cropIndex = 29,
                stage = 3,
                watered = true,
                health = Health.DISEASED,
                compost = Compost.SUPER,
                produce = 6,
                phase = Phase.CHECKED,
            )
        val expected =
            (1 shl 31) or (2 shl 28) or (1 shl 20) or (6 shl 16) or (2 shl 14) or (1 shl 12) or (1 shl 11) or
                (3 shl 7) or 29
        assertEquals(expected, state.pack())
    }

    @Test
    fun `legacy values keep their fields and drop the old minute countdown`() {
        val legacy =
            (2 shl 28) or (255 shl 20) or (6 shl 16) or (Compost.SUPER.ordinal shl 14) or
                (Health.DISEASED.ordinal shl 12) or (1 shl 11) or (3 shl 7) or 29
        val state = PatchState.unpack(legacy)
        assertEquals(
            PatchState(
                weeds = 2,
                cropIndex = 29,
                stage = 3,
                watered = true,
                health = Health.DISEASED,
                compost = Compost.SUPER,
                produce = 6,
            ),
            state,
        )
        assertEquals(state, PatchState.unpack(state.pack()))
        assertEquals(PatchState(), PatchState.unpack(0))
    }

    @Test
    fun `out of range fields are clamped instead of corrupting neighbours`() {
        val state = PatchState(cropIndex = 200, stage = 40, produce = 99)
        assertEquals(PatchState(cropIndex = 127, stage = 15, produce = 15), PatchState.unpack(state.pack()))
    }

    @Test
    fun `transmit picks dead, diseased, watered then healthy art by stage`() {
        for (stage in 0..4) {
            val state = PatchState(weeds = 3, cropIndex = 9, stage = stage)
            assertEquals(herb.art.healthy[stage], state.transmit(herb))
            assertEquals(herb.art.watered[stage], state.copy(watered = true).transmit(herb))
            assertEquals(herb.art.diseased[stage], state.copy(watered = true, health = Health.DISEASED).transmit(herb))
            assertEquals(herb.art.dead[stage], state.copy(health = Health.DEAD).transmit(herb))
        }
    }

    @Test
    fun `weeds transmit their stroke count and an unknown stage is clamped`() {
        for (weeds in 0..3) assertEquals(weeds, PatchState(weeds = weeds).transmit(null))
        assertEquals(herb.art.healthy[4], PatchState(cropIndex = 9, stage = 15).transmit(herb))
    }

    @Test
    fun `maturity sets one batch for batch crops`() {
        assertEquals(1, PatchState(stage = 4).matured(herb.copy(harvest = HarvestModel.BATCH)).produce)
    }

    @Test
    fun `emptying keeps the patch weeded and forgets everything else`() {
        val state =
            PatchState(
                weeds = 3,
                cropIndex = 9,
                stage = 3,
                compost = Compost.ULTRA,
                health = Health.DEAD,
                produce = 6,
                watered = true,
            )
        assertEquals(PatchState(weeds = 3), state.empty())
    }
}
