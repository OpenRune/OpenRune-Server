package org.rsmod.content.drops.tables.monsters

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class GiantMoleDropTableTest {
    @Test
    fun `hard falador diary notes mole parts`() {
        assertEquals("obj.cert_mole_claw", notedMolePart("obj.cert_mole_claw", true))
        assertEquals("obj.cert_mole_skin", notedMolePart("obj.cert_mole_skin", true))
    }

    @Test
    fun `without the diary mole parts stay unnoted`() {
        assertNull(notedMolePart("obj.cert_mole_claw", false))
        assertNull(notedMolePart("obj.cert_mole_skin", false))
    }
}
