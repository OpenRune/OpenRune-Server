package org.rsmod.content.skills.hunter

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.junit.jupiter.api.parallel.ResourceLock
import org.rsmod.api.registry.obj.ObjRegistry
import org.rsmod.api.registry.zone.ZoneUpdateMap
import org.rsmod.api.repo.obj.ObjRepository
import org.rsmod.game.MapClock
import org.rsmod.game.inv.InvObj

@Execution(ExecutionMode.SAME_THREAD)
@ResourceLock("ServerCacheManager")
class GuildTradeTest {
    private val registry = ObjRegistry(ZoneUpdateMap())
    private val script = HunterGuildScript(mockk(relaxed = true), mockk(relaxed = true), ObjRepository(MapClock(100), registry))

    @Test
    fun `repeated buys take the price each time until the coins run out`() {
        val f = HunterFixture()
        f.player.inv[0] = InvObj(COINS, 10)
        repeat(5) { f.call(script, "buy", TIPPLE, 3) }
        assertEquals(3L, f.total(TIPPLE))
        assertEquals(1L, f.total(COINS))
    }

    @Test
    fun `a full inventory buys only when the coins free their slot`() {
        val f = HunterFixture()
        f.player.inv[0] = InvObj(COINS, 4)
        f.fill()
        f.call(script, "buy", TIPPLE, 3)
        assertEquals(0L, f.total(TIPPLE))
        assertEquals(4L, f.total(COINS))
        f.player.inv[0] = InvObj(COINS, 3)
        f.call(script, "buy", TIPPLE, 3)
        assertEquals(1L, f.total(TIPPLE))
        assertEquals(0L, f.total(COINS))
    }

    @Test
    fun `an outfit piece traded twice gives one sack`() {
        val f = HunterFixture()
        val piece = HunterLootSackScript.TRADEABLE.first()
        val type = checkNotNull(ServerCacheManager.getItem(piece.asRSCM()))
        f.player.inv[0] = InvObj(piece, 1)
        f.run(script, "trade", type)
        f.run(script, "trade", type)
        assertEquals(0L, f.total(piece))
        assertEquals(1L, f.total(HunterLootSackScript.SUPPLIES))
    }

    @Test
    fun `a stack at the maximum drops the sack to the ground`() {
        val f = HunterFixture()
        val piece = HunterLootSackScript.TRADEABLE.first()
        val type = checkNotNull(ServerCacheManager.getItem(piece.asRSCM()))
        f.player.inv[0] = InvObj(piece, 1)
        f.player.inv[1] = InvObj(HunterLootSackScript.SUPPLIES, Int.MAX_VALUE)
        f.fill()
        f.run(script, "trade", type)
        assertEquals(0L, f.total(piece))
        assertEquals(Int.MAX_VALUE.toLong(), f.total(HunterLootSackScript.SUPPLIES))
        assertEquals(1, registry.findAll(f.player.coords).sumOf { it.count })
    }

    companion object {
        private const val COINS = "obj.coins"
        private const val TIPPLE = "obj.trappers_tipple"

        @JvmStatic
        @BeforeAll
        fun cache() = HunterFixture.cache()
    }
}
