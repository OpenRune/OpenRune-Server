package org.rsmod.content.minigames.motherlodemine

import dev.openrune.ServerCacheManager
import dev.openrune.cache.MAPS
import dev.openrune.map.GameMapBuilder
import dev.openrune.map.GameMapDecoder
import dev.openrune.map.loc.MapLocListDecoder
import dev.openrune.map.tile.MapTileDecoder
import dev.openrune.map.util.InlineByteBuf
import dev.openrune.rscm.RSCM.asRSCM
import net.rsprot.protocol.game.outgoing.zone.payload.MapAnim
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.ResourceLock
import org.rsmod.api.delivery.DeliveryClock
import org.rsmod.api.delivery.DeliveryLineRegistry
import org.rsmod.api.delivery.DeliveryManager
import org.rsmod.api.game.process.GameLifecycle
import org.rsmod.api.game.process.npc.NpcMovementProcessor
import org.rsmod.api.random.GameRandom
import org.rsmod.api.route.BoundValidator
import org.rsmod.api.route.RayCastValidator
import org.rsmod.api.route.StepFactory
import org.rsmod.content.minigames.motherlodemine.MotherlodeMiningTest.Fixture
import org.rsmod.content.minigames.motherlodemine.configs.MotherlodeConstants
import org.rsmod.content.minigames.motherlodemine.configs.MotherlodeConstants.isMotherlodeUpperFloor
import org.rsmod.content.minigames.motherlodemine.configs.MotherlodeDelivery
import org.rsmod.content.minigames.motherlodemine.scripts.MotherlodeEntranceScript
import org.rsmod.game.entity.Player
import org.rsmod.game.entity.PlayerList
import org.rsmod.game.loc.BoundLocInfo
import org.rsmod.map.CoordGrid
import org.rsmod.map.square.MapSquareKey
import org.rsmod.map.zone.ZoneKey
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.routefinder.RouteFinding
import org.rsmod.routefinder.flag.CollisionFlag

@ResourceLock("ServerCacheManager")
class MotherlodeMachineTest {
    private val random = object : GameRandom {
        override fun of(maxExclusive: Int) = 0
        override fun of(minInclusive: Int, maxInclusive: Int) = minInclusive
        override fun randomDouble() = 0.0
    }

    @Test fun `ore and xp arrive on the tick the native floater enters the sack`() {
        val f = Fixture()
        val cache = ServerCacheManager.init(241)
        try {
            val group = (58 shl 8) or 88
            val tiles = MapTileDecoder.decode(InlineByteBuf(checkNotNull(cache.data(MAPS, group, 0))))
            val locs = MapLocListDecoder.decode(InlineByteBuf(checkNotNull(cache.data(MAPS, group, 1))))
            for (level in 0..3) for (x in 3712..3775 step 8) for (z in 5632..5695 step 8) {
                f.collision.allocateIfAbsent(x, z, level)
            }
            val builder = GameMapBuilder()
            GameMapDecoder.putMaps(f.collision, MapSquareKey(58, 88), tiles)
            GameMapDecoder.putLocs(builder, f.collision, MapSquareKey(58, 88), tiles, locs)
            for ((zone, data) in builder.zoneBuilders) f.storage.mapLocs[zone] = data.build()
        } finally { cache.close() }
        val rockfalls = MotherlodeConstants.ROCKFALLS.map { it.asRSCM() }.toSet()
        for (x in 3712..3775 step 8) for (z in 5632..5695 step 8) {
            for (loc in f.locs.findAll(ZoneKey.fromAbsolute(x, z, 0)).toList()) {
                if (loc.id in rockfalls) f.rockfalls.clear(BoundLocInfo(loc, checkNotNull(ServerCacheManager.getObject(loc.id))))
            }
        }
        val landings = listOf(CoordGrid(3755, 5675), CoordGrid(3755, 5673)) +
            MotherlodeEntranceScript.ShortcutDestinations.values
        for (tile in landings) {
            val canLeave = listOf(tile.translate(1, 0), tile.translate(-1, 0),
                tile.translate(0, 1), tile.translate(0, -1)).any {
                StepFactory(f.collision).validated(tile, it, size = 1) != CoordGrid.NULL
            }
            val openNearby = (-3..3).flatMap { dx -> (-3..3).map { dz -> tile.translate(dx, dz) } }
                .filter { f.collision[it.x, it.z, it.level] and CollisionFlag.BLOCK_WALK == 0 }
            assertTrue(canLeave, "Trapped mine landing $tile; nearby floor=$openNearby")
        }
        val entries = mapOf(CoordGrid(3744, 5642) to CoordGrid(3744, 5641),
            CoordGrid(3745, 5645) to CoordGrid(3745, 5646),
            CoordGrid(3760, 5670) to CoordGrid(3759, 5670),
            CoordGrid(3764, 5671) to CoordGrid(3765, 5671))
        for ((tunnel, entry) in entries) {
            val loc = f.locs.findAll(ZoneKey.from(tunnel)).single {
                it.coords == tunnel && it.id == "loc.motherlode_shortcut".asRSCM()
            }
            val type = checkNotNull(ServerCacheManager.getObject(loc.id))
            assertTrue(RouteFinding(f.collision).findRoute(level = 0, srcX = entry.x, srcZ = entry.z,
                destX = tunnel.x, destZ = tunnel.z, locShape = loc.shapeId, locAngle = loc.angleId,
                destWidth = type.width, destLength = type.length, blockAccessFlags = type.forceApproachFlags,
                moveNear = false).success, "Tunnel cannot be approached from $entry to $tunnel")
        }
        val lowerLadder = f.locs.findAll(ZoneKey.fromAbsolute(3755, 5673, 0))
            .single { it.coords == CoordGrid(3755, 5673) && it.id == "loc.motherlode_ladder_bottom".asRSCM() }
        val ladderType = checkNotNull(ServerCacheManager.getObject(lowerLadder.id))
        val ladderPlayer = Player().apply { coords = CoordGrid(3755, 5672) }
        assertTrue(BoundValidator(f.collision).touches(ladderPlayer.avatar, BoundLocInfo(lowerLadder, ladderType)))
        assertEquals(CoordGrid.NULL, StepFactory(f.collision).validated(ladderPlayer.coords, lowerLadder.coords))
        val upperHopper = f.locs.findAll(ZoneKey.fromAbsolute(3755, 5677, 0))
            .single { it.id == MotherlodeConstants.HOPPER_LOC.asRSCM() }
        val hopperType = checkNotNull(ServerCacheManager.getObject(upperHopper.id))
        for (start in listOf(CoordGrid(3755, 5675), CoordGrid(3753, 5676), CoordGrid(3760, 5676))) {
            val hopperRoute = RouteFinding(f.collision).findRoute(level = 0, srcX = start.x, srcZ = start.z,
                destX = upperHopper.coords.x, destZ = upperHopper.coords.z,
                destWidth = hopperType.width, destLength = hopperType.length,
                locShape = upperHopper.shapeId, locAngle = upperHopper.angleId,
                blockAccessFlags = hopperType.forceApproachFlags)
            assertTrue(hopperRoute.success, "Upper hopper must be approachable from $start")
            val end = hopperRoute.last()
            val adjacent = CoordGrid(end.x, end.z, end.level)
            assertEquals(1, adjacent.chebyshevDistance(upperHopper.coords))
            assertTrue(RayCastValidator(f.collision).hasLineOfSight(adjacent, upperHopper.coords),
                "The adjacent hopper approach must allow the deposit operation")
        }
        val machine = MotherlodeMachine(f.locs, f.npcs, random, f.collision)
        machine.tick()
        assertTrue(machine.repairStrut(CoordGrid(3742, 5669)))
        assertTrue(machine.isWaterFlowing)
        assertWheel(f, 5668, fixed = true)
        assertWheel(f, 5662, fixed = false)
        assertChannel(f, flowing = true)
        assertTrue(f.locs.findLoc(CoordGrid(3744, 5669), MotherlodeConstants.STRUT_FIXED_LOC))
        val delivery = DeliveryFixture(f, machine)
        delivery.enqueue()
        machine.spawnPayDirtFloater()
        val dirt = f.npcList.single { it.id == MotherlodeConstants.NPC_PAYDIRT_FLOATER.asRSCM() }
        val movement = NpcMovementProcessor(f.collision, StepFactory(f.collision), f.events)
        val path = MotherlodeConstants.FLOAT_CHANNEL
        for (tile in path.drop(1)) {
            f.events.publish(GameLifecycle.StartCycle)
            dirt.currentMapClock = f.clock.cycle
            machine.tick()
            movement.process(dirt)
            f.clock.tick()
            f.events.publish(GameLifecycle.LateCycle)
            assertEquals(tile, dirt.coords)
            assertEquals(0, delivery.stored())
            assertEquals(0, delivery.player.statMap.getXP("stat.mining"))
        }
        f.events.publish(GameLifecycle.StartCycle)
        machine.tick()
        f.clock.tick()
        f.events.publish(GameLifecycle.LateCycle)
        assertFalse(dirt.isSlotAssigned)
        assertEquals(1, delivery.stored())
        assertEquals(15, delivery.player.statMap.getXP("stat.mining"))
    }

    @Test fun `repairs control the matching wheel and water stays flowing until both wheels break`() {
        val f = Fixture()
        f.loc(MotherlodeConstants.STRUT_FIXED_LOC, CoordGrid(3744, 5663))
        f.loc(MotherlodeConstants.STRUT_FIXED_LOC, CoordGrid(3744, 5669))
        f.loc(MotherlodeConstants.WHEEL_BROKEN_LOC, CoordGrid(3743, 5662))
        f.loc(MotherlodeConstants.WHEEL_BROKEN_LOC, CoordGrid(3743, 5668))
        val upperChannel = f.loc("loc.waterfall_overlay_1_brighter", CoordGrid(3752, 5676))
        val machine = MotherlodeMachine(f.locs, f.npcs, random, f.collision)
        machine.tick()
        assertFalse(machine.isWaterFlowing)
        assertChannel(f, flowing = false)
        for (z in listOf(5663, 5669)) {
            assertTrue(machine.isStrutBroken(CoordGrid(3742, z)))
            assertTrue(f.locs.findLoc(CoordGrid(3744, z), MotherlodeConstants.STRUT_FIXED_LOC))
            assertFalse(machine.repairStrut(CoordGrid(3744, z)))
        }
        assertTrue(machine.repairStrut(CoordGrid(3742, 5669)))
        assertFalse(machine.repairStrut(CoordGrid(3742, 5669)))
        assertTrue(machine.isWaterFlowing)
        assertWheel(f, 5668, fixed = true)
        assertWheel(f, 5662, fixed = false)
        assertChannel(f, flowing = true)
        f.updates.clear()
        repeat(MotherlodeConstants.STRUT_ROUND_TICKS) { machine.tick() }
        assertTrue(f.updates.updatedZones.isEmpty(), "Looping water needs no repeated packets")
        assertTrue(machine.repairStrut(CoordGrid(3742, 5663)))
        assertWheel(f, 5662, fixed = true)
        repeat(MotherlodeConstants.STRUT_ROUND_TICKS) { machine.tick() }
        assertWheel(f, 5668, fixed = false)
        assertWheel(f, 5662, fixed = true)
        assertTrue(machine.isWaterFlowing)
        assertChannel(f, flowing = true)
        repeat(MotherlodeConstants.STRUT_ROUND_TICKS) { machine.tick() }
        assertFalse(machine.isWaterFlowing)
        assertWheel(f, 5662, fixed = false)
        assertChannel(f, flowing = false)
        assertTrue(machine.repairStrut(CoordGrid(3742, 5669)))
        assertWheel(f, 5668, fixed = true)
        assertChannel(f, flowing = true)
        assertEquals(upperChannel.entity, f.locs.findExact(upperChannel.coords, upperChannel.shape)?.entity)
        assertTrue(f.npcList.none { it.name.contains("Pay-dirt", ignoreCase = true) })
        assertTrue(f.updates.updatedZones.values.flatMap { it.toList() }.none { it is MapAnim })
    }

    private fun assertWheel(f: Fixture, z: Int, fixed: Boolean) {
        val wheel = checkNotNull(f.locs.findExact(CoordGrid(3743, z), org.rsmod.game.loc.LocShape.CentrepieceStraight))
        assertEquals((if (fixed) MotherlodeConstants.WHEEL_FIXED_LOC else MotherlodeConstants.WHEEL_BROKEN_LOC).asRSCM(), wheel.id)
        assertEquals(if (fixed) 0 else 2, wheel.angleId)
        assertEquals(fixed, f.locs.findLoc(CoordGrid(3743, z + 3), "loc.waterfall_foam"))
        val strut = checkNotNull(f.locs.findExact(CoordGrid(3742, z + 1), org.rsmod.game.loc.LocShape.CentrepieceStraight))
        assertEquals((if (fixed) MotherlodeConstants.STRUT_FIXED_LOC else MotherlodeConstants.STRUT_BROKEN_LOC).asRSCM(), strut.id)
        assertEquals(0, strut.angleId)
    }

    private fun assertChannel(f: Fixture, flowing: Boolean) {
        val expected = buildMap {
            for (z in 5660..5671) put(CoordGrid(3743, z), 2)
            for (x in 3743..3747) put(CoordGrid(x, 5672), 3)
            for (z in 5661..5672) put(CoordGrid(3748, z), 0)
            for (x in 3744..3748) put(CoordGrid(x, 5660), 1)
        }
        for ((tile, angle) in expected) {
            val loc = f.locs.findAll(tile).singleOrNull { it.id == "loc.waterfall_overlay_1_brighter".asRSCM() }
            assertEquals(flowing, loc != null, "Water at $tile")
            if (loc != null) {
                assertEquals(22, loc.shapeId)
                assertEquals(angle, loc.angleId)
            }
        }
        for (tile in listOf(CoordGrid(3744, 5672), CoordGrid(3748, 5671), CoordGrid(3748, 5660), CoordGrid(3744, 5660))) {
            assertEquals(flowing, f.locs.findLoc(tile, "loc.waterfall_foam_small"), "Foam at $tile")
        }
    }

    @Test fun `stopped wheels pause the floater and delivery together`() {
        val f = Fixture()
        for (z in 5660..5671) {
            f.collision.allocateIfAbsent(3748, z, 0)
            f.collision.add(3748, z, 0, CollisionFlag.BLOCK_WALK)
        }
        f.loc(MotherlodeConstants.WHEEL_BROKEN_LOC, CoordGrid(3743, 5662))
        val machine = MotherlodeMachine(f.locs, f.npcs, random, f.collision)
        machine.tick()
        val delivery = DeliveryFixture(f, machine)
        delivery.enqueue()
        machine.spawnPayDirtFloater()
        val dirt = f.npcList.single { it.id == MotherlodeConstants.NPC_PAYDIRT_FLOATER.asRSCM() }
        val movement = NpcMovementProcessor(f.collision, StepFactory(f.collision), f.events)
        repeat(5) {
            f.events.publish(GameLifecycle.StartCycle)
            machine.tick()
            f.clock.tick()
            f.events.publish(GameLifecycle.LateCycle)
        }
        assertEquals(MotherlodeConstants.FLOAT_CHANNEL.first(), dirt.coords)
        assertEquals(0, delivery.stored())
        assertTrue(machine.repairStrut(CoordGrid(3742, 5663)))
        repeat(MotherlodeConstants.TRANSIT_TICKS) { elapsed ->
            f.events.publish(GameLifecycle.StartCycle)
            dirt.currentMapClock = f.clock.cycle
            machine.tick()
            if (dirt.isSlotAssigned) movement.process(dirt)
            f.clock.tick()
            f.events.publish(GameLifecycle.LateCycle)
            assertEquals(if (dirt.isSlotAssigned) 0 else 1, delivery.stored(), "Travel tick ${elapsed + 1}, dirt at ${dirt.coords}")
        }
        assertEquals(1, delivery.stored())
        assertEquals(15, delivery.player.statMap.getXP("stat.mining"))
    }

    private class DeliveryFixture(f: Fixture, machine: MotherlodeMachine) {
        val player = Player()
        private val clock = DeliveryClock()
        private val registry = DeliveryLineRegistry()
        private val manager = DeliveryManager(registry, clock)
        private val line = MotherlodeDelivery.PAY_DIRT_LINE

        init {
            MotherlodeDelivery.waterStopped = { !machine.flowedThisTick }
            MotherlodeDelivery.currentCycle = { f.clock.cycle }
            registry.register(line)
            val players = PlayerList().apply { this[1] = player }
            val hook = Class.forName("org.rsmod.api.delivery.hook.DeliveryTickScript")
                .getConstructor(DeliveryManager::class.java, DeliveryClock::class.java, PlayerList::class.java)
                .newInstance(manager, clock, players) as PluginScript
            with(hook) { f.context.startup() }
        }

        fun enqueue() { assertTrue(manager.enqueue(player, line, MotherlodeConstants.OBJ_COAL.asRSCM())) }
        fun stored() = manager.storedCount(player, line)
    }

    @Test fun `upper floor is the raised plane zero ledge rather than wheel platform`() {
        assertTrue(CoordGrid(3755, 5675).isMotherlodeUpperFloor())
        assertTrue(CoordGrid(3763, 5656).isMotherlodeUpperFloor())
        assertTrue(CoordGrid(3737, 5682).isMotherlodeUpperFloor())
        assertFalse(CoordGrid(3755, 5673).isMotherlodeUpperFloor())
        assertFalse(CoordGrid(3744, 5663).isMotherlodeUpperFloor())
        assertFalse(CoordGrid(3753, 5686).isMotherlodeUpperFloor())
        assertFalse(CoordGrid(3755, 5675, 1).isMotherlodeUpperFloor())
    }

    companion object {
        @JvmStatic @BeforeAll fun cache() { ServerCacheManager.init(241).close() }
    }
}
