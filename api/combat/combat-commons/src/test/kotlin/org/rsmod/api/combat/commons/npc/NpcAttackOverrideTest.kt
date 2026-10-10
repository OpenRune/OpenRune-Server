package org.rsmod.api.combat.commons.npc

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
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

    @Test
    fun `only a guaranteed max hit skips the accuracy roll`() {
        assertTrue(NpcAttackOverride.MaxHit(18, guaranteed = true).guaranteesHit())
        assertFalse(NpcAttackOverride.MaxHit(18).guaranteesHit())
        assertFalse(NpcAttackOverride.Fixed(6).guaranteesHit())
    }

    @Test
    fun `no override keeps the standard roll`() {
        val none: NpcAttackOverride? = null

        assertEquals(8, none.rollDamage(accurate = true, { standardMaxHit }, roll))
        assertEquals(0, none.rollDamage(accurate = false, { standardMaxHit }, roll))
        assertFalse(none.guaranteesHit())
        assertEquals(0, none.penetration())
    }
}
