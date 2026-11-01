package org.rsmod.content.skills.hunter

import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.junit.jupiter.api.parallel.ResourceLock
import org.rsmod.api.player.vars.VarPlayerIntMapSetter
import org.rsmod.game.MapClock
import org.rsmod.game.entity.Player
import org.rsmod.map.CoordGrid

@Execution(ExecutionMode.SAME_THREAD)
@ResourceLock("ServerCacheManager")
class PitfallTest {
    private val f = HunterFixture()
    private val script =
        PitfallScript(
            mockk(relaxed = true),
            mockk(relaxed = true),
            mockk(relaxed = true),
            mockk(relaxed = true),
            mockk(relaxed = true),
            f.random,
            MapClock(100),
            mockk(relaxed = true),
            mockk(relaxed = true),
            mockk(relaxed = true),
        )
    private val pit by lazy { f.field<List<PitfallScript.Pit>>(script, "pits").first { it.kinds.isNotEmpty() } }
    private val creature by lazy { pit.kinds.first() }

    private fun place() {
        VarPlayerIntMapSetter.set(f.player, pit.varbit, PitfallScript.STATE_FULL)
        val hunt = PitfallScript.Hunt()
        hunt.pits[pit.index] = PitfallScript.PitTimer(PitfallScript.PitState.Full, 100, CoordGrid(3223, 3222, 0), creature)
        f.field<HashMap<Player, PitfallScript.Hunt>>(script, "hunts")[f.player] = hunt
    }

    private fun loot() = (creature.loot + creature.furs).distinct().sumOf { f.total(it) }

    @Test
    fun `a full pit dismantled twice gives its loot once`() {
        place()
        f.run(script, "dismantle", pit)
        val once = loot()
        f.run(script, "dismantle", pit)
        assertEquals(once, loot())
        assertEquals(true, once > 0)
    }

    @Test
    fun `a full inventory keeps the catch`() {
        place()
        f.fill()
        f.run(script, "dismantle", pit)
        assertEquals(0L, loot())
        assertEquals(PitfallScript.STATE_FULL, f.player.vars[pit.varbit])
    }

    @Test
    fun `an inventory filled during the dismantle keeps the catch`() {
        place()
        f.call(script, "dismantle", pit)
        f.fill()
        f.finish()
        assertEquals(0L, loot())
        assertEquals(PitfallScript.STATE_FULL, f.player.vars[pit.varbit])
    }

    companion object {
        @JvmStatic
        @BeforeAll
        fun cache() = HunterFixture.cache()
    }
}
