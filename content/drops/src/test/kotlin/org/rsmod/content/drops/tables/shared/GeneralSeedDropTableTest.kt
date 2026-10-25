package org.rsmod.content.drops.tables.shared

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class GeneralSeedDropTableTest {
    @Test
    fun `each seed family starts at its threshold rather than one roll later`() {
        assertEquals(0, generalSeedTier(0))
        for (boundary in listOf(485, 728, 850, 947, 995)) {
            assertEquals(generalSeedTier(boundary - 1) + 1, generalSeedTier(boundary))
        }
        assertEquals(5, generalSeedTier(Int.MAX_VALUE))
    }
}
