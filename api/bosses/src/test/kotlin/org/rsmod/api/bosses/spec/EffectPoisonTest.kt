package org.rsmod.api.bosses.spec

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.rsmod.api.bosses.dsl.poison

class EffectPoisonTest {
    @Test
    fun `poison defaults to the caster severity`() {
        assertEquals(Effect.Poison(), poison())
    }

    @Test
    fun `poison accepts a starting damage or a severity`() {
        assertEquals(Effect.Poison(damage = 8), poison(damage = 8))
        assertEquals(Effect.Poison(severity = 40), poison(severity = 40))
    }

    @Test
    fun `poison rejects damage and severity together`() {
        assertThrows(IllegalArgumentException::class.java) {
            Effect.Poison(damage = 8, severity = 40)
        }
    }
}
