package org.rsmod.content.generic.npcs.crabs

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import dev.openrune.types.NpcMode
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.junit.jupiter.api.parallel.ResourceLock
import org.rsmod.annotations.InternalApi
import org.rsmod.api.npc.aggression.AggressionTolerance
import org.rsmod.content.generic.npcs.disguise.DisguisedNpcs
import org.rsmod.game.entity.player.PlayerUid
import org.rsmod.game.interact.InteractionPlayerOp
import org.rsmod.map.CoordGrid

@Execution(ExecutionMode.SAME_THREAD)
@ResourceLock("ServerCacheManager")
@OptIn(InternalApi::class)
class CrabsTest {
    private val rocks = "npc.zeah_sandcrab_inactive"
    private val crab = "npc.zeah_sandcrab"
    private val spot = CoordGrid(1773, 3460, 0)

    @Test
    fun `rocks wake into a sand crab when a player steps next to them`() {
        val world = CrabWorld()
        val npc = world.spawn(rocks, spot)
        world.player(spot.translate(1, 1))
        world.tick()
        assertEquals(crab.asRSCM(RSCMType.NPC), npc.visType.id)
        assertEquals(rocks.asRSCM(RSCMType.NPC), npc.type.id)
        assertEquals(0, npc.vars[DisguisedNpcs.IDLE])
        assertTrue(npc.isDelayed)
    }

    @Test
    fun `rocks stay disguised for a player two tiles away`() {
        val world = CrabWorld()
        val npc = world.spawn(rocks, spot)
        world.player(spot.translate(2, 0))
        world.tick(5)
        assertEquals(rocks.asRSCM(RSCMType.NPC), npc.visType.id)
        assertFalse(npc.isDelayed)
    }

    @Test
    fun `the woken crab goes for the player once it has finished revealing itself`() {
        val world = CrabWorld()
        val npc = world.spawn(rocks, spot)
        val player = world.player(spot.translate(1, 0))
        world.tick()
        world.tick(world.ticks("seq.horror_crab_reveal") - 1)
        assertNotEquals(NpcMode.OpPlayer2, npc.mode)

        world.tick(3)
        assertEquals(NpcMode.OpPlayer2, npc.mode)
        val interaction = npc.interaction as InteractionPlayerOp
        assertEquals(player, interaction.target)
    }

    @Test
    fun `a crab with nobody around burrows back into its rocks and heals`() {
        val world = CrabWorld()
        val npc = world.spawn(rocks, spot)
        val player = world.player(spot.translate(1, 0))
        world.tick()
        world.move(player, spot.translate(60, 0))
        npc.hitpoints = 12

        world.tick(world.ticks("seq.horror_crab_reveal") + 10)
        assertEquals(crab.asRSCM(RSCMType.NPC), npc.visType.id)

        world.tick(20 + world.ticks("seq.horror_crab_hide") + 2)
        assertEquals(rocks.asRSCM(RSCMType.NPC), npc.visType.id)
        assertEquals(npc.type.hitpoints, npc.hitpoints)
        assertEquals(spot, npc.coords)
    }

    @Test
    fun `a crab that lost its target or was lured off its spot returns to its rocks`() {
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

    @Test
    fun `a returned crab wakes again for the next player`() {
        val world = CrabWorld()
        val npc = world.spawn(rocks, spot)
        val player = world.player(spot.translate(1, 0))
        world.tick()
        world.move(player, spot.translate(60, 0))
        val restTicks = world.ticks("seq.horror_crab_reveal") + 20 + world.ticks("seq.horror_crab_hide")
        world.tick(restTicks + 5)
        assertEquals(rocks.asRSCM(RSCMType.NPC), npc.visType.id)

        world.move(player, spot.translate(0, -1))
        world.tick()
        assertEquals(crab.asRSCM(RSCMType.NPC), npc.visType.id)
    }

    @Test
    fun `after ten minutes in the area the rocks no longer wake`() {
        val world = CrabWorld()
        val player = world.player(spot.translate(1, 0))
        assertFalse(world.tolerance.isTolerant(player))
        world.stayFor(player, AggressionTolerance.TOLERANCE_TICKS)
        assertTrue(world.tolerance.isTolerant(player))

        val npc = world.spawn(rocks, spot)
        world.tick(10)
        assertEquals(rocks.asRSCM(RSCMType.NPC), npc.visType.id)
    }

    @Test
    fun `a tolerant player is ignored by a crab that is already awake`() {
        val world = CrabWorld()
        val player = world.player(spot.translate(1, 0))
        world.tolerance.isTolerant(player)
        world.stayFor(player, AggressionTolerance.TOLERANCE_TICKS)
        val npc = world.spawn(rocks, spot)
        world.disguises.wake(npc, Crabs.CRABS.first())

        world.tick(world.ticks("seq.horror_crab_reveal") + 10)
        assertEquals(PlayerUid.NULL, npc.huntPlayer)
        assertNotEquals(NpcMode.OpPlayer2, npc.mode)
    }

    @Test
    fun `a large crab wakes for a player beside any edge of its body`() {
        val world = CrabWorld()
        val npc = world.spawn("npc.giant_rockcrab_hidden", spot)
        val player = world.player(spot.translate(3, 3))
        world.tick(5)
        assertEquals("npc.giant_rockcrab_hidden".asRSCM(RSCMType.NPC), npc.visType.id)

        world.move(player, spot.translate(2, 2))
        world.tick()
        assertEquals("npc.giant_rockcrab".asRSCM(RSCMType.NPC), npc.visType.id)
    }

    companion object {
        @JvmStatic
        @BeforeAll
        fun cache() {
            ServerCacheManager.init(240).close()
        }
    }
}
