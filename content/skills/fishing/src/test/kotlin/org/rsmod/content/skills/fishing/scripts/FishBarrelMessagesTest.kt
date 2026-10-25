package org.rsmod.content.skills.fishing.scripts

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class FishBarrelMessagesTest {
    @Test
    fun `an empty barrel says so`() {
        assertEquals(listOf("The barrel is empty."), FishBarrelMessages.checkTexts(emptyList()))
    }

    @Test
    fun `one fish type lists its count after the header`() {
        assertEquals(
            listOf("The barrel contains:<br>3 x Raw shrimps"),
            FishBarrelMessages.checkTexts(listOf("Raw shrimps" to 3)),
        )
    }

    @Test
    fun `several fish types are comma separated`() {
        assertEquals(
            listOf("The barrel contains:<br>1 x Raw anglerfish, 2 x Raw monkfish, 3 x Raw shrimps"),
            FishBarrelMessages.checkTexts(
                listOf("Raw anglerfish" to 1, "Raw monkfish" to 2, "Raw shrimps" to 3)
            ),
        )
    }

    @Test
    fun `more fish types than one box holds continue in a second box`() {
        val entries = (1..10).map { "Raw fish $it" to 1 }
        val texts = FishBarrelMessages.checkTexts(entries)
        assertEquals(2, texts.size)
        assertEquals(true, texts[0].startsWith("The barrel contains:<br>1 x Raw fish 1, "))
        assertEquals("1 x Raw fish 9, 1 x Raw fish 10", texts[1])
    }

    @Test
    fun `the full and destroy texts match the wiki`() {
        assertEquals("The barrel is full. It may be emptied at a bank.", FishBarrelMessages.FULL)
        assertEquals("You may win another barrel from Tempoross.", FishBarrelMessages.DESTROY_EMPTY)
    }
}
