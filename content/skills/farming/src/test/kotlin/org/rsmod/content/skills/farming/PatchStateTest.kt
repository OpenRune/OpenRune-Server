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

    private val bush =
        herb.copy(
            kind = PatchKind.BUSH,
            harvest = HarvestModel.PICK,
            checkXp = 10.0,
            art = CropArt(healthy = listOf(5, 6, 7, 8, 9), produce = listOf(10, 11, 12, 13, 14), stump = 40),
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
                paid = true,
                resurrected = true,
                scarecrow = true,
                fertilised = true,
                grapePrepared = true,
            )
        assertEquals(state, PatchState.unpack(state.pack()))
        assertEquals(PatchState(), PatchState.unpack(PatchState().pack()))
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
            assertEquals(herb.art.healthy[stage], state.transmit(herb, herb.kind))
            assertEquals(herb.art.watered[stage], state.copy(watered = true).transmit(herb, herb.kind))
            assertEquals(herb.art.diseased[stage], state.copy(watered = true, health = Health.DISEASED).transmit(herb, herb.kind))
            assertEquals(herb.art.dead[stage], state.copy(health = Health.DEAD).transmit(herb, herb.kind))
        }
    }

    @Test
    fun `weeds transmit their stroke count and an unknown stage is clamped`() {
        for (weeds in 0..3) assertEquals(weeds, PatchState(weeds = weeds).transmit(null, PatchKind.ALLOTMENT))
        assertEquals(herb.art.healthy[4], PatchState(cropIndex = 9, stage = 15).transmit(herb, herb.kind))
    }

    @Test
    fun `empty grape soil uses native untreated or prepared art regardless of legacy weeds`() {
        for (weeds in 0..3) {
            val soil = PatchState(weeds = weeds)
            assertEquals(0, soil.transmit(null, PatchKind.GRAPE_VINE))
            assertEquals(1, soil.copy(grapePrepared = true).transmit(null, PatchKind.GRAPE_VINE))
            assertEquals(0, soil.copy(fertilised = true).transmit(null, PatchKind.GRAPE_VINE))
        }
    }

    @Test
    fun `check-health crops show mature art until checked, then remaining produce`() {
        val grown = PatchState(weeds = 3, cropIndex = 9, stage = 4)
        assertEquals(bush.art.healthy[4], grown.transmit(bush, bush.kind))
        assertEquals(14, grown.copy(phase = Phase.CHECKED, produce = 4).transmit(bush, bush.kind))
        assertEquals(10, grown.copy(phase = Phase.CHECKED, produce = 0).transmit(bush, bush.kind))
        assertEquals(14, grown.copy(phase = Phase.CHECKED, produce = 9).transmit(bush, bush.kind))
        assertEquals(40, grown.copy(phase = Phase.STUMP).transmit(bush, bush.kind))
        val mushroom = bush.copy(checkXp = 0.0)
        assertEquals(12, grown.copy(produce = 2).transmit(mushroom, mushroom.kind))
    }

    @Test
    fun `maturity sets fixed lives, one batch, or a full renewable crop when unchecked`() {
        val lives = herb.copy(harvest = HarvestModel.LIVES, harvestLives = 5)
        assertEquals(5, PatchState(stage = 4).matured(lives).produce)
        assertEquals(1, PatchState(stage = 4).matured(herb.copy(harvest = HarvestModel.BATCH)).produce)
        assertEquals(0, PatchState(stage = 4).matured(bush).produce)
        assertEquals(4, PatchState(stage = 4).matured(bush.copy(checkXp = 0.0)).produce)
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
                paid = true,
                resurrected = true,
            )
        assertEquals(PatchState(weeds = 3), state.empty())
    }
}
