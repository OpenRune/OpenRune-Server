package org.rsmod.content.generic.npcs.crabs

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import dev.openrune.types.NpcMode
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.junit.jupiter.api.parallel.ResourceLock
import org.rsmod.annotations.InternalApi
import org.rsmod.content.generic.npcs.disguise.DisguisedNpcs
import org.rsmod.map.CoordGrid

@Execution(ExecutionMode.SAME_THREAD)
@ResourceLock("ServerCacheManager")
@OptIn(InternalApi::class)
class CrabsTest {
    private val rocks = "npc.zeah_sandcrab_inactive"
    private val crab = "npc.zeah_sandcrab"
    private val spot = CoordGrid(1773, 3460, 0)

    @Test
    fun `rocks wake only when a player steps next to them`() {
        val world = CrabWorld()
        val npc = world.spawn(rocks, spot)
        val player = world.player(spot.translate(2, 0))
        world.tick(5)
        assertEquals(rocks.asRSCM(RSCMType.NPC), npc.visType.id)
        assertFalse(npc.isDelayed)

        world.move(player, spot.translate(1, 1))
        world.tick()
        assertEquals(crab.asRSCM(RSCMType.NPC), npc.visType.id)
        assertEquals(rocks.asRSCM(RSCMType.NPC), npc.type.id)
        assertEquals(0, npc.vars[DisguisedNpcs.IDLE])
        assertTrue(npc.isDelayed)
    }

    @Test
    fun `a crab returns to its rocks afterwards`() {
        val world = CrabWorld()
        val npc = world.spawn(rocks, spot)
        val player = world.player(spot.translate(1, 0))
        world.tick(world.ticks("seq.horror_crab_reveal") + 4)
        assertEquals(NpcMode.OpPlayer2, npc.mode)

        world.move(player, spot.translate(60, 0))
        npc.defaultMode()
        npc.coords = spot.translate(3, 3)
        world.tick(20 + world.ticks("seq.horror_crab_hide") + DisguisedNpcs.RETURN_GRACE + 10)
        assertEquals(rocks.asRSCM(RSCMType.NPC), npc.visType.id)
        assertEquals(spot, npc.coords)
    }

    companion object {
        @JvmStatic
        @BeforeAll
        fun cache() {
            ServerCacheManager.init(240).close()
        }
    }
}
