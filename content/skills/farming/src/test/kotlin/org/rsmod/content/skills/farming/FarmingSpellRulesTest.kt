package org.rsmod.content.skills.farming

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class FarmingSpellRulesTest {
    @Test
    fun `Geomancy keeps minimum yield growth and water in separate client coordinate fields`() {
        val packed = FarmingGeomancy.packedProgress(15, 16, 255, true)
        val x = packed ushr 14 and 0x3fff
        val z = packed and 0x3fff
        assertEquals(15, x % 64)
        assertEquals(255, x / 64)
        assertEquals(16, z % 64)
        assertEquals(1, packed ushr 28 and 3)
        assertEquals(0, FarmingGeomancy.packedProgress(0, 0, 0, false))
    }

    @Test
    fun `Geomancy shows real health and compost without inventing undead protection`() {
        val protected = PatchState(health = Health.DISEASED, compost = Compost.ULTRA, paid = true)
        assertEquals(1 or 4 or 8 or 16 or 128 or 256, FarmingGeomancy.flags(protected, true, true, true))
        assertEquals(2 or 32, FarmingGeomancy.flags(PatchState(health = Health.DEAD, compost = Compost.NORMAL), false, false, false))
        assertEquals(64, FarmingGeomancy.flags(PatchState(compost = Compost.SUPER), false, false, false))
        assertEquals(0, FarmingGeomancy.flags(PatchState(), false, false, false))
    }
}
