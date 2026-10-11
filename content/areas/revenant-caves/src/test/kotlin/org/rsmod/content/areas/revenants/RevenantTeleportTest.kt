package org.rsmod.content.areas.revenants

import dev.openrune.ServerCacheManager
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.parallel.ResourceLock
import org.mockito.Mockito.*
import org.rsmod.api.area.checker.AreaChecker
import org.rsmod.api.player.hook.*
import org.rsmod.api.player.protect.*
import org.rsmod.game.entity.Player
import org.rsmod.game.queue.WorldQueueList
import org.rsmod.map.CoordGrid
import org.rsmod.routefinder.collision.CollisionFlagMap

@ResourceLock("ServerCacheManager")
@OptIn(org.rsmod.annotations.InternalApi::class)
internal class RevenantTeleportTest {
    @Test fun `native teleport activates after two ticks and restarts after incoming attack`() {
        val f = Fixture(); f.jump(); assertEquals(f.origin, f.player.coords)
        f.tick(); f.player.attr[RevenantTeleportDelay.ATTACK] = f.player.currentMapClock
        f.tick(); assertEquals(f.origin, f.player.coords)
        f.tick(); assertEquals(f.destination, f.player.coords); assertEquals(1, f.arrivals)
    }

    @Test fun `hard diary and PvP combat bypass the NPC delay`() {
        for (diary in listOf(true, false)) {
            val f = Fixture()
            ProtectedAccessLauncher.withProtectedAccess(f.player, f.context) {
                if (diary)vars["varbit.wilderness_diary_hard_complete"] = 1 else vars["varp.lastcombat_pvp"] = 100
            }
            f.jump(); assertEquals(f.destination, f.player.coords); assertEquals(1, f.arrivals)
        }
    }

    @Test fun `moved origin death logout and late denial cancel without delayed arrival fx`() {
        for (cause in listOf("movement", "death", "logout", "denial")) {
            val f = Fixture(); f.jump()
            when (cause) { "movement" -> f.player.coords = f.origin.translateX(1); "death" -> f.player.statMap.setCurrentLevel("stat.hitpoints", 0); "logout" -> f.player.pendingLogout = true; else -> f.denial = "Blocked" }
            f.tick(); f.tick(); assertNotEquals(f.destination, f.player.coords); assertEquals(0, f.arrivals)
        }
    }

    @Test fun `an exempt replacement cancels the older queued teleport`() {
        val f = Fixture(); f.jump()
        val other = f.destination.translateX(1)
        ProtectedAccessLauncher.withProtectedAccess(f.player, f.context) { telejump(other, TeleportType.Exempt) }
        f.tick(); f.tick(); assertEquals(other, f.player.coords); assertEquals(0, f.arrivals)
    }
    private class Fixture {
        val player = Player().apply { slotId = 1; uuid = 1; assignUid(); currentMapClock = 100; processedMapClock = 100; statMap.setBaseLevel("stat.hitpoints", 99); statMap.setCurrentLevel("stat.hitpoints", 99) }
        val origin = CoordGrid(3197, 10056); val destination = CoordGrid(3198, 10057)
        val areas = mock(AreaChecker::class.java) { true }; val collision = CollisionFlagMap(); val queues = WorldQueueList()
        var denial: String? = null; var arrivals = 0
        val validator = PlayerTeleportValidator(setOf(PlayerTeleportValidateHook { _, _, _ -> denial }), setOf(RevenantTeleportDelay()), queues)
        val context = ProtectedAccessContextFactory.empty().copy(getTeleportValidator = { validator }, getAreaChecker = { areas }, getCollision = { collision })
        init {
            player.coords = origin
            for (x in 3180..3210)for (z in 10048..10080)collision.set(x, z, 0, 0)
            ProtectedAccessLauncher.withProtectedAccess(player, context) { vars["varp.lastcombat"] = 100 }
        }
        fun jump() { ProtectedAccessLauncher.withProtectedAccess(player, context) { beginTeleport(); telejump(destination, onArrival = { arrivals++ }) } }
        fun tick() { player.currentMapClock++; player.processedMapClock = player.currentMapClock; val it = queues.iterator(); while (it.hasNext()) { val q = it.next(); if (--q.remainingCycles <= 0) { it.remove(); q.action() } }; it.cleanUp() }
    }
    companion object { @JvmStatic @BeforeAll fun cache() { ServerCacheManager.init(240).close() } }
}
