@file:OptIn(org.rsmod.annotations.InternalApi::class, dev.openrune.types.util.UncheckedType::class)

package org.rsmod.content.quest.area.asgarnia.blackknights

import dev.openrune.ServerCacheManager
import dev.openrune.cache.MAPS
import dev.openrune.map.GameMapBuilder
import dev.openrune.map.GameMapDecoder
import dev.openrune.map.loc.MapLocListDecoder
import dev.openrune.map.npc.MapNpcDefinition
import dev.openrune.map.npc.MapNpcListDecoder
import dev.openrune.map.tile.MapTileDecoder
import dev.openrune.map.util.InlineByteBuf
import dev.openrune.rscm.RSCM.asRSCM
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.*
import org.rsmod.api.player.events.interact.LocContentEvents
import org.rsmod.api.player.events.interact.LocEvents
import org.rsmod.api.player.protect.ProtectedAccessContextFactory
import org.rsmod.api.player.protect.ProtectedAccessLauncher
import org.rsmod.api.registry.loc.LocRegistry
import org.rsmod.api.registry.loc.LocRegistryNormal
import org.rsmod.api.registry.zone.ZoneUpdateMap
import org.rsmod.api.repo.loc.LocRepository
import org.rsmod.content.generic.locs.doors.DoorScript
import org.rsmod.content.generic.locs.doors.DoubleDoorScript
import org.rsmod.content.generic.locs.staircase.SpiralStaircaseScript
import org.rsmod.events.EventBus
import org.rsmod.game.MapClock
import org.rsmod.game.cheat.CheatCommandMap
import org.rsmod.game.client.Client
import org.rsmod.game.entity.Player
import org.rsmod.game.inv.Inventory
import org.rsmod.game.loc.BoundLocInfo
import org.rsmod.game.loc.LocEntity
import org.rsmod.game.loc.LocInfo
import org.rsmod.game.map.LocZoneStorage
import org.rsmod.game.queue.EngineQueueCache
import org.rsmod.map.CoordGrid
import org.rsmod.map.square.MapSquareKey
import org.rsmod.plugin.scripts.ScriptContext
import org.rsmod.routefinder.RouteFinding
import org.rsmod.routefinder.collision.CollisionFlagMap
import org.rsmod.routefinder.flag.CollisionFlag

private val BoundLocInfo.type
    get() = checkNotNull(ServerCacheManager.getObject(id))

internal class BlackKnightsMapFixture(
    cache: dev.openrune.filesystem.Cache,
    centre: CoordGrid,
    radius: Int = 17,
    npcRadius: Int = 12,
    extraCentres: List<CoordGrid> = emptyList(),
) {
    val repository: LocRepository
    val clock = MapClock(100)
    val collision = CollisionFlagMap()
    val events = EventBus()
    val locs = mutableListOf<BoundLocInfo>()
    val npcs = mutableListOf<Pair<Int, CoordGrid>>()
    val player =
        Player().apply {
            client = mockk<Client<Any, Any>>(relaxed = true)
            currentMapClock = 100
            processedMapClock = 100
            inv =
                Inventory(
                    checkNotNull(ServerCacheManager.getInventory("inv.inv".asRSCM())),
                    arrayOfNulls(28),
                )
            worn =
                Inventory(
                    checkNotNull(ServerCacheManager.getInventory("inv.worn".asRSCM())),
                    arrayOfNulls(14),
                )
        }
    val context =
        ProtectedAccessContextFactory.empty()
            .copy(
                getEventBus = { events },
                getCollision = { collision },
                getAlignment = { org.rsmod.api.player.dialogue.align.TextAlignment() },
                getNpcInteractions = { org.rsmod.api.player.interact.NpcInteractions(events) },
                getTeleportValidator = {
                    org.rsmod.api.player.hook.PlayerTeleportValidator(emptySet())
                },
                getAreaChecker = { mockk(relaxed = true) },
            )

    init {
        val builder = GameMapBuilder()
        // Match production: every terrain square must load before any location collision.
        // Loading a later square's roof/tile mask can otherwise erase a neighbour wall edge.
        val decoded =
            mutableListOf<
                Triple<
                    MapSquareKey,
                    dev.openrune.map.tile.MapTileSimpleDefinition,
                    dev.openrune.map.loc.MapLocListDefinition,
                >
            >()
        val storage = LocZoneStorage()
        val centres = listOf(centre) + extraCentres
        val squares =
            centres
                .flatMap { c ->
                    ((c.x - 32) / 64..(c.x + 32) / 64).flatMap { mx ->
                        ((c.z - 32) / 64..(c.z + 32) / 64).map { mz ->
                            MapSquareKey.from(CoordGrid(mx * 64, mz * 64))
                        }
                    }
                }
                .distinct()
        for (square in squares) {
            val mx = square.toCoords(0).x / 64
            val mz = square.toCoords(0).z / 64
            val tileData = cache.data(MAPS, square.id, 0) ?: continue
            val tiles = MapTileDecoder.decode(InlineByteBuf(tileData))
            val nativeLocs =
                MapLocListDecoder.decode(
                    InlineByteBuf(checkNotNull(cache.data(MAPS, square.id, 1)))
                )
            for (level in 0..3) for (x in mx * 64 until mx * 64 + 64 step 8) for (z in
                mz * 64 until mz * 64 + 64 step 8) collision.allocateIfAbsent(x, z, level)
            GameMapDecoder.putMaps(collision, square, tiles)
            decoded += Triple(square, tiles, nativeLocs)
            cache.data(MAPS, square.id, 5)?.let { data ->
                for (npc in
                    MapNpcListDecoder.decode(InlineByteBuf(data))
                        .packedSpawns
                        .map(::MapNpcDefinition)) {
                    val at = square.toCoords(npc.level).translate(npc.localX, npc.localZ)
                    if (
                        centres.any {
                            kotlin.math.abs(at.x - it.x) <= npcRadius &&
                                kotlin.math.abs(at.z - it.z) <= npcRadius &&
                                at.level == it.level
                        }
                    )
                        npcs += npc.id to at
                }
            }
        }
        for ((square, tiles, nativeLocs) in decoded) GameMapDecoder.putLocs(
            builder,
            collision,
            square,
            tiles,
            nativeLocs,
        )
        for ((zone, data) in builder.zoneBuilders) {
            val packed = data.build()
            storage.mapLocs[zone] = packed
            val base = org.rsmod.map.zone.ZoneKey(zone).toCoords()
            for (entry in packed.byte2IntEntrySet()) {
                val key = org.rsmod.game.loc.LocZoneKey(entry.byteKey)
                val at = base.translate(key.x, key.z)
                if (
                    centres.none {
                        kotlin.math.abs(at.x - it.x) <= radius &&
                            kotlin.math.abs(at.z - it.z) <= radius
                    }
                )
                    continue
                val entity = LocEntity(entry.intValue)
                locs +=
                    BoundLocInfo(
                        LocInfo(key.layer, at, entity),
                        checkNotNull(ServerCacheManager.getObject(entity.id)),
                    )
            }
        }
        val normal = LocRegistryNormal(ZoneUpdateMap(), collision, storage)
        val repo = LocRepository(clock, LocRegistry(storage, normal, mockk()), mockk())
        repository = repo
        val scripts = ScriptContext(events, CheatCommandMap(), EngineQueueCache())
        with(DoorScript(repo)) { scripts.startup() }
        with(DoubleDoorScript(repo)) { scripts.startup() }
        with(SpiralStaircaseScript()) { scripts.startup() }
        with(org.rsmod.content.generic.locs.ladders.LadderScript()) { scripts.startup() }
        with(org.rsmod.content.generic.locs.staircase.StraightStaircaseScript()) {
            scripts.startup()
        }
    }

    fun find(symbol: String, at: CoordGrid) =
        locs.single { it.id == symbol.asRSCM() && it.coords == at }

    fun walkable(at: CoordGrid) =
        collision[at.x, at.z, at.level] and
            (CollisionFlag.BLOCK_WALK or CollisionFlag.LOC or CollisionFlag.GROUND_DECOR) == 0

    fun route(from: CoordGrid, to: CoordGrid) =
        RouteFinding(collision).findRoute(from.level, from.x, from.z, to.x, to.z, moveNear = false)

    fun connected(from: CoordGrid, to: CoordGrid) {
        assertEquals(from.level, to.level)
        assertTrue(walkable(from), "Blocked departure $from")
        assertTrue(walkable(to), "Blocked destination $to")
        assertTrue(route(from, to).success, "$from cannot reach $to")
    }

    fun approach(from: CoordGrid, loc: BoundLocInfo): CoordGrid {
        assertEquals(from.level, loc.level)
        assertTrue(walkable(from), "Blocked departure $from")
        val route =
            RouteFinding(collision)
                .findRoute(
                    from.level,
                    from.x,
                    from.z,
                    loc.x,
                    loc.z,
                    destWidth = loc.type.width,
                    destLength = loc.type.length,
                    locShape = loc.shape.id,
                    locAngle = loc.angle.id,
                    blockAccessFlags = loc.type.forceApproachFlags,
                    moveNear = false,
                )
        assertTrue(route.success, "$from cannot approach ${loc.type.internalName} at ${loc.coords}")
        val end = route.lastOrNull() ?: return from
        return CoordGrid(end.x, end.z, end.level).also { assertTrue(walkable(it)) }
    }

    fun operate(loc: BoundLocInfo, op: Int) {
        assertTrue(
            ProtectedAccessLauncher.withProtectedAccess(player, context) {
                val handled =
                    when (op) {
                        1 ->
                            events.publish(this, LocEvents.Op1(loc, loc, loc.type)) ||
                                events.publish(
                                    this,
                                    LocContentEvents.Op1(loc, loc, loc.type, loc.type.contentGroup),
                                )
                        2 ->
                            events.publish(this, LocEvents.Op2(loc, loc, loc.type)) ||
                                events.publish(
                                    this,
                                    LocContentEvents.Op2(loc, loc, loc.type, loc.type.contentGroup),
                                )
                        3 ->
                            events.publish(this, LocEvents.Op3(loc, loc, loc.type)) ||
                                events.publish(
                                    this,
                                    LocContentEvents.Op3(loc, loc, loc.type, loc.type.contentGroup),
                                )
                        else -> false
                    }
                assertTrue(handled, "Missing native op$op: ${loc.type.internalName}")
            }
        )
        player.advanceActiveCoroutine()
    }

    fun tick(count: Int) =
        repeat(count) {
            player.currentMapClock++
            player.processedMapClock = player.currentMapClock
            if (player.isNotDelayed) player.advanceActiveCoroutine()
        }
}
