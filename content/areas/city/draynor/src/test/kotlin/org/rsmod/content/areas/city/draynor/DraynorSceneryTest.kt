package org.rsmod.content.areas.city.draynor

import dev.openrune.ServerCacheManager
import dev.openrune.cache.MAPS
import dev.openrune.map.loc.MapLocDefinition
import dev.openrune.map.loc.MapLocListDecoder
import dev.openrune.map.util.InlineByteBuf
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.types.MoveRestrict
import kotlin.coroutines.Continuation
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.coroutines.startCoroutine
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.junit.jupiter.api.parallel.ResourceLock
import org.rsmod.annotations.InternalApi
import org.rsmod.api.config.refs.params
import org.rsmod.api.game.process.npc.NpcMovementProcessor
import org.rsmod.api.game.process.npc.hunt.NpcPlayerHuntProcessor
import org.rsmod.api.hunt.Hunt
import org.rsmod.api.player.events.interact.LocContentEvents
import org.rsmod.api.player.events.interact.NpcEvents
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.protect.ProtectedAccessContext
import org.rsmod.api.random.DefaultGameRandom
import org.rsmod.api.random.GameRandom
import org.rsmod.api.registry.controller.ControllerRegistry
import org.rsmod.api.registry.loc.LocRegistry
import org.rsmod.api.registry.loc.LocRegistryNormal
import org.rsmod.api.registry.loc.LocRegistryRegion
import org.rsmod.api.registry.npc.NpcRegistry
import org.rsmod.api.registry.obj.ObjRegistry
import org.rsmod.api.registry.player.PlayerRegistry
import org.rsmod.api.registry.region.RegionRegistry
import org.rsmod.api.registry.zone.ZonePlayerActivityBitSet
import org.rsmod.api.registry.zone.ZoneUpdateMap
import org.rsmod.api.repo.loc.LocRepository
import org.rsmod.api.route.RayCastValidator
import org.rsmod.api.route.StepFactory
import org.rsmod.content.generic.locs.doors.DoorScript
import org.rsmod.coroutine.GameCoroutine
import org.rsmod.events.EventBus
import org.rsmod.game.MapClock
import org.rsmod.game.cheat.CheatCommandMap
import org.rsmod.game.entity.ControllerList
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.NpcList
import org.rsmod.game.entity.Player
import org.rsmod.game.entity.PlayerList
import org.rsmod.game.entity.npc.NoopNpcInfo
import org.rsmod.game.entity.npc.NpcInfoProtocol
import org.rsmod.game.entity.player.PlayerUid
import org.rsmod.game.loc.BoundLocInfo
import org.rsmod.game.loc.LocEntity
import org.rsmod.game.loc.LocInfo
import org.rsmod.game.map.LocZoneStorage
import org.rsmod.game.queue.EngineQueueCache
import org.rsmod.game.region.RegionListLarge
import org.rsmod.game.region.RegionListSmall
import org.rsmod.map.CoordGrid
import org.rsmod.map.square.MapSquareKey
import org.rsmod.map.zone.ZoneKey
import org.rsmod.plugin.scripts.ScriptContext
import org.rsmod.routefinder.StepValidator
import org.rsmod.routefinder.collision.CollisionFlagMap
import org.rsmod.routefinder.flag.CollisionFlag

@Execution(ExecutionMode.SAME_THREAD)
@ResourceLock("ServerCacheManager")
@OptIn(InternalApi::class)
class DraynorSceneryTest {
    @Test
    fun nativeDoorsToggleAndRestoreCollisionOnEveryFloor() {
        assertEquals(11, doors.size)
        assertEquals(setOf(0, 1, 2), doors.map { it.coords.level }.toSet())
        for (door in doors) {
            val fixture = Fixture()
            val type = checkNotNull(ServerCacheManager.getObject(door.id))
            fixture.locs.add(LocInfo(door.layer, door.coords, door.entity), Int.MAX_VALUE)
            val offset = listOf(-1 to 0, 0 to 1, 1 to 0, 0 to -1)[door.angle.id]
            fun canCross() = StepValidator(fixture.collision).canTravel(
                level = door.coords.level, x = door.coords.x, z = door.coords.z,
                offsetX = offset.first, offsetZ = offset.second,
            )
            assertFalse(canCross(), "$door initially blocks its wall edge")
            fixture.operate(door)
            assertTrue(canCross(), "$door releases its former wall edge")
            val partnerId = type.param(params.next_loc_stage).id
            val changed = (-1..1).asSequence().flatMap { x ->
                (-1..1).asSequence().flatMap { z ->
                    fixture.locs.findAll(ZoneKey.from(door.coords).translate(x, z))
                }
            }.single { it.id == partnerId }
            val partner = BoundLocInfo(changed.coords, changed.entity, changed.layer, 1, 1, 0)
            fixture.operate(partner)
            val restored = fixture.locs.findAll(door.coords).single { it.id == door.id }
            assertEquals(door.entity, restored.entity)
            assertFalse(canCross(), "$door restores its wall edge")
        }
    }

    @Test
    fun treesRemainRootedEvenWithQueuedMovement() {
        val fixture = Fixture()
        val tree = fixture.tree()
        val start = tree.coords
        assertEquals(MoveRestrict.NoMove, tree.type.moveRestrict)
        assertEquals("seq.deadtree_attack".asRSCM(), tree.type.param(params.attack_anim).id)
        assertEquals("synth.deadtree_attack".asRSCM(), tree.type.param(params.attack_sound).id)
        tree.walk(start.translate(3, 0))
        val movement = NpcMovementProcessor(fixture.collision, StepFactory(fixture.collision), fixture.events)
        repeat(8) { movement.process(tree) }
        assertEquals(start, tree.coords)
        assertTrue(tree.routeDestination.isEmpty())
        assertFalse(tree.type.giveChase)
    }

    @Test
    fun treesTargetAdjacentPlayersButNotDistantHiddenOrUpstairsPlayers() {
        for (scenario in 0..3) {
            val fixture = Fixture()
            val tree = fixture.tree()
            val coords = when (scenario) {
                1 -> tree.coords.translate(2, 0)
                3 -> CoordGrid(tree.coords.x + 1, tree.coords.z, 1)
                else -> tree.coords.translate(1, 0)
            }
            val player = fixture.player(coords).apply { hidden = scenario == 2 }
            fixture.hunting.process(tree)
            assertEquals(if (scenario == 0) player.uid else PlayerUid.NULL, tree.huntPlayer, "scenario $scenario")
        }
    }

    @Test
    fun treesDoNotTargetThroughWalls() {
        val fixture = Fixture()
        val tree = fixture.tree()
        val player = fixture.player(tree.coords.translate(1, 0))
        fixture.collision.add(player.coords.x, player.coords.z, 0, CollisionFlag.WALL_WEST_PROJ_BLOCKER)
        fixture.hunting.process(tree)
        assertEquals(PlayerUid.NULL, tree.huntPlayer)
    }

    @Test
    fun swipesCannotStartPlayerAutoRetaliationAgainstTreeVariants() {
        val fixture = Fixture()
        for (name in listOf("nasty_tree", "nasty_tree_unchoppable", "nasty_tree_choppable")) {
            assertTrue(fixture.events.contains(NpcEvents.Op2::class.java, "npc.$name".asRSCM()))
            assertTrue(fixture.events.contains(NpcEvents.Ap2::class.java, "npc.$name".asRSCM()))
        }
    }

    private class Fixture {
        val collision = CollisionFlagMap()
        val events = EventBus()
        private val clock = MapClock().apply { cycle = 100 }
        private val updates = ZoneUpdateMap()
        private val zones = LocZoneStorage()
        private val activity = ZonePlayerActivityBitSet()
        private val npcs = NpcRegistry(NpcList(), collision, events)
        private val players = PlayerRegistry(PlayerList(), collision, activity, events)
        private val normal = LocRegistryNormal(updates, collision, zones)
        private val regions = RegionRegistry(
            RegionListSmall(), RegionListLarge(), normal, collision, zones, npcs,
            ControllerRegistry(clock, ControllerList()), activity,
        )
        private val locRegistry = LocRegistry(zones, normal, LocRegistryRegion(updates, collision, zones, regions))
        val locs = LocRepository(clock, locRegistry, regions)
        private val random = object : GameRandom by DefaultGameRandom(1) {
            override fun of(minInclusive: Int, maxInclusive: Int) = minInclusive
        }
        val hunting = NpcPlayerHuntProcessor(
            random, clock, Hunt(RayCastValidator(collision), players, npcs, ObjRegistry(updates), locRegistry),
        )

        init {
            for (level in 0..2) for (x in 3080..3135 step 8) for (z in 3336..3383 step 8) {
                collision.allocateIfAbsent(x, z, level)
            }
            val context = ScriptContext(events, CheatCommandMap(), EngineQueueCache())
            with(DoorScript(locs)) { context.startup() }
            with(DraynorTrees()) { context.startup() }
        }

        fun tree() = Npc("npc.nasty_tree", CoordGrid(3110, 3347)).also {
            npcs.add(it)
            it.infoProtocol = object : NpcInfoProtocol by NoopNpcInfo {
                override fun isActive() = true
            }
        }

        fun player(at: CoordGrid) = Player().apply {
            coords = at
            slotId = checkNotNull(players.nextFreeSlot())
            uuid = slotId.toLong()
            players.add(this)
            players.change(this, ZoneKey.NULL, ZoneKey.from(at))
        }

        fun operate(door: BoundLocInfo) {
            val player = player(door.coords)
            val context = ProtectedAccessContext(
                getRandom = { random },
                getEventBus = { events },
                getNpcList = { error("Unexpected NPC access") },
                getPlayerList = { error("Unexpected player list access") },
                getCollision = { collision },
                getAreaChecker = { error("Unexpected area access") },
                getAlignment = { error("Unexpected dialogue access") },
                getLocInteractions = { error("Unexpected location interaction") },
                getNpcInteractions = { error("Unexpected NPC interaction") },
                getPlayerInteractions = { error("Unexpected player interaction") },
                getHeldInteractions = { error("Unexpected held interaction") },
                getWornInteractions = { error("Unexpected worn interaction") },
                getMusicPlayer = { error("Unexpected music player access") },
                getMarketPrices = { error("Unexpected market access") },
                getInstantHitProcessor = { error("Unexpected hit processing") },
                getTeleportValidator = { error("Unexpected teleport") },
                getHitModifier = { error("Unexpected hit modification") },
            )
            val access = ProtectedAccess(player, GameCoroutine(), context)
            var outcome: Result<Unit>? = null
            val action: suspend () -> Unit = {
                val type = checkNotNull(ServerCacheManager.getObject(door.id))
                assertTrue(events.publish(access, LocContentEvents.Op1(door, door, type, type.contentGroup)))
            }
            action.startCoroutine(object : Continuation<Unit> {
                override val context = EmptyCoroutineContext
                override fun resumeWith(result: Result<Unit>) { outcome = result }
            })
            checkNotNull(outcome) { "Door interaction unexpectedly suspended" }.getOrThrow()
        }
    }

    companion object {
        private lateinit var doors: List<BoundLocInfo>

        @JvmStatic
        @BeforeAll
        fun loadCache() {
            val cache = ServerCacheManager.init(240)
            try {
                val square = MapSquareKey.from(CoordGrid(3108, 3353))
                val map = MapLocListDecoder.decode(InlineByteBuf(checkNotNull(cache.data(MAPS, square.id, 1))))
                val ids = setOf("loc.draynor_panelled_door".asRSCM(), "loc.draynor_panelled_door_open".asRSCM())
                doors = map.spawns.map(::MapLocDefinition).filter { it.id in ids }.map {
                    BoundLocInfo(
                        square.toCoords(it.level).translate(it.localX, it.localZ),
                        LocEntity(it.id, it.shape, it.angle), 0, 1, 1, 0,
                    )
                }
            } finally {
                cache.close()
            }
        }
    }
}
