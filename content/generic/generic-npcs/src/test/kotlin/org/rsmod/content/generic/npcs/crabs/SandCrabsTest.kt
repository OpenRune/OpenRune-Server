package org.rsmod.content.generic.npcs.crabs

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import dev.openrune.types.NpcMode
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.junit.jupiter.api.parallel.ResourceLock
import org.rsmod.annotations.InternalApi
import org.rsmod.api.combat.commons.npc.attackRate
import org.rsmod.api.config.refs.params
import org.rsmod.api.npc.aggression.AggressionTolerance
import org.rsmod.api.player.vars.VarPlayerIntMapSetter
import org.rsmod.content.generic.npcs.disguise.DisguisedNpcs
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.player.PlayerUid
import org.rsmod.game.interact.InteractionPlayerOp
import org.rsmod.map.CoordGrid

@Execution(ExecutionMode.SAME_THREAD)
@ResourceLock("ServerCacheManager")
@OptIn(InternalApi::class)
class SandCrabsTest {
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
    fun `the small variant disguises the same way`() {
        val world = CrabWorld()
        val npc = world.spawn("npc.zeah_sandcrab_small_inactive", spot)
        world.player(spot.translate(0, 1))
        world.tick()
        assertEquals("npc.zeah_sandcrab_small".asRSCM(RSCMType.NPC), npc.visType.id)
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
    fun `a woken crab does not attack a player who is already fighting`() {
        val world = CrabWorld()
        val npc = world.spawn(rocks, spot)
        val player = world.player(spot.translate(1, 0))
        VarPlayerIntMapSetter.set(player, "varp.lastcombat", world.clock.cycle + 1_000)
        world.tick(20)
        assertEquals(crab.asRSCM(RSCMType.NPC), npc.visType.id)
        assertNotEquals(NpcMode.OpPlayer2, npc.mode)
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
    fun `a crab that lost its target returns to its rocks`() {
        val world = CrabWorld()
        val npc = world.spawn(rocks, spot)
        val player = world.player(spot.translate(1, 0))
        world.tick(world.ticks("seq.horror_crab_reveal") + 4)
        assertEquals(NpcMode.OpPlayer2, npc.mode)

        world.move(player, spot.translate(60, 0))
        npc.defaultMode()
        world.tick(20 + world.ticks("seq.horror_crab_hide") + 6)
        assertEquals(rocks.asRSCM(RSCMType.NPC), npc.visType.id)
    }

    @Test
    fun `a crab that was lured off its spot ends up back on it`() {
        val world = CrabWorld()
        val npc = world.spawn(rocks, spot)
        val player = world.player(spot.translate(1, 0))
        world.tick()
        world.move(player, spot.translate(60, 0))
        npc.coords = spot.translate(3, 3)

        world.tick(world.ticks("seq.horror_crab_reveal") + 20 + DisguisedNpcs.RETURN_GRACE + 10)
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
    fun `just under ten minutes the rocks still wake`() {
        val world = CrabWorld()
        val player = world.player(spot.translate(1, 0))
        world.tolerance.isTolerant(player)
        world.stayFor(player, AggressionTolerance.TOLERANCE_TICKS - 10)
        val npc = world.spawn(rocks, spot)
        world.tick()
        assertEquals(crab.asRSCM(RSCMType.NPC), npc.visType.id)
    }

    @Test
    fun `a tolerant player is ignored by a crab that is already awake`() {
        val world = CrabWorld()
        val player = world.player(spot.translate(1, 0))
        world.tolerance.isTolerant(player)
        world.stayFor(player, AggressionTolerance.TOLERANCE_TICKS)
        val npc = world.spawn(rocks, spot)
        world.disguises.wake(npc, SandCrabs.SAND_CRABS.first())

        world.tick(world.ticks("seq.horror_crab_reveal") + 10)
        assertEquals(PlayerUid.NULL, npc.huntPlayer)
        assertNotEquals(NpcMode.OpPlayer2, npc.mode)
    }

    @Test
    fun `walking away and coming back resets the ten minute timer`() {
        val world = CrabWorld()
        val player = world.player(spot.translate(1, 0))
        world.tolerance.isTolerant(player)
        world.stayFor(player, AggressionTolerance.TOLERANCE_TICKS)
        assertTrue(world.tolerance.isTolerant(player))

        world.move(player, spot.translate(AggressionTolerance.LEAVE_DISTANCE + 5, 0))
        assertFalse(world.tolerance.isTolerant(player))
        world.move(player, spot.translate(1, 0))
        assertFalse(world.tolerance.isTolerant(player))

        val npc = world.spawn(rocks, spot)
        world.tick()
        assertEquals(crab.asRSCM(RSCMType.NPC), npc.visType.id)
    }

    @Test
    fun `staying away for a while resets the timer even when coming back to the same spot`() {
        val world = CrabWorld()
        val player = world.player(spot.translate(1, 0))
        world.tolerance.isTolerant(player)
        world.stayFor(player, AggressionTolerance.TOLERANCE_TICKS)
        assertTrue(world.tolerance.isTolerant(player))

        world.clock.cycle += AggressionTolerance.ABSENT_TICKS + 10
        assertFalse(world.tolerance.isTolerant(player))
    }

    @Test
    fun `tolerance only applies to enrolled npc types`() {
        val world = CrabWorld()
        val player = world.player(spot.translate(1, 0))
        world.tolerance.isTolerant(player)
        world.stayFor(player, AggressionTolerance.TOLERANCE_TICKS)
        val rockCrab = world.spawn("npc.horror_rockcrab", spot.translate(0, 4))
        assertFalse(world.tolerance.isTolerant(rockCrab, player))
        assertTrue(world.tolerance.isTolerant(world.spawn(rocks, spot.translate(0, 2)), player))
    }

    @Test
    fun `sand crabs match the wiki stats`() {
        val names = listOf(rocks, crab, "npc.zeah_sandcrab_small_inactive", "npc.zeah_sandcrab_small")
        for (name in names) {
            val type = checkNotNull(ServerCacheManager.getNpc(name.asRSCM(RSCMType.NPC)))
            assertTrue(type.name.isNotEmpty(), "$name has an empty cache definition; see sand_crabs.toml")
            assertEquals(60, type.hitpoints, name)
            assertEquals(2, type.paramOrNull(params.elemental_weakness_type), name)
            assertEquals(20, type.paramOrNull(params.elemental_weakness_percent), name)
            assertTrue(type.hasParam(params.dropped_remains.raw), "$name must leave no remains")
            assertNull(type.paramOrNull(params.dropped_remains), name)
            assertEquals(NpcMode.None, type.defaultMode, name)
            assertEquals(1, type.attack, name)
            assertEquals(1, type.strength, name)
            assertEquals(1, type.defence, name)
            assertEquals(50, type.respawnRate, name)
            assertNull(type.huntMode, name)
            val attackType = type.paramOrNull(params.npc_attack_type)
            assertEquals("category.attacktype_crush".asRSCM(RSCMType.CATEGORY), attackType?.id, name)
        }
        val awake = checkNotNull(ServerCacheManager.getNpc(crab.asRSCM(RSCMType.NPC)))
        assertEquals(15, awake.combatLevel)
        assertEquals("Sand Crab", awake.name)
        assertEquals(4, Npc(crab, spot).attackRate())
        val disguised = checkNotNull(ServerCacheManager.getNpc(rocks.asRSCM(RSCMType.NPC)))
        assertEquals("Sandy rocks", disguised.name)
        assertFalse(disguised.hasOp(1))
    }

    companion object {
        @JvmStatic
        @BeforeAll
        fun cache() {
            ServerCacheManager.init(240).close()
        }
    }
}
