package org.rsmod.content.other.multicannon

import com.google.inject.AbstractModule
import com.google.inject.Scopes
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
import org.rsmod.api.combat.weapon.scripts.WeaponAttackStylesScript
import org.rsmod.api.combat.weapon.scripts.WeaponAttackTypesScript
import org.rsmod.api.combat.weapon.styles.AttackStyles
import org.rsmod.api.combat.weapon.types.AttackTypes
import org.rsmod.api.testing.GameTestState
import org.rsmod.api.testing.scope.GameTestScope
import org.rsmod.content.other.multicannon.DwarfMulticannon.Companion.BREAK_CYCLES
import org.rsmod.content.other.multicannon.DwarfMulticannon.Companion.BROKEN_LOC
import org.rsmod.content.other.multicannon.DwarfMulticannon.Companion.BROKEN_MESSAGE
import org.rsmod.content.other.multicannon.DwarfMulticannon.Companion.CANNONBALL
import org.rsmod.content.other.multicannon.DwarfMulticannon.Companion.CANNON_LOC
import org.rsmod.content.other.multicannon.DwarfMulticannon.Companion.DECAYED_MESSAGE
import org.rsmod.content.other.multicannon.DwarfMulticannon.Companion.DECAY_AFTER_BREAK_CYCLES
import org.rsmod.content.other.multicannon.DwarfMulticannon.Companion.MAX_AMMO
import org.rsmod.content.other.multicannon.DwarfMulticannon.Companion.PARTS
import org.rsmod.content.other.multicannon.DwarfMulticannon.Companion.REPAIR_MESSAGE
import org.rsmod.content.other.multicannon.DwarfMulticannon.Companion.inTargetRegion
import org.rsmod.content.other.multicannon.DwarfMulticannon.Companion.packCoord
import org.rsmod.content.other.multicannon.DwarfMulticannon.Companion.rangedXp
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
            sent += advanceCollecting(SETUP_TICKS)
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
            assertEquals(CANNON_LOC.asRSCM(RSCMType.LOC), cannon.loc?.id)
        }

    @Test
    fun GameTestState.`set-up loads the balls in the inventory the tick after the furnace`() =
        runInjectedGameTest(DwarfMulticannon::class, null, DwarfMulticannon::class) { script ->
            prepare()
            player.inv[4] = InvObj(CANNONBALL.asRSCM(RSCMType.OBJ), 50)
            with(script) { player.withProtectedAccess { setUp() } }
            val sent = advanceCollecting(SETUP_TICKS)
            assertTrue("You load the cannon with 30 cannonballs." in sent) { "$sent" }
            assertEquals(MAX_AMMO, player.cannonAmmo)
            assertEquals(20, player.inv.count(CANNONBALL))
        }

    @Test
    fun GameTestState.`a second cannon is refused`() =
        runInjectedGameTest(DwarfMulticannon::class, null, DwarfMulticannon::class) { script ->
            prepare()
            player.inv[10] = InvObj(PARTS[0].asRSCM(RSCMType.OBJ), 1)
            with(script) { player.withProtectedAccess { setUp() } }
            advance(ticks = SETUP_TICKS)
            val sent = collect { with(script) { player.withProtectedAccess { setUp() } } }
            assertEquals(listOf("You cannot construct more than one cannon at a time."), sent)
            assertEquals(1, player.inv.count(PARTS[0]))
        }

    @Test
    fun GameTestState.`firing loads up to 30 balls, hits a target in its region and runs dry`() =
        runInjectedGameTest(
            DwarfMulticannon::class,
            WeaponTestModule,
            DwarfMulticannon::class,
            WeaponAttackStylesScript::class,
            WeaponAttackTypesScript::class,
        ) { script ->
            val origin = prepare()
            with(script) { player.withProtectedAccess { setUp() } }
            advance(ticks = SETUP_TICKS)
            player.inv[4] = InvObj(CANNONBALL.asRSCM(RSCMType.OBJ), 50)

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
    fun GameTestState.`load-x asks for the amount once, then loads that many`() =
        runInjectedGameTest(DwarfMulticannon::class, null, DwarfMulticannon::class) { script ->
            val origin = prepare()
            with(script) { player.withProtectedAccess { setUp() } }
            advance(ticks = SETUP_TICKS)
            player.inv[4] = InvObj(CANNONBALL.asRSCM(RSCMType.OBJ), 50)

            with(script) { player.withProtectedAccess { loadX(origin) } }
            player.resumeCountDialog(10)
            advance(ticks = 1)
            assertMessageSent("You will now load 10 cannonballs when using Load-x.")
            assertEquals(10, player.cannonLoadX)
            assertEquals(0, player.cannonAmmo)

            with(script) { player.withProtectedAccess { loadX(origin) } }
            assertMessageSent("You load the cannon with 10 cannonballs.")
            assertEquals(10, player.cannonAmmo)
            assertEquals(40, player.inv.count(CANNONBALL))
        }

    @Test
    fun GameTestState.`empty gives the balls back with the unload line`() =
        runInjectedGameTest(DwarfMulticannon::class, null, DwarfMulticannon::class) { script ->
            val origin = prepare()
            with(script) { player.withProtectedAccess { setUp() } }
            advance(ticks = SETUP_TICKS)
            player.inv[4] = InvObj(CANNONBALL.asRSCM(RSCMType.OBJ), 12)
            with(script) { player.withProtectedAccess { load(origin, startFiring = false) } }
            with(script) { player.withProtectedAccess { empty(origin) } }
            assertMessageSent("You unload your cannon and receive Steel cannonball x 12.")
            assertEquals(12, player.inv.count(CANNONBALL))
            assertEquals(0, player.cannonAmmo)
        }

    @Test
    fun GameTestState.`pick-up returns every part and the loaded balls exactly once`() =
        runInjectedGameTest(DwarfMulticannon::class, null, DwarfMulticannon::class) { script ->
            val origin = prepare()
            with(script) { player.withProtectedAccess { setUp() } }
            advance(ticks = SETUP_TICKS)
            player.inv[4] = InvObj(CANNONBALL.asRSCM(RSCMType.OBJ), 12)
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
            advance(ticks = SETUP_TICKS)
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
    fun GameTestState.`it breaks after 25 minutes and Repair restores it`() =
        runInjectedGameTest(DwarfMulticannon::class, null, DwarfMulticannon::class) { script ->
            val origin = prepare()
            with(script) { player.withProtectedAccess { setUp() } }
            advance(ticks = SETUP_TICKS)
            val cannon = checkNotNull(script.cannonOf(player))
            advance(ticks = BREAK_CYCLES - SETUP_TICKS - 5)
            assertFalse(cannon.broken)

            val sent = advanceCollecting(10)
            assertEquals(1, sent.count { it == BROKEN_MESSAGE }) { "$sent" }
            assertTrue(cannon.broken)
            assertEquals(BROKEN_LOC.asRSCM(RSCMType.LOC), cannon.loc?.id)

            with(script) { player.withProtectedAccess { repair(origin) } }
            assertMessageSent(REPAIR_MESSAGE)
            assertFalse(cannon.broken)
            assertEquals(CANNON_LOC.asRSCM(RSCMType.LOC), cannon.loc?.id)
            assertTrue(advanceCollecting(BREAK_CYCLES - 10).none { it == BROKEN_MESSAGE })
        }

    @Test
    fun GameTestState.`an unrepaired cannon disappears 10 minutes after breaking`() =
        runInjectedGameTest(DwarfMulticannon::class, null, DwarfMulticannon::class) { script ->
            prepare()
            with(script) { player.withProtectedAccess { setUp() } }
            advance(ticks = SETUP_TICKS)
            advance(ticks = BREAK_CYCLES)
            assertTrue(checkNotNull(script.cannonOf(player)).broken)

            val sent = advanceCollecting(DECAY_AFTER_BREAK_CYCLES + 5)
            assertEquals(1, sent.count { it == DECAYED_MESSAGE }) { "$sent" }
            assertNull(script.cannonOf(player))
            assertTrue(player.cannonDecayed)
            assertEquals(4, player.cannonStage)
        }

    @Test
    fun GameTestState.`the cannon stays set up through a logout`() =
        runInjectedGameTest(DwarfMulticannon::class, null, DwarfMulticannon::class) { script ->
            val origin = prepare()
            with(script) { player.withProtectedAccess { setUp() } }
            advance(ticks = SETUP_TICKS)
            val cannon = checkNotNull(script.cannonOf(player))
            val loc = checkNotNull(cannon.loc)

            with(script) { player.logoutCannon() }
            assertNull(cannon.owner)
            assertTrue(locExists(loc.coords, checkNotNull(ServerCacheManager.getObject(loc.id))))
            for (part in PARTS) {
                assertEquals(0, player.inv.count(part), part)
            }

            with(script) { player.loginCannon() }
            assertEquals(player, cannon.owner)
            assertEquals(4, player.cannonStage)
            assertEquals(packCoord(origin), player.cannonCoord)
            assertFalse(player.cannonDecayed)
        }

    @Test
    fun GameTestState.`a cannon missing from the world at login counts as lost`() =
        runInjectedGameTest(DwarfMulticannon::class, null, DwarfMulticannon::class) { script ->
            player.cannonStage = 4
            player.cannonAmmo = 7
            with(script) { player.loginCannon() }
            assertTrue(player.cannonDecayed)
            assertEquals(4, player.cannonStage)
            assertEquals(7, player.cannonAmmo)
        }

    @Test
    fun GameTestState.`in single-way combat it only fires at the owner's attacker`() =
        runInjectedGameTest(DwarfMulticannon::class, null, DwarfMulticannon::class) { script ->
            val origin = prepare()
            with(script) { player.withProtectedAccess { setUp() } }
            advance(ticks = SETUP_TICKS)
            val cannon = checkNotNull(script.cannonOf(player))
            val center = origin.translate(1, 1)
            val near = spawnNpc(center.translate(0, 3), dummy)
            val attacker = spawnNpc(center.translate(0, 5), dummy)
            cannon.direction = 0
            assertEquals(near, script.findTarget(player, cannon))

            player.setVarp("varp.aggressive_npc", attacker.uid.packed)
            player.setVarp("varp.lastcombat", mapClock.cycle)
            assertEquals(attacker, script.findTarget(player, cannon))
        }

    @Test
    fun `ranged xp is paid only on the damage the target can take`() {
        assertEquals(16.0, rangedXp(damage = 30, targetHitpoints = 8))
        assertEquals(60.0, rangedXp(damage = 30, targetHitpoints = 50))
        assertEquals(0.0, rangedXp(damage = 0, targetHitpoints = 8))
    }

    @Test
    fun `target regions overlap one tile off the centre lines`() {
        assertEquals(listOf(0), directionsOf(0, 3))
        assertEquals(listOf(0, 1), directionsOf(1, 3))
        assertEquals(listOf(1, 2), directionsOf(3, 1))
        assertEquals(listOf(4, 5), directionsOf(-1, -2))
        assertEquals(listOf(1), directionsOf(2, 2))
        assertEquals(emptyList<Int>(), directionsOf(1, 1))
        assertEquals(emptyList<Int>(), directionsOf(0, 0))
    }

    private fun directionsOf(dx: Int, dz: Int): List<Int> = (0 until 8).filter { inTargetRegion(it, dx, dz) }

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

    private companion object {
        const val SETUP_TICKS = 13
    }

    private object WeaponTestModule : AbstractModule() {
        override fun configure() {
            bind(AttackTypes::class.java).`in`(Scopes.SINGLETON)
            bind(AttackStyles::class.java).`in`(Scopes.SINGLETON)
        }
    }
}
