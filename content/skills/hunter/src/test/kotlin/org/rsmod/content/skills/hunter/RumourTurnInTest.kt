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
        assertEquals(0L, parts())
        assertEquals(1L, sacks())
    }

    @Test
    fun `a full inventory takes the sack in the part's slot`() {
        assign()
        f.player.inv[0] = InvObj(rumour.part.internalName, 1)
        f.fill()
        assertTrue(rumours.turnIn(f.player, hunter))
        assertEquals(0L, parts())
        assertEquals(1L, sacks())
    }

    private fun hunting() {
        assign()
        rumours.resume(f.player, hunter)
        VarPlayerIntMapSetter.set(f.player, HunterRumours.PITY, rumour.pity)
    }

    private fun ground(obj: String) =
        registry.findAll(f.player.coords).filter { it.type == InvObj(obj, 1).id }.sumOf { it.count.toLong() }

    private fun parts() = f.total(rumour.part.internalName) + ground(rumour.part.internalName)

    private fun sacks() = f.total(hunter.lootSack.internalName) + ground(hunter.lootSack.internalName)

    @Test
    fun `a full sack stack drops the sack beside the player`() {
        assign()
        f.player.inv[0] = InvObj(rumour.part.internalName, 1)
        f.player.inv[1] = InvObj(hunter.lootSack.internalName, Int.MAX_VALUE)
        assertTrue(rumours.turnIn(f.player, hunter))
        assertEquals(0L, parts())
        assertEquals(Int.MAX_VALUE.toLong(), f.total(hunter.lootSack.internalName))
        assertEquals(1L, ground(hunter.lootSack.internalName))
    }

    @Test
    fun `a part gone before the hand-in gives nothing`() {
        assign()
        f.player.inv[0] = InvObj(rumour.part.internalName, 1)
        assertTrue(rumours.hasPart(f.player, hunter))
        f.player.inv[0] = null
        assertFalse(rumours.turnIn(f.player, hunter))
        assertEquals(0L, sacks())
        assertEquals(rumour.index, f.player.vars[rumours.hunterVarp(hunter)])
    }

    @Test
    fun `a tuft from another kebbit is not handed in`() {
        val (caught, assigned) =
            rumours.rumours.filter { it.hunters and (1 shl hunter.index) != 0 }.groupBy { it.part.id }.values.first { it.size > 1 }
        VarPlayerIntMapSetter.set(f.player, rumours.hunterVarp(hunter), caught.index)
        rumours.resume(f.player, hunter)
        VarPlayerIntMapSetter.set(f.player, HunterRumours.PITY, caught.pity)
        rumours.onCatch(f.player, caught.creature)
        assertEquals(1L, f.total(caught.part.internalName))
        VarPlayerIntMapSetter.set(f.player, rumours.hunterVarp(hunter), assigned.index)
        assertFalse(rumours.hasPart(f.player, hunter))
        assertFalse(rumours.turnIn(f.player, hunter))
        assertEquals(1L, f.total(caught.part.internalName))
        assertEquals(0L, sacks())
        VarPlayerIntMapSetter.set(f.player, rumours.hunterVarp(hunter), caught.index)
        assertTrue(rumours.turnIn(f.player, hunter))
        assertEquals(0L, f.total(caught.part.internalName))
        assertEquals(1L, sacks())
    }

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
        assertEquals(0L, sacks())
    }

    companion object {
        @JvmStatic
        @BeforeAll
        fun cache() = HunterFixture.cache()
    }
}
