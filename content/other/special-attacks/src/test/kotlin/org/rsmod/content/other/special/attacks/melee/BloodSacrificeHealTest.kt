package org.rsmod.content.other.special.attacks.melee

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class BloodSacrificeHealTest {
    @Test
    fun `heals the full damage dealt when under both caps`() {
        assertEquals(15, BloodSacrificeHeal.healAmount(damage = 15, targetBaseHitpoints = 200, healCap = 15))
    }

    @Test
    fun `caps at fifteen percent of the target's base hitpoints`() {
        // 15% of 80 base HP = 12, below the flat cap, so the percent cap is the binding one.
        assertEquals(12, BloodSacrificeHeal.healAmount(damage = 25, targetBaseHitpoints = 80, healCap = 25))
    }

    @Test
    fun `caps at the flat player heal cap against a high-hp target`() {
        // 15% of 990 base HP would be 148, far above the real 15-hp player cap.
        assertEquals(15, BloodSacrificeHeal.healAmount(damage = 25, targetBaseHitpoints = 990, healCap = 15))
    }

    @Test
    fun `never heals more than the damage actually dealt`() {
        assertEquals(3, BloodSacrificeHeal.healAmount(damage = 3, targetBaseHitpoints = 990, healCap = 25))
    }

    @Test
    fun `zero damage heals nothing`() {
        assertEquals(0, BloodSacrificeHeal.healAmount(damage = 0, targetBaseHitpoints = 200, healCap = 15))
    }
}
