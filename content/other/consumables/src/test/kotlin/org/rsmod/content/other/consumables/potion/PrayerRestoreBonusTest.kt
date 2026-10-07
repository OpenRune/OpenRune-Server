package org.rsmod.content.other.consumables.potion

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

class PrayerRestoreBonusTest {
    @ParameterizedTest(name = "{0} at Prayer {1}: {4} without, {5} with")
    @CsvSource(
        "prayer potion, 99, 7, 25, 31, 33",
        "super restore, 99, 8, 25, 32, 34",
        "sanfew serum, 99, 4, 30, 33, 35",
        "prayer potion, 70, 7, 25, 24, 25",
        "prayer potion, 3, 7, 25, 7, 7",
    )
    fun `the bonus raises the prayer percentage by two`(
        name: String,
        level: Int,
        constant: Int,
        percent: Int,
        without: Int,
        with: Int,
    ) {
        assertEquals(without, restore(level, constant, PrayerRestoreBonus.percent(percent, false)))
        assertEquals(with, restore(level, constant, PrayerRestoreBonus.percent(percent, true)))
    }

    @Test
    fun `a holy wrench counts only when carried`() {
        assertTrue(applies(carried = setOf("obj.deal_wrench_blessed")))
        assertFalse(applies(worn = setOf("obj.deal_wrench_blessed")))
    }

    @Test
    fun `ring of the gods (i) counts only when worn`() {
        listOf("obj.nzone_rotg", "obj.sw_rotg", "obj.pvpa_rotg").forEach { ring ->
            assertTrue(applies(worn = setOf(ring)), ring)
            assertFalse(applies(carried = setOf(ring)), ring)
        }
        assertFalse(applies(worn = setOf("obj.rotg")))
    }

    @Test
    fun `prayer and max capes count worn or carried`() {
        listOf("obj.skillcape_prayer", "obj.skillcape_prayer_trimmed", "obj.skillcape_max")
            .forEach { cape ->
                assertTrue(applies(worn = setOf(cape)), cape)
                assertTrue(applies(carried = setOf(cape)), cape)
            }
        assertFalse(applies(worn = setOf("obj.skillcape_max_infernalcape")))
    }

    @Test
    fun `sources do not stack`() {
        val all =
            PrayerRestoreBonus.applies(
                isWorn = { it in PrayerRestoreBonus.WORN },
                isCarried = { it in PrayerRestoreBonus.CARRIED },
            )
        assertEquals(27, PrayerRestoreBonus.percent(25, all))
    }

    private fun applies(worn: Set<String> = emptySet(), carried: Set<String> = emptySet()) =
        PrayerRestoreBonus.applies(isWorn = { it in worn }, isCarried = { it in carried })

    private fun restore(level: Int, constant: Int, percent: Int): Int =
        constant + level * percent / 100
}
