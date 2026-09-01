package org.rsmod.content.other.special.attacks.melee

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class BoneDaggerBackstabTest {
    @Test
    fun `guarantees accuracy when another player was the last positive damager`() {
        val source = 1L

        assertTrue(
            BoneDaggerBackstab.isUnsuspecting(
                lastDamagingPlayerUuid = 2L,
                sourceUuid = source,
            ),
        )
    }

    @Test
    fun `uses normal accuracy after this player dealt the last positive damage`() {
        val source = 1L

        assertFalse(
            BoneDaggerBackstab.isUnsuspecting(
                lastDamagingPlayerUuid = source,
                sourceUuid = source,
            ),
        )
    }

    @Test
    fun `allows the guarantee while a player uuid is unavailable`() {
        assertTrue(
            BoneDaggerBackstab.isUnsuspecting(
                lastDamagingPlayerUuid = 2L,
                sourceUuid = null,
            ),
        )
    }
}
