package org.rsmod.content.areas.city.draynor

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.types.MoveRestrict
import dev.openrune.types.NpcMode
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.junit.jupiter.api.parallel.ResourceLock
import org.rsmod.annotations.InternalApi
import org.rsmod.api.game.process.npc.AiTimerProcessor
import org.rsmod.api.game.process.npc.NpcMovementProcessor
import org.rsmod.api.random.DefaultGameRandom
import org.rsmod.api.random.GameRandom
import org.rsmod.api.registry.npc.NpcRegistry
import org.rsmod.api.registry.player.PlayerRegistry
import org.rsmod.api.registry.zone.ZonePlayerActivityBitSet
import org.rsmod.api.repo.player.PlayerRepository
import org.rsmod.api.route.RayCastValidator
import org.rsmod.api.route.RouteFactory
import org.rsmod.api.route.StepFactory
import org.rsmod.events.EventBus
import org.rsmod.game.cheat.CheatCommandMap
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.NpcList
import org.rsmod.game.entity.Player
import org.rsmod.game.entity.PlayerList
import org.rsmod.game.queue.EngineQueueCache
import org.rsmod.map.CoordGrid
import org.rsmod.map.zone.ZoneKey
import org.rsmod.plugin.scripts.ScriptContext
import org.rsmod.routefinder.collision.CollisionFlagMap
import org.rsmod.routefinder.flag.CollisionFlag

@OptIn(InternalApi::class)
@Execution(ExecutionMode.SAME_THREAD)
@ResourceLock("ServerCacheManager")
class DraynorPossessedChairsTest {
    @Test fun `chairs follow a moving player on their own floor through native timers`() {
        for (level in 0..1) {
            val f = Fixture(level)
            val target = f.player(f.chair.coords.translate(0, 4))
            assertEquals("seq.draynor_possesed_chair".asRSCM(), f.chair.type.walkAnim)
            assertEquals(1, f.chair.aiTimerStart)
            assertEquals(NpcMode.None, f.chair.defaultMode)
            assertEquals(MoveRestrict.Normal, f.chair.moveRestrict)
            assertEquals(0, f.chair.wanderRange)
            f.tick()
            assertEquals(f.chair.spawnCoords.translate(0, 1), f.chair.coords)
            f.move(target, target.coords.translate(1, 0))
            f.tick(3)
            assertTrue(f.chair.isWithinDistance(target, 1))
            assertEquals(level, f.chair.level)
            assertNotEquals(target.coords, f.chair.coords)
        }
    }

    @Test fun `chairs pause after a following spell and can choose a new target later`() {
        val f = Fixture()
        val target = f.player(f.chair.coords.translate(0, 4))
        f.tick(10)
        val stopped = f.chair.coords
        f.move(target, target.coords.translate(3, 0))
        f.tick()
        assertEquals(stopped, f.chair.coords)
        f.tick(8)
        assertEquals(stopped, f.chair.coords)
        f.tick()
        assertNotEquals(stopped, f.chair.coords)
    }

    @Test fun `chairs do not acquire players behind solid scenery`() {
        val f = Fixture()
        f.player(f.chair.coords.translate(0, 4))
        f.collision.add(f.chair.coords.x, f.chair.coords.z + 1, 0, CollisionFlag.LOC or CollisionFlag.LOC_PROJ_BLOCKER)
        f.tick(20)
        assertEquals(f.chair.spawnCoords, f.chair.coords)
        assertTrue(f.chair.routeDestination.isEmpty())
    }

    @Test fun `following uses collision routes around furniture`() {
        val f = Fixture()
        val target = f.player(f.chair.coords.translate(0, 4))
        val furniture = f.chair.coords.translate(0, 1)
        f.collision.add(furniture.x, furniture.z, furniture.level, CollisionFlag.LOC)
        repeat(8) {
            f.tick()
            assertNotEquals(furniture, f.chair.coords)
        }
        assertTrue(f.chair.isWithinDistance(target, 1))
    }

    @Test fun `chairs release targets that leave log out die or become hidden`() {
        val changes: List<(Player) -> Unit> = listOf(
            { it.coords = it.coords.translate(0, 0, 1) },
            { it.coords = CoordGrid(3108, 3352) },
            { it.coords = CoordGrid(3124, 3364) },
            { it.coords = CoordGrid(3120, 3373) },
            { it.pendingLogout = true },
            { it.hidden = true },
            { it.statMap.setCurrentLevel("stat.hitpoints", 0) },
        )
        for (change in changes) {
            val f = Fixture()
            val target = f.player(f.chair.coords.translate(0, 4))
            f.tick()
            val stopped = f.chair.coords
            change(target)
            f.tick(3)
            assertEquals(stopped, f.chair.coords)
            assertTrue(f.chair.routeDestination.isEmpty())
        }
    }

    @Test fun `closed doors cancel an existing following route before another step`() {
        val f = Fixture()
        f.player(f.chair.coords.translate(0, 5))
        f.tick()
        val stopped = f.chair.coords
        f.collision.add(stopped.x, stopped.z + 1, 0, CollisionFlag.LOC or CollisionFlag.LOC_PROJ_BLOCKER)
        f.tick()
        assertEquals(stopped, f.chair.coords)
        assertTrue(f.chair.routeDestination.isEmpty())
    }

    @Test fun `chairs do not chase through the manor entrance or stairs`() {
        for (destination in listOf(CoordGrid(3113, 3352), CoordGrid(3113, 3358, 1))) {
            val f = Fixture()
            f.player(destination)
            f.tick(20)
            assertEquals(f.chair.spawnCoords, f.chair.coords)
        }
    }

    private class Fixture(level: Int = 0) {
        val collision = CollisionFlagMap()
        private val events = EventBus()
        private val players = PlayerRegistry(PlayerList(), collision, ZonePlayerActivityBitSet(), events)
        private val timers = AiTimerProcessor(events)
        private val movement = NpcMovementProcessor(collision, StepFactory(collision), events)
        val chair = Npc(DraynorPossessedChairs.Chair, CoordGrid(3113, if (level == 0) 3354 else 3369, level))
        private val random = object : GameRandom by DefaultGameRandom(1) {
            override fun of(maxExclusive: Int) = 0
            override fun of(minInclusive: Int, maxInclusive: Int) = minInclusive
            override fun randomBoolean(maxExclusive: Int) = true
        }

        init {
            for (floor in 0..1) for (x in 3080..3135 step 8) for (z in 3336..3391 step 8) {
                collision.allocateIfAbsent(x, z, floor)
            }
            NpcRegistry(NpcList(), collision, events).add(chair)
            val context = ScriptContext(events, CheatCommandMap(), EngineQueueCache())
            with(DraynorPossessedChairs(PlayerRepository(players), random, RouteFactory(collision), RayCastValidator(collision))) {
                context.startup()
            }
        }

        fun player(position: CoordGrid) = Player().apply {
            coords = position
            slotId = checkNotNull(players.nextFreeSlot())
            uuid = slotId.toLong()
            players.add(this)
            players.change(this, ZoneKey.NULL, ZoneKey.from(position))
        }

        fun move(player: Player, destination: CoordGrid) {
            val oldZone = ZoneKey.from(player.coords)
            player.coords = destination
            val newZone = ZoneKey.from(destination)
            if (oldZone != newZone) players.change(player, oldZone, newZone)
        }

        fun tick(count: Int = 1) = repeat(count) {
            chair.currentMapClock++
            timers.process(chair)
            movement.process(chair)
        }
    }

    companion object {
        @JvmStatic @BeforeAll fun cache() { ServerCacheManager.init(240).close() }
    }
}
