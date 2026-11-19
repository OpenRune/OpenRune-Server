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

@Execution(ExecutionMode.SAME_THREAD)
@ResourceLock("ServerCacheManager")
class MossLizardTest {
    private val f = HunterFixture()
    private val script =
        MossLizardScript(
            mockk(relaxed = true),
            ObjRepository(MapClock(100), ObjRegistry(ZoneUpdateMap())),
            mockk(relaxed = true),
            mockk(relaxed = true),
            mockk(relaxed = true),
            MapClock(100),
        )

    @Test
    fun `one lizard cut twice gives one tail`() {
        f.player.inv[0] = InvObj(MossLizardScript.LIZARD, 1)
        f.fill()
        f.call(script, "cutTail")
        f.call(script, "cutTail")
        assertEquals(0L, f.total(MossLizardScript.LIZARD))
        assertEquals(1L, f.total(MossLizardScript.TAIL))
    }

    @Test
    fun `a full inventory cuts the tail in the lizard's slot`() {
        f.fill()
        f.player.inv[5] = InvObj(MossLizardScript.LIZARD, 1)
        f.call(script, "cutTail")
        assertEquals(0L, f.total(MossLizardScript.LIZARD))
        assertEquals(1L, f.total(MossLizardScript.TAIL))
        assertEquals(27L, f.total("obj.bronze_dagger"))
    }

    @Test
    fun `a lizard gone before the cut gives no tail`() {
        f.player.inv[0] = InvObj(MossLizardScript.KNIFE, 1)
        f.call(script, "cutTail")
        assertEquals(0L, f.total(MossLizardScript.TAIL))
        assertEquals(1L, f.total(MossLizardScript.KNIFE))
    }

    companion object {
        @JvmStatic
        @BeforeAll
        fun cache() = HunterFixture.cache()
    }
}
