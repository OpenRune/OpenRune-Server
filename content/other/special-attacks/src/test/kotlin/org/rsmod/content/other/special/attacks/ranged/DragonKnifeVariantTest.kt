package org.rsmod.content.other.special.attacks.ranged

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class DragonKnifeVariantTest {
    @Test
    fun `plain dragon knife is not poisoned`() {
        assertFalse(DragonKnifeVariant.isPoisoned("Dragon knife"))
    }

    @Test
    fun `poisoned variants are detected regardless of dose`() {
        assertTrue(DragonKnifeVariant.isPoisoned("Dragon knife(p)"))
        assertTrue(DragonKnifeVariant.isPoisoned("Dragon knife(p+)"))
        assertTrue(DragonKnifeVariant.isPoisoned("Dragon knife(p++)"))
    }

    @Test
    fun `detection is case-insensitive`() {
        assertTrue(DragonKnifeVariant.isPoisoned("DRAGON KNIFE(P++)"))
    }
}
