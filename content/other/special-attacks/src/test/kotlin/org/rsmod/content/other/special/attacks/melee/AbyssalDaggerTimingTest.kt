package org.rsmod.content.other.special.attacks.melee

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class AbyssalDaggerTimingTest {
    @Test
    fun `second hit lands 1 tick later against a player`() {
        assertEquals(1, AbyssalDaggerTiming.secondHitDelay(targetIsPlayer = true))
    }

    @Test
    fun `second hit lands 2 ticks later against an npc`() {
        assertEquals(2, AbyssalDaggerTiming.secondHitDelay(targetIsPlayer = false))
    }
}
