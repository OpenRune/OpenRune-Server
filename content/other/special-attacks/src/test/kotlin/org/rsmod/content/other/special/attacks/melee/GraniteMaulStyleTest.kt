package org.rsmod.content.other.special.attacks.melee

import org.junit.jupiter.api.Assertions.assertEquals
import org.rsmod.api.combat.commons.CombatStance
import org.rsmod.api.combat.commons.styles.MeleeAttackStyle
import org.junit.jupiter.api.Test

class GraniteMaulStyleTest {
    @Test
    fun `stance 1 maps to accurate`() {
        assertEquals(MeleeAttackStyle.Accurate, GraniteMaulStyle.resolve(CombatStance.Stance1))
    }

    @Test
    fun `stance 2 maps to aggressive`() {
        assertEquals(MeleeAttackStyle.Aggressive, GraniteMaulStyle.resolve(CombatStance.Stance2))
    }

    @Test
    fun `stances 3 and 4 map to defensive`() {
        assertEquals(MeleeAttackStyle.Defensive, GraniteMaulStyle.resolve(CombatStance.Stance3))
        assertEquals(MeleeAttackStyle.Defensive, GraniteMaulStyle.resolve(CombatStance.Stance4))
    }

    @Test
    fun `an unrecognized stance falls back to defensive`() {
        assertEquals(MeleeAttackStyle.Defensive, GraniteMaulStyle.resolve(null))
    }
}
