package org.rsmod.content.skills.farming

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class FarmingSpiritTreesTest {
    @Test
    fun `a held producer does not disable an existing checked spirit tree`() {
        val crop = spirit()
        assertTrue(spiritTreeOperational(PatchState(stage = crop.stages, phase = Phase.CHECKED), crop))
    }

    @Test
    fun `unchecked immature unhealthy and cleared trees cannot provide transport`() {
        val crop = spirit()
        val grown = PatchState(stage = crop.stages, phase = Phase.CHECKED)
        assertFalse(spiritTreeOperational(grown.copy(phase = Phase.GROWING), crop))
        assertFalse(spiritTreeOperational(grown.copy(stage = crop.stages - 1), crop))
        assertFalse(spiritTreeOperational(grown.copy(phase = Phase.STUMP), crop))
        assertFalse(spiritTreeOperational(grown.copy(health = Health.DISEASED), crop))
        assertFalse(spiritTreeOperational(grown.copy(health = Health.DEAD), crop))
        assertFalse(spiritTreeOperational(grown.empty(), null))
    }

    @Test
    fun `a checked crop of another kind cannot unlock a spirit destination`() {
        val crop = spirit().copy(kind = PatchKind.TREE)
        assertFalse(spiritTreeOperational(PatchState(stage = crop.stages, phase = Phase.CHECKED), crop))
    }

    private fun spirit() = Crop(
        index = 1,
        kind = PatchKind.SPIRIT_TREE,
        name = "spirit tree",
        seed = "obj.plantpot_spirit_tree_sapling",
        produce = null,
        level = 83,
        plantXp = 0.0,
        harvestXp = 0.0,
        stages = 12,
        cycleMinutes = 320,
        art = CropArt(emptyList()),
        harvest = HarvestModel.NONE,
        held = "Disease and death remain unsourced",
    )
}
