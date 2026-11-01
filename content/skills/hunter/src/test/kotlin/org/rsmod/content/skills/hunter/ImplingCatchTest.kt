package org.rsmod.content.skills.hunter

import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.junit.jupiter.api.parallel.ResourceLock
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.registry.obj.ObjRegistry
import org.rsmod.api.registry.zone.ZoneUpdateMap
import org.rsmod.api.repo.obj.ObjRepository
import org.rsmod.game.MapClock
import org.rsmod.game.entity.Npc
import org.rsmod.game.inv.InvObj

@Execution(ExecutionMode.SAME_THREAD)
@ResourceLock("ServerCacheManager")
class ImplingCatchTest {
    private val registry = ObjRegistry(ZoneUpdateMap())
    private val f = HunterFixture()
    private val script =
        ImplingScript(mockk(relaxed = true), ImplingLootScript(ObjRepository(MapClock(100), registry)), mockk(relaxed = true))
    private val npc =
        mockk<Npc>(relaxed = true) {
            every { isSlotAssigned } returns true
            every { isVisible } returns true
        }
    private val always: ProtectedAccess.(String?) -> Boolean = { true }

    private fun catch() = f.call(script, "catch", npc, 1, JAR, 20, false, always, {})

    private fun hunter() {
        f.player.statMap.setBaseLevel("stat.hunter", 99)
        f.player.statMap.setCurrentLevel("stat.hunter", 99)
    }

    @Test
    fun `one impling caught twice fills one jar`() {
        hunter()
        f.player.inv[0] = InvObj(ImplingScript.IMPLING_JAR, 1)
        f.player.inv[1] = InvObj(ImplingScript.IMPLING_JAR, 1)
        catch()
        f.finish()
        catch()
        f.finish()
        assertEquals(1L, f.total(JAR))
        assertEquals(1L, f.total(ImplingScript.IMPLING_JAR))
        assertEquals(InvObj(JAR, 1).id, f.player.inv[0]?.id)
    }

    @Test
    fun `a full inventory with a jar keeps the jarred impling in the jar's slot`() {
        hunter()
        f.player.inv[0] = InvObj(ImplingScript.IMPLING_JAR, 1)
        f.fill()
        catch()
        f.finish()
        assertEquals(1L, f.total(JAR))
        assertEquals(0L, f.total(ImplingScript.IMPLING_JAR))
    }

    @Test
    fun `an inventory filled during the attempt catches nothing without a jar`() {
        hunter()
        catch()
        f.fill()
        f.finish()
        assertTrue(f.player.inv.all { it?.id == f.player.inv[0]?.id })
        assertEquals(0, registry.findAll(f.player.coords).count())
    }

    companion object {
        private const val JAR = "obj.ii_captured_impling_1"

        @JvmStatic
        @BeforeAll
        fun cache() = HunterFixture.cache()
    }
}
