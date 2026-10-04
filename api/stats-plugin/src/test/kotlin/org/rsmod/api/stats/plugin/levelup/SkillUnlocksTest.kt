package org.rsmod.api.stats.plugin.levelup

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class SkillUnlocksTest {
    @Test
    fun `reads every stat and level tuple of a skill column`() {
        val unlocks =
            SkillUnlocks(SkillUnlocks.fromTuples(arrayOf(8, 15, 0, 9, 20, 1), tupleSize = 3))
        assertTrue(unlocks.contains(stat = 8, level = 15))
        assertTrue(unlocks.contains(stat = 9, level = 20))
        assertFalse(unlocks.contains(stat = 8, level = 20))
        assertFalse(unlocks.contains(stat = 15, level = 8))
    }

    @Test
    fun `malformed columns are ignored`() {
        assertTrue(SkillUnlocks.fromTuples(arrayOf(8), tupleSize = 1).isEmpty())
        assertTrue(SkillUnlocks.fromTuples(arrayOf(8, 15, 0, 9), tupleSize = 3).size == 1)
    }
}
