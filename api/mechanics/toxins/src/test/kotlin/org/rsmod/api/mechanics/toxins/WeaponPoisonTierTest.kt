package org.rsmod.api.mechanics.toxins

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class WeaponPoisonTierTest {
    @Test
    fun `bare (p) suffix resolves to the regular tier`() {
        assertEquals(WeaponPoisonTier.Regular, WeaponPoisonTier.of("obj.dragon_dagger_p"))
        assertEquals(4, WeaponPoisonTier.of("obj.dragon_dagger_p")?.meleeDamage)
        assertEquals(2, WeaponPoisonTier.of("obj.rune_dart_p")?.rangedDamage)
    }

    @Test
    fun `(p+) suffix resolves to the plus tier and not the regular tier`() {
        assertEquals(WeaponPoisonTier.Plus, WeaponPoisonTier.of("obj.rune_spear_p+"))
        assertEquals(5, WeaponPoisonTier.of("obj.rune_spear_p+")?.meleeDamage)
        assertEquals(3, WeaponPoisonTier.of("obj.rune_dart_p+")?.rangedDamage)
    }

    @Test
    fun `(p++) suffix resolves to the strongest tier`() {
        assertEquals(WeaponPoisonTier.PlusPlus, WeaponPoisonTier.of("obj.abyssal_dagger_p++"))
        assertEquals(6, WeaponPoisonTier.of("obj.abyssal_dagger_p++")?.meleeDamage)
        assertEquals(4, WeaponPoisonTier.of("obj.rune_dart_p++")?.rangedDamage)
    }

    @Test
    fun `an unpoisoned weapon resolves to no tier`() {
        assertNull(WeaponPoisonTier.of("obj.dragon_dagger"))
        assertNull(WeaponPoisonTier.of("obj.rune_spear"))
    }

    @Test
    fun `a null alias resolves to no tier`() {
        assertNull(WeaponPoisonTier.of(null))
    }

    @Test
    fun `an unrelated name ending in p is not mistaken for poison`() {
        // Regression guard for the suffix-matching approach: only an exact "_p"/"_p+"/"_p++"
        // trailer should match, not any name that merely contains "p" near the end.
        assertNull(WeaponPoisonTier.of("obj.iron_dagger_pouch"))
    }
}
