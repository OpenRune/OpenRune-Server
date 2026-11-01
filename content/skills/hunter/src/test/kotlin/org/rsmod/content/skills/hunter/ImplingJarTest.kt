package org.rsmod.content.skills.hunter

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
class ImplingJarTest {
    private val registry = ObjRegistry(ZoneUpdateMap())
    private val script = ImplingLootScript(ObjRepository(MapClock(100), registry))

    @Test
    fun `one jar looted twice gives loot once`() {
        val f = HunterFixture()
        f.player.inv[0] = InvObj(JAR, 1)
        f.run(script, "loot", 0, JAR)
        val after = f.player.inv.objs.toList()
        f.run(script, "loot", 0, JAR)
        assertEquals(0L, f.total(JAR))
        assertEquals(after, f.player.inv.objs.toList())
    }

    @Test
    fun `a full inventory keeps the jar`() {
        val f = HunterFixture()
        f.player.inv[0] = InvObj(JAR, 1)
        f.fill()
        f.run(script, "loot", 0, JAR)
        assertEquals(1L, f.total(JAR))
    }

    @Test
    fun `stack overflow drops the loot instead of losing it`() {
        val f = HunterFixture()
        f.player.inv[0] = InvObj(COINS, Int.MAX_VALUE)
        f.call(script, "give", listOf(ImplingDrop(COINS, 5, 5, false, 1, 1, null)))
        assertEquals(Int.MAX_VALUE, f.player.inv.count(COINS))
        assertEquals(5, registry.findAll(f.player.coords).sumOf { it.count })
    }

    companion object {
        private const val JAR = "obj.ii_captured_impling_1"
        private const val COINS = "obj.coins"

        @JvmStatic
        @BeforeAll
        fun cache() = HunterFixture.cache()
    }
}
