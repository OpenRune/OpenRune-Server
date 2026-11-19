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
class NetTrapTest {
    private val f = HunterFixture()
    private val registry = ObjRegistry(ZoneUpdateMap())
    private val script =
        NetTrapScript(
            mockk(relaxed = true),
            mockk(relaxed = true),
            ObjRepository(MapClock(100), registry),
            mockk(relaxed = true),
            mockk(relaxed = true),
            mockk(relaxed = true),
            f.random,
            MapClock(100),
            mockk(relaxed = true),
            HunterTrapLimits(),
        )
    private val traps: HashMap<CoordGrid, NetTrapScript.NetTrap> by lazy { f.field(script, "traps") }
    private val tree by lazy { f.field<List<NetTrapScript.Tree>>(script, "trees").first() }
    private val loc = LocInfo(0, CoordGrid(3223, 3222, 0), LocEntity(0))

    private fun place(state: NetTrapScript.State) =
        NetTrapScript.NetTrap(f.player, tree, loc, CoordGrid(3224, 3222, 0)).also {
            it.state = state
            traps[loc.coords] = it
        }

    private fun ground() = registry.findAll(f.player.coords).sumOf { it.count }

    @Test
    fun `a full net checked twice returns one net trap and one catch`() {
        val trap = place(NetTrapScript.State.Full)
        f.run(script, "check", trap)
        f.run(script, "check", trap)
        assertEquals(1L, f.total(NetTrapScript.NET_TRAP))
        assertEquals(26, f.player.inv.freeSpace())
        assertEquals(0, traps.size)
    }

    @Test
    fun `an inventory filled during the check keeps the net`() {
        val trap = place(NetTrapScript.State.Full)
        f.call(script, "check", trap)
        f.fill()
        f.finish()
        assertEquals(0L, f.total(NetTrapScript.NET_TRAP))
        assertEquals(1, traps.size)
    }

    @Test
    fun `an inventory filled during the dismantle keeps the net`() {
        val trap = place(NetTrapScript.State.Set)
        f.call(script, "dismantle", trap)
        f.fill()
        f.finish()
        assertEquals(0L, f.total(NetTrapScript.NET_TRAP))
        assertEquals(1, traps.size)
        f.player.inv[0] = null
        f.run(script, "dismantle", trap)
        f.run(script, "dismantle", trap)
        assertEquals(1L, f.total(NetTrapScript.NET_TRAP))
    }

    @Test
    fun `a full inventory keeps the catch`() {
        val trap = place(NetTrapScript.State.Full)
        f.fill()
        f.run(script, "check", trap)
        assertEquals(0L, f.total(NetTrapScript.NET_TRAP))
        assertEquals(1, traps.size)
    }

    @Test
    fun `a net trap stack at max keeps the net and the catch`() {
        f.player.inv[0] = InvObj(NetTrapScript.NET_TRAP, Int.MAX_VALUE)
        val full = place(NetTrapScript.State.Full)
        f.run(script, "check", full)
        assertEquals(1, traps.size)
        traps.clear()
        val set = place(NetTrapScript.State.Set)
        f.run(script, "dismantle", set)
        assertEquals(1, traps.size)
        assertEquals(Int.MAX_VALUE.toLong(), f.total(NetTrapScript.NET_TRAP))
        assertEquals(27, f.player.inv.freeSpace())
        assertEquals(0, ground())
    }

    @Test
    fun `a net trap dropped while setting lays no trap`() {
        f.player.statMap.setBaseLevel("stat.hunter", 99)
        f.player.statMap.setCurrentLevel("stat.hunter", 99)
        f.player.inv[0] = InvObj(NetTrapScript.NET_TRAP, 1)
        f.call(script, "set", tree, loc)
        f.player.inv[0] = null
        f.finish()
        assertEquals(0, traps.size)
        assertEquals(0L, f.total(NetTrapScript.NET_TRAP))
    }

    @Test
    fun `a salamander released twice from one slot removes only it`() {
        val salamander = tree.loot.first().first
        f.player.inv[0] = InvObj(salamander, 1)
        f.player.inv[1] = InvObj(salamander, 1)
        f.call(script, "release", salamander, 0)
        f.call(script, "release", salamander, 0)
        assertEquals(1L, f.total(salamander))
        assertEquals(null, f.player.inv[0])
        assertEquals(1, f.player.inv[1]?.count)
    }

    @Test
    fun `making a net trap onto a full stack keeps the rope and net`() {
        f.player.statMap.setBaseLevel("stat.hunter", 99)
        f.player.statMap.setCurrentLevel("stat.hunter", 99)
        f.player.inv[0] = InvObj(NetTrapScript.ROPE, 1)
        f.player.inv[1] = InvObj(NetTrapScript.SMALL_NET, 1)
        f.player.inv[2] = InvObj(NetTrapScript.NET_TRAP, Int.MAX_VALUE)
        f.call(script, "makeNetTrap")
        assertEquals(2L, f.total(NetTrapScript.ROPE) + f.total(NetTrapScript.SMALL_NET))
        assertEquals(Int.MAX_VALUE.toLong(), f.total(NetTrapScript.NET_TRAP))
        assertEquals(0, ground())
        f.player.inv[2] = null
        f.call(script, "makeNetTrap")
        f.call(script, "makeNetTrap")
        assertEquals(0L, f.total(NetTrapScript.ROPE) + f.total(NetTrapScript.SMALL_NET))
        assertEquals(1L, f.total(NetTrapScript.NET_TRAP))
    }

    companion object {
        @JvmStatic
        @BeforeAll
        fun cache() = HunterFixture.cache()
    }
}
