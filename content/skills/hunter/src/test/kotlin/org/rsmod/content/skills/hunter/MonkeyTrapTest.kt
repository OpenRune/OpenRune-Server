package org.rsmod.content.skills.hunter

import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.junit.jupiter.api.parallel.ResourceLock
import org.rsmod.game.MapClock
import org.rsmod.game.entity.Player
import org.rsmod.game.inv.InvObj
import org.rsmod.game.loc.BoundLocInfo
import org.rsmod.game.loc.LocEntity
import org.rsmod.game.loc.LocInfo
import org.rsmod.map.CoordGrid

@Execution(ExecutionMode.SAME_THREAD)
@ResourceLock("ServerCacheManager")
class MonkeyTrapTest {
    private val f = HunterFixture()
    private val script =
        MonkeyTrapScript(
            mockk(relaxed = true),
            mockk(relaxed = true),
            mockk(relaxed = true),
            mockk(relaxed = true),
            f.random,
            MapClock(100),
            mockk(relaxed = true),
        )
    private val traps: HashMap<CoordGrid, MonkeyTrapScript.MonkeyTrap> by lazy { f.field(script, "traps") }
    private val boulder = LocInfo(0, CoordGrid(3223, 3222, 0), LocEntity(0))

    private fun place() =
        MonkeyTrapScript.MonkeyTrap(f.player, boulder, 1, 1).also {
            it.state = MonkeyTrapScript.State.Full
            traps[boulder.coords] = it
        }

    private fun tails() = f.total(MonkeyTrapScript.PERFECT) + f.total(MonkeyTrapScript.DAMAGED)

    @Test
    fun `a full trap checked twice gives one tail`() {
        val trap = place()
        f.run(script, "check", trap)
        f.run(script, "check", trap)
        assertEquals(1L, tails())
        assertEquals(0, traps.size)
    }

    @Test
    fun `a full inventory keeps the monkey`() {
        val trap = place()
        f.fill()
        f.run(script, "check", trap)
        assertEquals(0L, tails())
        assertEquals(1, traps.size)
    }

    @Test
    fun `an inventory filled during the check keeps the monkey`() {
        val trap = place()
        f.call(script, "check", trap)
        f.fill()
        f.finish()
        assertEquals(0L, tails())
        assertEquals(1, traps.size)
    }

    @Test
    fun `a banana eaten while setting lays no trap, and a second set takes no second banana`() {
        val bound = BoundLocInfo(boulder.coords, boulder.entity, boulder.layer, 1, 1, 0)
        f.player.statMap.setBaseLevel("stat.hunter", 99)
        f.player.statMap.setCurrentLevel("stat.hunter", 99)
        f.field<HashSet<Player>>(script, "mounted").add(f.player)
        f.player.inv[0] = InvObj(MonkeyTrapScript.BANANA, 1)
        f.call(script, "set", bound)
        f.player.inv[0] = null
        f.finish()
        assertEquals(0, traps.size)
        f.player.inv[0] = InvObj(MonkeyTrapScript.BANANA, 1)
        f.player.inv[1] = InvObj(MonkeyTrapScript.BANANA, 1)
        f.run(script, "set", bound)
        f.run(script, "set", bound)
        assertEquals(1, traps.size)
        assertEquals(1L, f.total(MonkeyTrapScript.BANANA))
    }

    companion object {
        @JvmStatic
        @BeforeAll
        fun cache() = HunterFixture.cache()
    }
}
