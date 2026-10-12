package org.rsmod.game.stat

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

class PlayerSkillXPTableTest {
    @ParameterizedTest(name = "level {0} starts at {1} xp")
    @CsvSource(
        "1, 0",
        "2, 83",
        "3, 174",
        "10, 1154",
        "50, 101333",
        "92, 6517253",
        "99, 13034431",
        "100, 14391160",
    )
    fun `each level starts at its wiki xp threshold`(level: Int, xp: Int) {
        // when
        val threshold = PlayerSkillXPTable.getXPFromLevel(level)

        // then
        assertEquals(xp, threshold)
    }

    @ParameterizedTest(name = "{0} xp is level {1}")
    @CsvSource(
        "0, 1",
        "82, 1",
        "83, 2",
        "84, 2",
        "173, 2",
        "174, 3",
        "13034430, 98",
        "13034431, 99",
        "13034432, 99",
    )
    fun `xp around a threshold maps to the right level`(xp: Int, level: Int) {
        // when
        val result = PlayerSkillXPTable.getLevelFromXP(xp)

        // then
        assertEquals(level, result)
    }

    @Test
    fun `every threshold round trips back to its own level`() {
        // given
        val levels = 1..PlayerSkillXPTable.XP_TABLE.size

        // when
        val roundTripped = levels.map { PlayerSkillXPTable.getLevelFromXP(xpOf(it)) }

        // then
        assertEquals(levels.toList(), roundTripped)
    }

    @Test
    fun `xp beyond the table caps at the highest level`() {
        // when
        val level = PlayerSkillXPTable.getLevelFromXP(Int.MAX_VALUE)

        // then
        assertEquals(PlayerSkillXPTable.XP_TABLE.size, level)
    }

    @Test
    fun `fine xp is converted before the level lookup`() {
        // given
        val fineXp = PlayerSkillXPTable.getFineXPFromLevel(99)

        // when
        val level = PlayerSkillXPTable.getLevelFromFineXP(fineXp)

        // then
        assertEquals(99, level)
    }

    @ParameterizedTest(name = "level {0} is out of range")
    @CsvSource("0", "-1", "127")
    fun `levels outside the table are rejected`(level: Int) {
        // when / then
        assertThrows<IllegalArgumentException> { PlayerSkillXPTable.getXPFromLevel(level) }
    }

    private fun xpOf(level: Int): Int = PlayerSkillXPTable.getXPFromLevel(level)
}
