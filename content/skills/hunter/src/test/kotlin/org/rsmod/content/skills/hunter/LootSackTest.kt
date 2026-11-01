package org.rsmod.content.skills.hunter

import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.junit.jupiter.api.parallel.ResourceLock
import org.rsmod.api.registry.obj.ObjRegistry
import org.rsmod.api.registry.zone.ZoneUpdateMap
import org.rsmod.api.repo.obj.ObjRepository
import org.rsmod.api.table.hunter.HunterLootSacksRow
import org.rsmod.game.MapClock
import org.rsmod.game.inv.InvObj
import org.rsmod.map.zone.ZoneKey

@Execution(ExecutionMode.SAME_THREAD)
@ResourceLock("ServerCacheManager")
class LootSackTest {
    private val registry = ObjRegistry(ZoneUpdateMap())
    private val script = HunterLootSackScript(ObjRepository(MapClock(100), registry), mockk(relaxed = true))
    private val sack by lazy { HunterLootSacksRow.all().first() }

    @Test
    fun `one sack opened twice rolls once`() {
        val f = HunterFixture()
        f.player.inv[0] = InvObj(sack.sack.internalName, 1)
        f.call(script, "open", sack)
        val after = f.player.inv.objs.toList()
        f.call(script, "open", sack)
        assertEquals(0L, f.total(sack.sack.internalName))
        assertEquals(after, f.player.inv.objs.toList())
        assertTrue(after.any { it != null })
    }

    @Test
    fun `a full inventory drops what does not fit`() {
        val f = HunterFixture()
        f.player.inv[0] = InvObj(sack.sack.internalName, 1)
        f.fill()
        f.call(script, "open", sack)
        assertEquals(0L, f.total(sack.sack.internalName))
        assertTrue(registry.findAll(ZoneKey.from(f.player.coords)).any())
    }

    @Test
    fun `stack overflow drops the excess`() {
        val f = HunterFixture()
        f.player.inv[0] = InvObj(sack.sack.internalName, 1)
        f.player.inv[1] = InvObj(COINS, Int.MAX_VALUE)
        f.call(script, "open", sack)
        assertEquals(0L, f.total(sack.sack.internalName))
        assertEquals(Int.MAX_VALUE.toLong(), f.total(COINS))
    }

    companion object {
        private const val COINS = "obj.coins"

        @JvmStatic
        @BeforeAll
        fun cache() = HunterFixture.cache()
    }
}
