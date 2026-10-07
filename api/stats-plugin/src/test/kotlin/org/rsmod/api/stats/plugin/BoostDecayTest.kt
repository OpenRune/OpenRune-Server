package org.rsmod.api.stats.plugin

import dev.openrune.ServerCacheManager
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.junit.jupiter.api.parallel.ResourceLock
import org.rsmod.api.player.stat.StatBoostDecayPrevention
import org.rsmod.api.player.stat.stat
import org.rsmod.game.entity.Player

@Execution(ExecutionMode.SAME_THREAD)
@ResourceLock("ServerCacheManager")
class BoostDecayTest {
    @Test
    fun `boosted hitpoints decay one level per cycle`() {
        val player = player(HITPOINTS to 115)
        player.decayBoostedStats()
        assertEquals(114, player.stat(HITPOINTS))
    }

    @Test
    fun `boosted prayer points do not decay`() {
        val player = player(PRAYER to 103)
        player.decayBoostedStats()
        assertEquals(103, player.stat(PRAYER))
    }

    @Test
    fun `other boosts still decay and drained or base levels are left alone`() {
        val player = player(ATTACK to 118, STRENGTH to 88, DEFENCE to 99, HITPOINTS to 90)
        player.decayBoostedStats()
        assertEquals(117, player.stat(ATTACK))
        assertEquals(88, player.stat(STRENGTH))
        assertEquals(99, player.stat(DEFENCE))
        assertEquals(90, player.stat(HITPOINTS))
    }

    @Test
    fun `decay prevention still holds a boosted stat`() {
        val player = player(ATTACK to 118)
        StatBoostDecayPrevention.add(player, ATTACK, "test")
        player.decayBoostedStats()
        assertEquals(118, player.stat(ATTACK))
    }

    @Test
    fun `natural regeneration still skips hitpoints and prayer`() {
        val names = statNames()
        assertTrue(HITPOINTS in names && PRAYER in names)
        assertFalse(StatDecayRules.regenerates(HITPOINTS))
        assertFalse(StatDecayRules.regenerates(PRAYER))
        assertTrue(StatDecayRules.regenerates(ATTACK))
        assertTrue(StatDecayRules.boostDecays(HITPOINTS))
        assertFalse(StatDecayRules.boostDecays(PRAYER))
    }

    private fun player(vararg current: Pair<String, Int>): Player =
        Player().apply {
            statNames().forEach { stat ->
                statMap.setBaseLevel(stat, 99.toByte())
                statMap.setCurrentLevel(stat, 99.toByte())
            }
            current.forEach { (stat, level) -> statMap.setCurrentLevel(stat, level.toByte()) }
        }

    private companion object {
        const val HITPOINTS = "stat.hitpoints"
        const val PRAYER = "stat.prayer"
        const val ATTACK = "stat.attack"
        const val STRENGTH = "stat.strength"
        const val DEFENCE = "stat.defence"

        @JvmStatic
        @BeforeAll
        fun cache() {
            ServerCacheManager.init(240).close()
        }
    }
}
