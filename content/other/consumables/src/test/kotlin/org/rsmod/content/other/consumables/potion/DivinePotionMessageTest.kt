package org.rsmod.content.other.consumables.potion

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class DivinePotionMessageTest {
    @Test
    fun `refusal matches the wiki text`() {
        assertEquals(
            "You need more than 10 hitpoints to survive the power of a divine potion.",
            divineRefusalMessage(10),
        )
    }
}
