package org.rsmod.content.other.special.attacks.magic

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class NightmareStaffSpecialDamageTest {
    @Test
    fun volatileSpecialUsesVisibleMagicScalingAndLevel98Cap() {
        assertEquals(49, NightmareStaffSpecialDamage.volatileBaseMaxHit(82))
        assertEquals(53, NightmareStaffSpecialDamage.volatileBaseMaxHit(89))
        assertEquals(57, NightmareStaffSpecialDamage.volatileBaseMaxHit(96))
        assertEquals(58, NightmareStaffSpecialDamage.volatileBaseMaxHit(98))
        assertEquals(58, NightmareStaffSpecialDamage.volatileBaseMaxHit(120))
    }

    @Test
    fun eldritchSpecialUsesVisibleMagicScalingAndLevel97Cap() {
        assertEquals(37, NightmareStaffSpecialDamage.eldritchBaseMaxHit(82))
        assertEquals(41, NightmareStaffSpecialDamage.eldritchBaseMaxHit(90))
        assertEquals(43, NightmareStaffSpecialDamage.eldritchBaseMaxHit(96))
        assertEquals(44, NightmareStaffSpecialDamage.eldritchBaseMaxHit(97))
        assertEquals(44, NightmareStaffSpecialDamage.eldritchBaseMaxHit(120))
    }

    @Test
    fun baseHitRemainsDefinedForInvalidlyLowVisibleMagic() {
        assertEquals(1, NightmareStaffSpecialDamage.volatileBaseMaxHit(0))
        assertEquals(1, NightmareStaffSpecialDamage.eldritchBaseMaxHit(-1))
    }
}
