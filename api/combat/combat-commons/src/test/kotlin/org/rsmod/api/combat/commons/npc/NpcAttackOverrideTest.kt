package org.rsmod.api.combat.commons.npc

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class NpcAttackOverrideTest {
    private val roll: (Int) -> Int = { maxHit -> maxHit }
    private val standardMaxHit = 8

    @Test
    fun `a fixed hit lands whether or not the accuracy roll succeeds`() {
        val override = NpcAttackOverride.Fixed(14, penetration = 100)

        assertEquals(14, override.rollDamage(accurate = false, { standardMaxHit }, roll))
        assertEquals(14, override.rollDamage(accurate = true, { standardMaxHit }, roll))
        assertEquals(100, override.penetration())
    }

    @Test
    fun `a max hit override replaces the standard max hit only on an accurate hit`() {
        val override = NpcAttackOverride.MaxHit(15)

        assertEquals(15, override.rollDamage(accurate = true, { standardMaxHit }, roll))
        assertEquals(0, override.rollDamage(accurate = false, { standardMaxHit }, roll))
        assertEquals(0, override.penetration())
    }
}
