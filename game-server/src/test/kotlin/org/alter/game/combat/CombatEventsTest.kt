package org.alter.game.combat

import org.alter.game.model.combat.CombatStyle
import org.alter.game.model.combat.isMagic
import org.alter.game.model.combat.isMelee
import org.alter.game.model.combat.isRanged
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CombatEventsTest {

    // --- DisengageReason ---

    @Test
    fun `DisengageReason has all four expected values`() {
        val values = DisengageReason.entries
        assertEquals(4, values.size)
        assertTrue(values.contains(DisengageReason.TARGET_DEAD))
        assertTrue(values.contains(DisengageReason.OUT_OF_RANGE))
        assertTrue(values.contains(DisengageReason.MANUAL))
        assertTrue(values.contains(DisengageReason.TIMEOUT))
    }

    @Test
    fun `DisengageReason values are distinct`() {
        val values = DisengageReason.entries
        assertEquals(values.size, values.toSet().size)
    }

    // --- CombatStyle extension functions ---

    @Test
    fun `isMelee returns true for STAB, SLASH, CRUSH`() {
        assertTrue(CombatStyle.STAB.isMelee())
        assertTrue(CombatStyle.SLASH.isMelee())
        assertTrue(CombatStyle.CRUSH.isMelee())
    }

    @Test
    fun `isMelee returns false for RANGED, MAGIC, NONE`() {
        assertFalse(CombatStyle.RANGED.isMelee())
        assertFalse(CombatStyle.MAGIC.isMelee())
        assertFalse(CombatStyle.NONE.isMelee())
    }

    @Test
    fun `isRanged returns true only for RANGED`() {
        assertTrue(CombatStyle.RANGED.isRanged())
        assertFalse(CombatStyle.STAB.isRanged())
        assertFalse(CombatStyle.SLASH.isRanged())
        assertFalse(CombatStyle.CRUSH.isRanged())
        assertFalse(CombatStyle.MAGIC.isRanged())
        assertFalse(CombatStyle.NONE.isRanged())
    }

    @Test
    fun `isMagic returns true only for MAGIC`() {
        assertTrue(CombatStyle.MAGIC.isMagic())
        assertFalse(CombatStyle.STAB.isMagic())
        assertFalse(CombatStyle.SLASH.isMagic())
        assertFalse(CombatStyle.CRUSH.isMagic())
        assertFalse(CombatStyle.RANGED.isMagic())
        assertFalse(CombatStyle.NONE.isMagic())
    }

    @Test
    fun `exactly one of isMelee isRanged isMagic is true for each combat style`() {
        // STAB, SLASH, CRUSH: only melee
        for (style in listOf(CombatStyle.STAB, CombatStyle.SLASH, CombatStyle.CRUSH)) {
            val count = listOf(style.isMelee(), style.isRanged(), style.isMagic()).count { it }
            assertEquals(1, count, "Expected exactly one true for $style")
        }
        // RANGED: only ranged
        assertEquals(1, listOf(CombatStyle.RANGED.isMelee(), CombatStyle.RANGED.isRanged(), CombatStyle.RANGED.isMagic()).count { it })
        // MAGIC: only magic
        assertEquals(1, listOf(CombatStyle.MAGIC.isMelee(), CombatStyle.MAGIC.isRanged(), CombatStyle.MAGIC.isMagic()).count { it })
        // NONE: none of them
        assertEquals(0, listOf(CombatStyle.NONE.isMelee(), CombatStyle.NONE.isRanged(), CombatStyle.NONE.isMagic()).count { it })
    }
}
