package org.rsmod.content.other.special.attacks.ranged

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class ArmadylCrossbowSpecialDamageTest {
    @Test
    fun armadylDoublesEachBoltTypesBaseProcChance() {
        assertEquals(0.10, ArmadylEnchantedBolt.Opal.armadylActivationChance(true), 0.00001)
        assertEquals(0.12, ArmadylEnchantedBolt.Jade.armadylActivationChance(false), 0.00001)
        assertEquals(0.50, ArmadylEnchantedBolt.Sapphire.armadylActivationChance(false), 0.00001)
        assertEquals(0.20, ArmadylEnchantedBolt.Diamond.armadylActivationChance(false), 0.00001)
        assertEquals(0.0, ArmadylEnchantedBolt.Topaz.armadylActivationChance(false), 0.00001)
    }

    @Test
    fun resolverCoversNormalAndDragonEnchantedBoltNames() {
        assertEquals(ArmadylEnchantedBolt.Ruby, ArmadylEnchantedBolt.fromName("Ruby bolts (e)"))
        assertEquals(
            ArmadylEnchantedBolt.Dragonstone,
            ArmadylEnchantedBolt.fromName("Dragonstone dragon bolts (e)"),
        )
        assertEquals(null, ArmadylEnchantedBolt.fromName("Diamond bolts"))
    }

    @Test
    fun fixedBoltDamageHelpersFollowOsrsCapsAndRounding() {
        assertEquals(100, ArmadylCrossbowSpecialDamage.rubyDamage(500))
        assertEquals(19, ArmadylCrossbowSpecialDamage.rubyDamage(99))
        assertEquals(9, ArmadylCrossbowSpecialDamage.rubySelfDamage(99))
        assertEquals(11, ArmadylCrossbowSpecialDamage.opalBonus(119))
        assertEquals(7, ArmadylCrossbowSpecialDamage.pearlBonus(119, fieryTarget = true))
        assertEquals(5, ArmadylCrossbowSpecialDamage.sapphirePrayerDrain(119))
        assertEquals(2, ArmadylCrossbowSpecialDamage.sapphirePrayerRestore(5))
        assertEquals(7, ArmadylCrossbowSpecialDamage.onyxHeal(31))
    }
}
