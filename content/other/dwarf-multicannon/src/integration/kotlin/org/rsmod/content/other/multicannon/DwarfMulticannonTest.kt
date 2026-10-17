package org.rsmod.content.other.multicannon

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import dev.openrune.types.NpcServerType
import dev.openrune.types.util.UncheckedType
import net.rsprot.protocol.game.outgoing.misc.player.MessageGame
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.rsmod.api.testing.GameTestState
import org.rsmod.api.testing.scope.GameTestScope
import org.rsmod.content.other.multicannon.DwarfMulticannon.Companion.CANNONBALL
import org.rsmod.content.other.multicannon.DwarfMulticannon.Companion.MAX_AMMO
import org.rsmod.content.other.multicannon.DwarfMulticannon.Companion.PARTS
import org.rsmod.content.other.multicannon.DwarfMulticannon.Companion.directionOf
import org.rsmod.content.other.multicannon.DwarfMulticannon.Companion.packCoord
import org.rsmod.game.inv.InvObj
import org.rsmod.map.CoordGrid

@OptIn(UncheckedType::class)
@Execution(ExecutionMode.SAME_THREAD)
class DwarfMulticannonTest {
    @Test
    fun GameTestState.`set-up builds the cannon in four stages and sets the plugin varps`() =
        runInjectedGameTest(DwarfMulticannon::class, null, DwarfMulticannon::class) { script ->
            val origin = prepare()
            val sent = collect { with(script) { player.withProtectedAccess { setUp() } } }
            sent += advanceCollecting(10)
            assertEquals(
                listOf(
                    "You place the cannon base on the ground.",
                    "You add the stand.",
                    "You add the barrels.",
                    "You add the furnace.",
                ),
                sent,
            )
            assertEquals(4, player.cannonStage)
            assertEquals(packCoord(origin), player.cannonCoord)
            assertFalse(player.cannonDecayed)
            for (part in PARTS) {
                assertEquals(0, player.inv.count(part), part)
            }
            val cannon = checkNotNull(script.cannonOf(player))
            assertEquals(4, cannon.stage)
            assertEquals(origin, cannon.loc?.coords)
            assertEquals("loc.dwarf_multicannon1".asRSCM(RSCMType.LOC), cannon.loc?.id)
        }

    @Test
    fun GameTestState.`a second cannon is refused`() =
        runInjectedGameTest(DwarfMulticannon::class, null, DwarfMulticannon::class) { script ->
            prepare()
            player.inv[10] = InvObj(PARTS[0].asRSCM(RSCMType.OBJ), 1)
            with(script) { player.withProtectedAccess { setUp() } }
            advance(ticks = 10)
            val sent = collect { with(script) { player.withProtectedAccess { setUp() } } }
            assertEquals(listOf("You cannot construct more than one cannon at a time."), sent)
            assertEquals(1, player.inv.count(PARTS[0]))
        }

    @Test
    fun GameTestState.`firing loads up to 30 balls, hits a target in its octant and runs dry`() =
        runInjectedGameTest(DwarfMulticannon::class, null, DwarfMulticannon::class) { script ->
            val origin = prepare()
            player.inv[4] = InvObj(CANNONBALL.asRSCM(RSCMType.OBJ), 50)
            with(script) { player.withProtectedAccess { setUp() } }
            advance(ticks = 10)

            val center = origin.translate(1, 1)
            val target = spawnNpc(center.translate(0, 3), dummy) { hitpoints = 10_000 }
            with(script) { player.withProtectedAccess { fire(origin) } }
            assertMessageSent("You load the cannon with 30 cannonballs.")
            assertEquals(20, player.inv.count(CANNONBALL))
            assertEquals(MAX_AMMO, player.cannonAmmo)

            assertTrue(advanceCollecting(8).none { it == "Your cannon is out of ammo!" })
            assertTrue(player.cannonAmmo < MAX_AMMO) { "ammo=${player.cannonAmmo}" }
            assertTrue(target.hitpoints <= 10_000)

            val sent = advanceCollecting(8 * MAX_AMMO)
            assertEquals(0, player.cannonAmmo)
            assertEquals(1, sent.count { it == "Your cannon is out of ammo!" }) { "$sent" }
            assertTrue(advanceCollecting(20).none { it == "Your cannon is out of ammo!" })
            assertFalse(checkNotNull(script.cannonOf(player)).firing)
        }

    @Test
    fun GameTestState.`pick-up returns every part and the loaded balls exactly once`() =
        runInjectedGameTest(DwarfMulticannon::class, null, DwarfMulticannon::class) { script ->
            val origin = prepare()
            player.inv[4] = InvObj(CANNONBALL.asRSCM(RSCMType.OBJ), 12)
            with(script) { player.withProtectedAccess { setUp() } }
            advance(ticks = 10)
            with(script) { player.withProtectedAccess { load(origin, startFiring = false) } }
            assertEquals(12, player.cannonAmmo)
            assertEquals(0, player.inv.count(CANNONBALL))

            with(script) { player.withProtectedAccess { pickUp(origin) } }
            assertMessageSent("You pick up the cannon. It's really heavy.")
            for (part in PARTS) {
                assertEquals(1, player.inv.count(part), part)
            }
            assertEquals(12, player.inv.count(CANNONBALL))
            assertEquals(0, player.cannonStage)
            assertEquals(0, player.cannonAmmo)
            assertNull(script.cannonOf(player))
        }

    @Test
    fun GameTestState.`pick-up without room changes nothing`() =
        runInjectedGameTest(DwarfMulticannon::class, null, DwarfMulticannon::class) { script ->
            val origin = prepare()
            with(script) { player.withProtectedAccess { setUp() } }
            advance(ticks = 10)
            val filler = InvObj("obj.bronze_sword".asRSCM(RSCMType.OBJ), 1)
            for (slot in 0 until player.inv.size) {
                if (player.inv[slot] == null) player.inv[slot] = filler
            }
            player.inv[0] = null
            with(script) { player.withProtectedAccess { pickUp(origin) } }
            assertMessageSent("You don't have enough inventory space to pick up the cannon.")
            assertEquals(4, player.cannonStage)
            assertNotNull(script.cannonOf(player))
            for (part in PARTS) {
                assertEquals(0, player.inv.count(part), part)
            }
        }

    @Test
    fun GameTestState.`a cannon left by a logout goes back to the bank once`() =
        runInjectedGameTest(DwarfMulticannon::class, null, DwarfMulticannon::class) { script ->
            player.cannonStage = 4
            player.cannonAmmo = 7
            with(script) { player.reclaimToBank() }
            val bank = player.invMap.getOrPut("inv.bank")
            for (part in PARTS) {
                assertEquals(1, bank.count(part), part)
            }
            assertEquals(7, bank.count(CANNONBALL))
            assertEquals(0, player.cannonStage)
            assertEquals(0, player.cannonAmmo)

            with(script) { player.reclaimToBank() }
            assertEquals(1, bank.count(PARTS[0]))
        }

    @Test
    fun `octants run clockwise from north`() {
        assertEquals(0, directionOf(0, 5))
        assertEquals(1, directionOf(3, 3))
        assertEquals(2, directionOf(5, 0))
        assertEquals(4, directionOf(0, -5))
        assertEquals(6, directionOf(-5, 0))
        assertEquals(7, directionOf(-3, 3))
    }

    private fun GameTestScope.collect(action: () -> Unit): MutableList<String> {
        action()
        return client.mapOf(MessageGame::message).toMutableList()
    }

    private fun GameTestScope.advanceCollecting(ticks: Int): List<String> {
        val sent = mutableListOf<String>()
        repeat(ticks) {
            advance(ticks = 1)
            sent += client.mapOf(MessageGame::message)
        }
        return sent
    }

    private fun GameTestScope.prepare(): CoordGrid {
        val center = CoordGrid(0, 50, 50, 20, 20)
        for (dx in -12..12 step 4) {
            for (dz in -12..12 step 4) {
                allocZoneCollision(center.translate(dx, dz))
            }
        }
        player.teleport(center)
        player.clearInv()
        for ((slot, part) in PARTS.withIndex()) {
            player.inv[slot] = InvObj(part.asRSCM(RSCMType.OBJ), 1)
        }
        return center.translate(-1, -1)
    }

    private val dummy: NpcServerType
        get() = checkNotNull(ServerCacheManager.getNpc("npc.man".asRSCM(RSCMType.NPC)))
}
