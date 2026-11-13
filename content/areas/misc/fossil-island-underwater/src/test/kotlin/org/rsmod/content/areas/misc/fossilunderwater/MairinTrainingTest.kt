package org.rsmod.content.areas.misc.fossilunderwater

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

class MairinTrainingTest {
    @Test
    fun `at 99 a tear pays the wiki's rates`() {
        assertEquals(2646, MairinTraining.agilityTenths(99, both = false))
        assertEquals(9702, MairinTraining.thievingTenths(99, both = false))
        assertEquals(1764, MairinTraining.agilityTenths(99, both = true))
        assertEquals(6468, MairinTraining.thievingTenths(99, both = true))
    }

    @Test
    fun `the rates shown are whole xp a tear`() {
        assertEquals(264, MairinTraining.agilityTenths(99, both = false) / 10)
        assertEquals(970, MairinTraining.thievingTenths(99, both = false) / 10)
        assertEquals(176, MairinTraining.agilityTenths(99, both = true) / 10)
        assertEquals(646, MairinTraining.thievingTenths(99, both = true) / 10)
    }

    @Test
    fun `eighty-three tears at 62 agility pay 5735 agility xp`() {
        val perTear = MairinTraining.agilityTenths(62, both = true)
        assertEquals(691, perTear)
        assertEquals(5735, perTear * 83 / 10)
    }

    @Test
    fun `three tears at 99 pay 529 and 1940 xp`() {
        assertEquals(529, MairinTraining.agilityTenths(99, both = true) * 3 / 10)
        assertEquals(1940, MairinTraining.thievingTenths(99, both = true) * 3 / 10)
    }

    @ParameterizedTest
    @CsvSource("1, 1, 1", "1, 0, 0", "5, 3, 3", "700, 900, 700", "900, 900, 700", "900, 800, 700")
    fun `no more than 700 tears are paid for`(requested: Int, carried: Int, paid: Int) {
        assertEquals(paid, MairinTraining.paidTears(requested, carried))
    }

    @ParameterizedTest
    @CsvSource("1, 1, 1", "5, 3, 3", "900, 900, 900", "800, 900, 800")
    fun `the tears handed over beyond 700 are lost`(requested: Int, carried: Int, taken: Int) {
        assertEquals(taken, MairinTraining.takenTears(requested, carried))
    }
}
