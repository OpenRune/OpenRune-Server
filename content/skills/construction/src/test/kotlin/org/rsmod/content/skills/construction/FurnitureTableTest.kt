package org.rsmod.content.skills.construction

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

/** Item ids are this cache's own; the xp values are the ones OSRS awards. */
class FurnitureTableTest {
    @Test
    fun `xp comes from the reference table`() {
        assertEquals(58.0, furnitureXp(8309))
        assertEquals(120.0, furnitureXp(8312))
        assertEquals(280.0, furnitureXp(8315))
    }

    @Test
    fun `furniture outside the table falls back to its materials`() {
        assertNull(furnitureXp(-1))
        assertNull(furnitureXp(0))
    }
}
