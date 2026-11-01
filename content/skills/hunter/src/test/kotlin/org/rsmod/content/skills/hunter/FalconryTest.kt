package org.rsmod.content.skills.hunter

import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.junit.jupiter.api.parallel.ResourceLock
import org.rsmod.game.MapClock
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player

@Execution(ExecutionMode.SAME_THREAD)
@ResourceLock("ServerCacheManager")
class FalconryTest {
    private val f = HunterFixture()
    private val script =
        FalconryScript(mockk(relaxed = true), mockk(relaxed = true), mockk(relaxed = true), f.random, MapClock(100), mockk(relaxed = true))
    private val flights: HashMap<Player, FalconryScript.Flight> by lazy { f.field(script, "flights") }
    private val kebbit by lazy { f.field<List<FalconryScript.Kebbit>>(script, "kebbits").first() }
    private val falcon = mockk<Npc>(relaxed = true)

    private fun fly() {
        flights[f.player] = FalconryScript.Flight(kebbit, mockk(relaxed = true), true, 100, 100).also { it.falcon = falcon }
    }

    private fun loot() = kebbit.loot.distinct().sumOf { f.total(it) }

    @Test
    fun `a falcon retrieved twice gives its catch once`() {
        fly()
        f.call(script, "retrieve", falcon)
        f.call(script, "retrieve", falcon)
        assertEquals(kebbit.loot.size.toLong(), loot())
        assertTrue(flights.isEmpty())
    }

    @Test
    fun `a full inventory keeps the falcon on its catch`() {
        fly()
        f.fill()
        f.call(script, "retrieve", falcon)
        assertEquals(0L, loot())
        assertEquals(1, flights.size)
    }

    companion object {
        @JvmStatic
        @BeforeAll
        fun cache() = HunterFixture.cache()
    }
}
