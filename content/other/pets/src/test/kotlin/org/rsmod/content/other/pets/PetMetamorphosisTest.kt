package org.rsmod.content.other.pets

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class PetMetamorphosisTest {
    @Test
    fun `Lil Zik cycles through every unlocked form and wraps back to its base form`() {
        val unlocked = BooleanArray(6) { true }

        for (current in unlocked.indices) {
            val next = nextUnlockedFormIndex(unlocked.size, current) { unlocked[it] }
            assertEquals((current + 1) % unlocked.size, next)
        }
    }

    @Test
    fun `metamorphosis skips forms that are still locked`() {
        val unlocked = listOf(true, false, true, false, true)

        assertEquals(2, nextUnlockedFormIndex(unlocked.size, 0) { unlocked[it] })
        assertEquals(4, nextUnlockedFormIndex(unlocked.size, 2) { unlocked[it] })
        assertEquals(0, nextUnlockedFormIndex(unlocked.size, 4) { unlocked[it] })
    }

    @Test
    fun `metamorphosis reports no alternate form when only the current form is unlocked`() {
        val unlocked = listOf(true, false, false)

        assertNull(nextUnlockedFormIndex(unlocked.size, 0) { unlocked[it] })
    }

    @Test
    fun `metamorphosis rejects an unknown current form`() {
        assertNull(nextUnlockedFormIndex(formCount = 6, currentIndex = -1) { true })
    }
}
