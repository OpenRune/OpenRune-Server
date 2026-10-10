package org.rsmod.content.slayer

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class SlayerTaskChoiceInterfaceTest {
    @Test
    fun `a click on a row background picks that row`() {
        assertEquals(0, SlayerTaskChoiceInterface.resolveSlot(0, 3))
        assertEquals(1, SlayerTaskChoiceInterface.resolveSlot(10, 3))
        assertEquals(2, SlayerTaskChoiceInterface.resolveSlot(20, 3))
    }

    @Test
    fun `a click on a row detail or a row without an offer picks nothing`() {
        assertNull(SlayerTaskChoiceInterface.resolveSlot(1, 3))
        assertNull(SlayerTaskChoiceInterface.resolveSlot(19, 3))
        assertNull(SlayerTaskChoiceInterface.resolveSlot(20, 2))
        assertNull(SlayerTaskChoiceInterface.resolveSlot(-10, 3))
    }
}
