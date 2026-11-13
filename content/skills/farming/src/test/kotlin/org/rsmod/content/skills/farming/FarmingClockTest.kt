package org.rsmod.content.skills.farming

import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.junit.jupiter.api.parallel.ResourceLock
import org.rsmod.api.player.vars.VarPlayerIntMapSetter
import org.rsmod.game.entity.Player

@Execution(ExecutionMode.SAME_THREAD)
@ResourceLock("ServerCacheManager")
class FarmingClockTest {
    @AfterEach
    fun resetDefault() { FarmingClock.defaultRate = 1 }

    @Test
    fun `normal rate preserves existing cycle alignment including fractional minutes`() {
        val player = player()
        assertEquals(NOW + 123, FarmingClock.millis(player, NOW + 123))
        assertEquals(NOW + 60_001, FarmingClock.millis(player, NOW + 60_001))
        assertEquals(2000L, FarmingClock.cycle(player, 5, NOW + 60_001))
    }

    @Test
    fun `five times growth crosses a five minute boundary after one real minute`() {
        FarmingClock.defaultRate = 5
        val player = player()
        assertEquals(2000L, FarmingClock.cycle(player, 5, NOW))
        assertEquals(100, FarmingClock.ticksUntilNext(player, 5, NOW))
        assertEquals(2000L, FarmingClock.cycle(player, 5, NOW + 59_999))
        assertEquals(1, FarmingClock.ticksUntilNext(player, 5, NOW + 59_999))
        assertEquals(2001L, FarmingClock.cycle(player, 5, NOW + 60_000))
    }

    @Test
    fun `player rate overrides default and reset retains accumulated growth`() {
        FarmingClock.defaultRate = 3
        val player = player()
        FarmingClock.setRate(player, 5, NOW)
        assertEquals(5, FarmingClock.rate(player))
        assertEquals(NOW + 300_000, FarmingClock.millis(player, NOW + 60_000))
        FarmingClock.setRate(player, 0, NOW + 60_000)
        assertEquals(3, FarmingClock.rate(player))
        assertEquals(NOW + 480_000, FarmingClock.millis(player, NOW + 120_000))
    }

    @Test
    fun `changing rate retains subminute progress without rescaling previous time`() {
        val player = player()
        assertEquals(NOW + 123, FarmingClock.millis(player, NOW + 123))
        FarmingClock.setRate(player, 5, NOW + 20_124)
        assertEquals(NOW + 20_124, FarmingClock.millis(player, NOW + 20_124))
        assertEquals(NOW + 25_129, FarmingClock.millis(player, NOW + 21_125))
        FarmingClock.setRate(player, 1, NOW + 21_125)
        assertEquals(NOW + 26_130, FarmingClock.millis(player, NOW + 22_126))
    }

    @Test
    fun `changed server default settles unseen elapsed time at the recorded old rate`() {
        val player = player()
        FarmingClock.millis(player, NOW)
        FarmingClock.defaultRate = 5
        assertEquals(NOW + 60_000, FarmingClock.millis(player, NOW + 60_000))
        assertEquals(NOW + 360_000, FarmingClock.millis(player, NOW + 120_000))
    }

    @Test
    fun `restored permanent vars retain rate progress and offline catchup`() {
        val original = player()
        FarmingClock.setRate(original, 5, NOW + 123)
        FarmingClock.millis(original, NOW + 1123)
        val restored = player().apply { vars.backing.putAll(original.vars.backing) }
        assertEquals(5, restored.farmingGrowthRate)
        assertEquals(NOW + 305_123, FarmingClock.millis(restored, NOW + 61_123))
        assertEquals(NOW + 305_123, FarmingClock.millis(restored, NOW + 61_123))
    }

    @Test
    fun `advance applies growth minutes once and future time continues at selected rate`() {
        val player = player()
        FarmingClock.setRate(player, 5, NOW)
        FarmingClock.advance(player, 10, NOW)
        assertEquals(NOW + 600_000, FarmingClock.millis(player, NOW))
        assertEquals(NOW + 900_000, FarmingClock.millis(player, NOW + 60_000))
    }

    @Test
    fun `backward wall clock cannot reverse or double count growth`() {
        val player = player()
        FarmingClock.millis(player, NOW)
        assertEquals(NOW, FarmingClock.millis(player, NOW - 1000))
        assertEquals(NOW + 1000, FarmingClock.millis(player, NOW + 1000))
    }

    @Test
    fun `invalid rates and advances leave player growth unchanged`() {
        val player = player()
        FarmingClock.millis(player, NOW)
        assertThrows(IllegalArgumentException::class.java) { FarmingClock.defaultRate = 0 }
        assertThrows(IllegalArgumentException::class.java) { FarmingClock.setRate(player, -1, NOW) }
        assertThrows(IllegalArgumentException::class.java) { FarmingClock.advance(player, 0, NOW) }
        assertThrows(ArithmeticException::class.java) { FarmingClock.advance(player, Int.MAX_VALUE, NOW) }
        assertEquals(0, player.farmingGrowthRate)
        assertEquals(NOW, FarmingClock.millis(player, NOW))
    }

    private fun player(): Player = Player().apply {
        VarPlayerIntMapSetter.set(this, "varp.farming_growth_offset", 32)
    }

    companion object {
        private const val NOW = 600_000_000L

        @JvmStatic @BeforeAll fun cache() { loadFarmingTestCache() }
    }
}
