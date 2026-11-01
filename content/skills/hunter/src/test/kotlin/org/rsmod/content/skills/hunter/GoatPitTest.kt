package org.rsmod.content.skills.hunter

import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.junit.jupiter.api.parallel.ResourceLock
import org.rsmod.api.player.vars.VarPlayerIntMapSetter

@Execution(ExecutionMode.SAME_THREAD)
@ResourceLock("ServerCacheManager")
class GoatPitTest {
    private val f = HunterFixture()
    private val script =
        GoatPitScript(
            mockk(relaxed = true),
            mockk(relaxed = true),
            mockk(relaxed = true),
            mockk(relaxed = true),
            mockk(relaxed = true),
            mockk(relaxed = true),
            mockk(relaxed = true),
            mockk(relaxed = true),
            mockk(relaxed = true),
        )

    private fun goats(count: Int) {
        f.player.statMap.setBaseLevel("stat.hunter", 99)
        f.player.statMap.setCurrentLevel("stat.hunter", 99)
        VarPlayerIntMapSetter.set(f.player, GoatPitScript.IN_PIT, count)
    }

    private fun loot() = f.total(GoatPitScript.HORN) + f.total(GoatPitScript.FUR)

    @Test
    fun `cleared goats give one item each, cleared twice`() {
        goats(3)
        f.run(script, "clear")
        f.run(script, "clear")
        assertEquals(3L, loot())
        assertEquals(0, f.player.vars[GoatPitScript.IN_PIT])
    }

    @Test
    fun `a full inventory leaves the goats in the pit`() {
        goats(3)
        f.fill()
        f.run(script, "clear")
        assertEquals(0L, loot())
        assertEquals(3, f.player.vars[GoatPitScript.IN_PIT])
    }

    @Test
    fun `an inventory filled during the clear keeps the goat`() {
        goats(3)
        f.call(script, "clear")
        f.fill()
        f.finish()
        assertEquals(0L, loot())
        assertEquals(3, f.player.vars[GoatPitScript.IN_PIT])
    }

    companion object {
        @JvmStatic
        @BeforeAll
        fun cache() = HunterFixture.cache()
    }
}
