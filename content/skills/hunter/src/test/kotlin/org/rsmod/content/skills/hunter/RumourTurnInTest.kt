package org.rsmod.content.skills.hunter

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.junit.jupiter.api.parallel.ResourceLock
import org.rsmod.api.player.vars.VarPlayerIntMapSetter
import org.rsmod.api.registry.obj.ObjRegistry
import org.rsmod.api.registry.zone.ZoneUpdateMap
import org.rsmod.api.repo.obj.ObjRepository
import org.rsmod.game.MapClock
import org.rsmod.game.inv.InvObj

@Execution(ExecutionMode.SAME_THREAD)
@ResourceLock("ServerCacheManager")
class RumourTurnInTest {
    private val registry = ObjRegistry(ZoneUpdateMap())
    private val f = HunterFixture()
    private val rumours = HunterRumours(ObjRepository(MapClock(100), registry), f.random)
    private val hunter by lazy { rumours.hunters.first() }
    private val rumour by lazy { rumours.rumours.first { it.hunters and (1 shl hunter.index) != 0 } }

    private fun assign() = VarPlayerIntMapSetter.set(f.player, rumours.hunterVarp(hunter), rumour.index)

    @Test
    fun `a part turned in twice gives one sack`() {
        assign()
        f.player.inv[0] = InvObj(rumour.part.internalName, 1)
        assertTrue(rumours.turnIn(f.player, hunter))
        assertFalse(rumours.turnIn(f.player, hunter))
        assertEquals(0L, f.total(rumour.part.internalName))
        assertEquals(1L, f.total(hunter.lootSack.internalName))
    }

    @Test
    fun `a full inventory takes the sack in the part's slot`() {
        assign()
        f.player.inv[0] = InvObj(rumour.part.internalName, 1)
        f.fill()
        assertTrue(rumours.turnIn(f.player, hunter))
        assertEquals(0L, f.total(rumour.part.internalName))
        assertEquals(1L, f.total(hunter.lootSack.internalName))
    }

    private fun hunting() {
        assign()
        rumours.resume(f.player, hunter)
        VarPlayerIntMapSetter.set(f.player, HunterRumours.PITY, rumour.pity)
    }

    private fun parts() = f.total(rumour.part.internalName) + registry.findAll(f.player.coords).sumOf { it.count }

    @Test
    fun `a rumour creature caught twice gives one part`() {
        hunting()
        rumours.onCatch(f.player, rumour.creature)
        rumours.onCatch(f.player, rumour.creature)
        assertEquals(1L, parts())
    }

    @Test
    fun `a full inventory keeps the pity part for the next catch`() {
        hunting()
        f.fill()
        rumours.onCatch(f.player, rumour.creature)
        assertEquals(0L, parts())
        f.player.inv[0] = null
        rumours.onCatch(f.player, rumour.creature)
        assertEquals(1L, f.total(rumour.part.internalName))
    }

    @Test
    fun `no part gives nothing`() {
        assign()
        assertFalse(rumours.turnIn(f.player, hunter))
        assertEquals(0L, f.total(hunter.lootSack.internalName))
    }

    companion object {
        @JvmStatic
        @BeforeAll
        fun cache() = HunterFixture.cache()
    }
}
