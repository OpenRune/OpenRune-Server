package org.rsmod.content.skills.hunter

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
import org.rsmod.game.loc.LocEntity
import org.rsmod.game.loc.LocInfo
import org.rsmod.map.CoordGrid

@Execution(ExecutionMode.SAME_THREAD)
@ResourceLock("ServerCacheManager")
class BoxSnareTrapTest {
    private val f = HunterFixture()
    private val objRepo = ObjRepository(MapClock(100), ObjRegistry(ZoneUpdateMap()))
    private val script =
        HunterTrapScript(
            mockk(relaxed = true),
            mockk(relaxed = true),
            objRepo,
            mockk(relaxed = true),
            mockk(relaxed = true),
            mockk(relaxed = true),
            mockk(relaxed = true),
            f.random,
            MapClock(100),
            mockk(relaxed = true),
            mockk(relaxed = true),
            HunterTrapLimits(),
            mockk(relaxed = true),
        )
    private val traps: HashMap<CoordGrid, HunterTrapScript.Trap> by lazy { f.field(script, "traps") }
    private val kind by lazy {
        f.field<List<HunterTrapScript.TrapKind>>(script, "kinds").first { k -> !k.consumed && k.creatures.any { "chinchompa" in it.name } }
    }
    private val snare by lazy { f.field<List<HunterTrapScript.TrapKind>>(script, "kinds").first { it.snare } }
    private val chosen by lazy { kind.creatures.first { "chinchompa" in it.name } }
    private val tile = CoordGrid(3223, 3222, 0)

    private fun place(state: HunterTrapScript.TrapState) =
        HunterTrapScript.Trap(f.player, kind, tile, LocInfo(0, tile, LocEntity(0)), state, chosen, 100).also {
            traps[tile] = it
        }

    private fun lootTotal() = chosen.loot.sumOf { f.total(it.obj) }

    @Test
    fun `a full trap checked twice gives its loot once`() {
        place(HunterTrapScript.TrapState.Full)
        f.run(script, "check", tile)
        val loot = lootTotal()
        f.run(script, "check", tile)
        assertEquals(1L, f.total(kind.item))
        assertEquals(loot, lootTotal())
        assertEquals(true, loot > 0)
    }

    @Test
    fun `a full inventory keeps the trap and its catch`() {
        place(HunterTrapScript.TrapState.Full)
        f.fill()
        f.run(script, "check", tile)
        assertEquals(0L, f.total(kind.item) + lootTotal())
        assertEquals(1, traps.size)
    }

    @Test
    fun `a stack at the maximum keeps the trap`() {
        place(HunterTrapScript.TrapState.Full)
        f.player.inv[0] = InvObj(chosen.loot.first().obj, Int.MAX_VALUE)
        f.fill()
        f.run(script, "check", tile)
        assertEquals(Int.MAX_VALUE.toLong(), f.total(chosen.loot.first().obj))
        assertEquals(1, traps.size)
    }

    @Test
    fun `an inventory filled during the check keeps the trap`() {
        place(HunterTrapScript.TrapState.Full)
        f.call(script, "check", tile)
        f.fill()
        f.finish()
        assertEquals(0L, f.total(kind.item) + lootTotal())
        assertEquals(1, traps.size)
    }

    @Test
    fun `a set trap dismantled twice gives one trap`() {
        place(HunterTrapScript.TrapState.Set)
        f.run(script, "dismantle", tile)
        f.run(script, "dismantle", tile)
        assertEquals(1L, f.total(kind.item))
        assertEquals(0, traps.size)
    }

    @Test
    fun `an inventory filled during the dismantle keeps the trap`() {
        place(HunterTrapScript.TrapState.Set)
        f.call(script, "dismantle", tile)
        f.fill()
        f.finish()
        assertEquals(0L, f.total(kind.item))
        assertEquals(1, traps.size)
    }

    @Test
    fun `a full inventory keeps the set trap`() {
        place(HunterTrapScript.TrapState.Set)
        f.fill()
        f.run(script, "dismantle", tile)
        assertEquals(0L, f.total(kind.item))
        assertEquals(1, traps.size)
    }

    @Test
    fun `a trap stack at the maximum keeps the set trap`() {
        place(HunterTrapScript.TrapState.Set)
        f.player.inv[0] = InvObj(kind.item, Int.MAX_VALUE)
        f.fill()
        f.run(script, "dismantle", tile)
        assertEquals(Int.MAX_VALUE.toLong(), f.total(kind.item))
        assertEquals(1, traps.size)
    }

    private fun groundTotal() = objRepo.findAll(f.player.coords).filter { it.type == snare.itemType.id }.sumOf { it.count.toLong() }

    private fun layer() {
        f.player.statMap.setBaseLevel("stat.hunter", 99)
        f.player.statMap.setCurrentLevel("stat.hunter", 99)
        f.player.inv[0] = InvObj(snare.item, 1)
        f.player.inv[1] = InvObj(snare.item, 1)
    }

    @Test
    fun `a trap laid twice on one tile is laid once`() {
        layer()
        f.run(script, "lay", snare, null)
        f.run(script, "lay", snare, null)
        assertEquals(1L, f.total(snare.item))
        assertEquals(0L, groundTotal())
        assertEquals(1, traps.size)
    }

    @Test
    fun `a laid trap taken from the ground is not set`() {
        layer()
        f.call(script, "lay", snare, null)
        val laid = objRepo.findAll(f.player.coords).single { it.type == snare.itemType.id }
        objRepo.del(laid, Int.MAX_VALUE)
        f.finish()
        assertEquals(1L, f.total(snare.item))
        assertEquals(0L, groundTotal())
        assertEquals(0, traps.size)
    }

    companion object {
        @JvmStatic
        @BeforeAll
        fun cache() = HunterFixture.cache()
    }
}
