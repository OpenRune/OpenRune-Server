package org.rsmod.content.other.special.attacks.boost

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class DragonBattleaxeDrainTest {
    @Test
    fun `drains 10 percent of the current level, rounded down`() {
        assertEquals(9, DragonBattleaxeDrain.drainAmount(99))
        assertEquals(0, DragonBattleaxeDrain.drainAmount(9))
    }

    @Test
    fun `strength boost is 10 plus a quarter of the total drained`() {
        // At 99 in all four drained stats: 4 * 9 = 36 total drained, boost = 10 + 36/4 = 19.
        assertEquals(19, DragonBattleaxeDrain.strengthBoost(totalDrained = 36))
    }

    @Test
    fun `strength boost floors to 10 when nothing was drained`() {
        assertEquals(10, DragonBattleaxeDrain.strengthBoost(totalDrained = 0))
    }
}
