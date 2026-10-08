package org.rsmod.api.stats.plugin.levelup

import dev.openrune.tables.LevelUpRule
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class LevelUpJingleTest {
    private fun jingle(rule: LevelUpRule, threshold: Int? = null) =
        LevelUpJingle(rule, NORMAL, ALTERNATE, threshold)

    private fun LevelUpJingle.pick(
        level: Int,
        maxLevel: Int = 99,
        unlocks: Boolean = false,
        guideList: Boolean = true,
    ): Int = select(level, maxLevel, unlocks, guideList)

    @Test
    fun `unlock levels play the alternate jingle only with the guide list enabled`() {
        val jingle = jingle(LevelUpRule.Unlocks)
        assertEquals(NORMAL, jingle.pick(14))
        assertEquals(ALTERNATE, jingle.pick(15, unlocks = true))
        assertEquals(NORMAL, jingle.pick(15, unlocks = true, guideList = false))
    }

    @Test
    fun `guide list rule follows the setting alone`() {
        val jingle = jingle(LevelUpRule.GuideList)
        assertEquals(ALTERNATE, jingle.pick(2))
        assertEquals(NORMAL, jingle.pick(2, guideList = false))
    }

    @Test
    fun `single rule never plays an alternate`() {
        val jingle = LevelUpJingle(LevelUpRule.Single, NORMAL, alternate = null, threshold = null)
        assertEquals(NORMAL, jingle.pick(62, unlocks = true))
    }

    @Test
    fun `from level rule switches at its threshold`() {
        val jingle = jingle(LevelUpRule.FromLevel, threshold = 50)
        assertEquals(NORMAL, jingle.pick(49))
        assertEquals(ALTERNATE, jingle.pick(50))
        assertEquals(ALTERNATE, jingle.pick(99))
    }

    @Test
    fun `parity rule plays the alternate on odd levels`() {
        val jingle = jingle(LevelUpRule.Parity)
        assertEquals(NORMAL, jingle.pick(2))
        assertEquals(ALTERNATE, jingle.pick(3))
    }

    @Test
    fun `every tenth rule plays the alternate on multiples of ten`() {
        val jingle = jingle(LevelUpRule.EveryTenth)
        assertEquals(NORMAL, jingle.pick(11))
        assertEquals(ALTERNATE, jingle.pick(20))
    }

    @Test
    fun `max level rule plays the alternate at the stat cap`() {
        val jingle = jingle(LevelUpRule.MaxLevel)
        assertEquals(NORMAL, jingle.pick(98))
        assertEquals(ALTERNATE, jingle.pick(99))
        assertEquals(ALTERNATE, jingle.pick(120, maxLevel = 120))
        assertEquals(NORMAL, jingle.pick(99, maxLevel = 120))
    }

    @Test
    fun `incomplete jingle data is rejected`() {
        assertThrows(IllegalArgumentException::class.java) {
            LevelUpJingle(LevelUpRule.Unlocks, NORMAL, alternate = null, threshold = null)
        }
        assertThrows(IllegalArgumentException::class.java) {
            LevelUpJingle(LevelUpRule.FromLevel, NORMAL, ALTERNATE, threshold = null)
        }
    }

    private companion object {
        const val NORMAL = 100
        const val ALTERNATE = 200
    }
}
