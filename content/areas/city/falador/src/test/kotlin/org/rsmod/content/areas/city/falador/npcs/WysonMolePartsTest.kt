package org.rsmod.content.areas.city.falador.npcs

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class WysonMolePartsTest {
    @Test
    fun `nest rolls use the osrs 72 20 8 split`() {
        assertEquals(WysonNest.SEEDS, WysonMoleParts.nestForRoll(0))
        assertEquals(WysonNest.SEEDS, WysonMoleParts.nestForRoll(71))
        assertEquals(WysonNest.RING, WysonMoleParts.nestForRoll(72))
        assertEquals(WysonNest.RING, WysonMoleParts.nestForRoll(91))
        assertEquals(WysonNest.EMPTY, WysonMoleParts.nestForRoll(92))
        assertEquals(WysonNest.EMPTY, WysonMoleParts.nestForRoll(99))
    }

    @Test
    fun `immaculate skins are worth five nests`() {
        assertEquals(7, MoleParts(claws = 1, skins = 1, immaculate = 1).rolls)
        assertEquals(5, MoleParts(claws = 0, skins = 0, immaculate = 1).rolls)
        assertEquals(0, MoleParts(claws = 0, skins = 0, immaculate = 0).rolls)
    }
}
