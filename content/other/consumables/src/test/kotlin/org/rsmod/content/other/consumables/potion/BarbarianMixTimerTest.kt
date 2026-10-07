package org.rsmod.content.other.consumables.potion

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.rsmod.content.other.consumables.ConsumableType

class BarbarianMixTimerTest {
    @Test
    fun `barbarian mixes share the food timer`() {
        assertEquals(ConsumableType.FOOD, potionConsumableType(mix = true))
    }

    @Test
    fun `other potions keep the potion timer`() {
        assertEquals(ConsumableType.POTION, potionConsumableType(mix = false))
    }
}
