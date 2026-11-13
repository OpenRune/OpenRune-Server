package org.rsmod.content.skills.hunter

import dev.openrune.util.Wearpos
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
import org.rsmod.game.inv.InvObj

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

    private fun gloves() = FalconryScript.GLOVES.sumOf { f.total(it) }

    @Test
    fun `a falcon retrieved twice gives its catch once`() {
        fly()
        f.call(script, "retrieve", falcon)
        f.call(script, "retrieve", falcon)
        assertEquals(kebbit.loot.size.toLong(), loot())
        assertTrue(flights.isEmpty())
    }

    @Test
    fun `a full inventory discards the catch and ends the flight`() {
        fly()
        f.fill()
        f.call(script, "retrieve", falcon)
        assertEquals(0L, loot())
        assertEquals(28L, f.total("obj.bronze_dagger"))
        assertTrue(flights.isEmpty())
    }

    @Test
    fun `investing twice takes the coins once`() {
        f.player.inv[0] = InvObj(FalconryScript.COINS, FalconryScript.INVESTMENT * 2)
        f.call(script, "payInvestment")
        f.call(script, "payInvestment")
        assertEquals(FalconryScript.INVESTMENT.toLong(), f.total(FalconryScript.COINS))
        assertEquals(1, f.player.vars[FalconryScript.INVESTED])
    }

    @Test
    fun `coins gone before the investment is paid unlock nothing`() {
        f.player.inv[0] = InvObj(FalconryScript.COINS, FalconryScript.INVESTMENT - 1)
        f.call(script, "payInvestment")
        assertEquals(FalconryScript.INVESTMENT - 1L, f.total(FalconryScript.COINS))
        assertEquals(0, f.player.vars[FalconryScript.INVESTED])
    }

    @Test
    fun `a falcon hired twice takes one fee and gives one glove`() {
        f.player.inv[0] = InvObj(FalconryScript.COINS, FalconryScript.FEE * 2)
        f.call(script, "takeFalcon", FalconryScript.FEE)
        f.call(script, "takeFalcon", FalconryScript.FEE)
        assertEquals(FalconryScript.FEE.toLong(), f.total(FalconryScript.COINS))
        assertEquals(1L, gloves())
    }

    @Test
    fun `full hands and a full inventory hire nothing`() {
        f.player.inv[0] = InvObj(FalconryScript.COINS, FalconryScript.FEE)
        f.player.worn[Wearpos.RightHand.slot] = InvObj("obj.bronze_dagger", 1)
        f.fill()
        f.call(script, "takeFalcon", FalconryScript.FEE)
        assertEquals(FalconryScript.FEE.toLong(), f.total(FalconryScript.COINS))
        assertEquals(28L, f.total("obj.bronze_dagger"))
        assertEquals(0L, gloves())
    }

    @Test
    fun `coins gone before the fee is paid give no glove`() {
        f.call(script, "takeFalcon", FalconryScript.FEE)
        assertEquals(0L, f.total(FalconryScript.COINS))
        assertEquals(0L, gloves())
    }

    companion object {
        @JvmStatic
        @BeforeAll
        fun cache() = HunterFixture.cache()
    }
}
