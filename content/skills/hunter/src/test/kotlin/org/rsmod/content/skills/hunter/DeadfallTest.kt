package org.rsmod.content.skills.hunter

import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.junit.jupiter.api.parallel.ResourceLock
import org.rsmod.game.MapClock
import org.rsmod.game.loc.LocEntity
import org.rsmod.game.loc.LocInfo
import org.rsmod.map.CoordGrid

@Execution(ExecutionMode.SAME_THREAD)
@ResourceLock("ServerCacheManager")
class DeadfallTest {
    private val f = HunterFixture()
    private val script =
        DeadfallScript(
            mockk(relaxed = true),
            mockk(relaxed = true),
            mockk(relaxed = true),
            mockk(relaxed = true),
            mockk(relaxed = true),
            f.random,
            MapClock(100),
            mockk(relaxed = true),
        )
    private val traps: HashMap<CoordGrid, DeadfallScript.Deadfall> by lazy { f.field(script, "traps") }
    private val creature by lazy { f.field<List<DeadfallScript.Creature>>(script, "creatures").first() }
    private val boulder = LocInfo(0, CoordGrid(3223, 3222, 0), LocEntity(0))

    private fun place() =
        DeadfallScript.Deadfall(f.player, boulder, 1, 1).also {
            it.state = DeadfallScript.State.Full
            it.caught = creature
            traps[boulder.coords] = it
        }

    private fun loot() = creature.loot.sumOf { f.total(it) }

    @Test
    fun `a full deadfall checked twice gives its loot once`() {
        val trap = place()
        f.run(script, "check", trap)
        f.run(script, "check", trap)
        assertEquals(creature.loot.size.toLong(), loot())
        assertEquals(0, traps.size)
    }

    @Test
    fun `a full inventory keeps the catch`() {
        val trap = place()
        f.fill()
        f.run(script, "check", trap)
        assertEquals(0L, loot())
        assertEquals(1, traps.size)
    }

    @Test
    fun `an inventory filled during the check keeps the catch`() {
        val trap = place()
        f.call(script, "check", trap)
        f.fill()
        f.finish()
        assertEquals(0L, loot())
        assertEquals(1, traps.size)
    }

    companion object {
        @JvmStatic
        @BeforeAll
        fun cache() = HunterFixture.cache()
    }
}
