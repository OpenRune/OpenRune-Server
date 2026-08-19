package dev.openrune.map

import dev.openrune.ServerCacheManager
import dev.openrune.cache.MAPS
import dev.openrune.filesystem.Cache
import dev.openrune.map.area.MapAreaDecoder
import dev.openrune.map.area.MapAreaDefinition
import dev.openrune.map.area.MapAreaDefinitions
import dev.openrune.map.loc.MapLocDefinition
import dev.openrune.map.loc.MapLocListDecoder
import dev.openrune.map.loc.MapLocListDefinition
import dev.openrune.map.npc.MapNpcDefinition
import dev.openrune.map.npc.MapNpcListDecoder
import dev.openrune.map.npc.MapNpcListDefinition
import dev.openrune.map.obj.MapObjDefinition
import dev.openrune.map.obj.MapObjListDecoder
import dev.openrune.map.obj.MapObjListDefinition
import dev.openrune.map.tile.MapTileDecoder
import dev.openrune.map.tile.MapTileSimpleDefinition
import dev.openrune.map.util.InlineByteBuf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.runBlocking
import org.rsmod.game.loc.LocEntity
import org.rsmod.game.map.collision.toggleLoc
import org.rsmod.map.CoordGrid
import org.rsmod.map.square.MapSquareGrid
import org.rsmod.map.square.MapSquareKey
import org.rsmod.map.zone.ZoneGrid
import org.rsmod.map.zone.ZoneKey
import org.rsmod.routefinder.collision.CollisionFlagMap
import org.rsmod.routefinder.flag.CollisionFlag
import org.rsmod.routefinder.loc.LocLayerConstants

object GameMapDecoder {

    /**
     * The plane carrying the LINK_BELOW ("bridge") tile flag. It is authored on plane 1 only and
     * governs every plane: the client reads `tileSettings[1][x][z]` regardless of which plane it is
     * resolving.
     */
    private const val BRIDGE_FLAG_LEVEL = 1

    /**
     * Resolves the plane that content authored on [level] at ([x], [z]) actually occupies.
     *
     * A tile flagged LINK_BELOW draws - and therefore collides - one plane lower than authored.
     * That is what puts you on top of a bridge deck instead of in the river beneath it, and what
     * lets a staircase span from ground level up onto a raised platform.
     *
     * Returns `-1` when the result falls below ground. That is the filler underneath a bridge,
     * which has nowhere to go, so callers must skip it rather than write it: [CollisionFlagMap]
     * masks the plane with `(level and 0x3)`, so a negative plane silently wraps to 3.
     *
     * [putMaps] and [putLocs] must resolve identically. If they disagree, a square's tile collision
     * and its loc collision land on different planes.
     */
    private fun bridgeLevel(mapDef: MapTileSimpleDefinition, x: Int, z: Int, level: Int): Int {
        val flags = mapDef[x, z, BRIDGE_FLAG_LEVEL].toInt()
        val bridged = (flags and MapTileSimpleDefinition.LINK_BELOW) != 0
        return if (bridged) level - 1 else level
    }

    public fun decodeAll(spawnSink: GameMapSpawnSink, cache: Cache): Unit =
        runBlocking(Dispatchers.Default) {
            val mapBuffers = cache.readMapBuffers()
            val decodedMaps = decodeAll(mapBuffers)
            putMapCollision(decodedMaps)
            putAreas(decodedMaps)

            val mapBuilder = GameMapBuilder()
            putSpawns(mapBuilder, decodedMaps, spawnSink)
            cacheLocs(mapBuilder)
        }

    private fun Cache.readMapBuffers(): List<MapBuffer> =
        archives(MAPS)
            .asSequence()
            .filter { it <= 25286 }
            .mapNotNull { groupId ->
                val mapData = data(MAPS, groupId, 0) ?: return@mapNotNull null
                val locData = data(MAPS, groupId, 1) ?: return@mapNotNull null

                val x = groupId shr 8
                val z = groupId and 0xFF

                MapBuffer(
                    key = MapSquareKey(x, z),
                    map = InlineByteBuf(mapData),
                    locs = InlineByteBuf(locData),
                    npcs = data(MAPS, groupId, 5)?.let(::InlineByteBuf),
                    objs = data(MAPS, groupId, 6)?.let(::InlineByteBuf),
                    areas = data(MAPS, groupId, 7)?.let(::InlineByteBuf),
                )
            }
            .toList()

    private suspend fun decodeAll(buffers: List<MapBuffer>): List<DecodedMap> = coroutineScope {
        buffers.map { buffer -> async { buffer.decode() } }.awaitAll()
    }

    private suspend fun putMapCollision(maps: List<DecodedMap>): Unit = coroutineScope {
        maps
            .map { decoded -> async { putMaps(MapSingletons.collision, decoded.key, decoded.map) } }
            .awaitAll()
    }

    private fun putAreas(maps: List<DecodedMap>) {
        for (map in maps) {
            val areas = map.areas ?: continue
            MapAreaDefinitions.putAreas(MapSingletons.areaIndex, map.key, areas)
        }
    }

    private suspend fun putSpawns(
        builder: GameMapBuilder,
        decodedMaps: List<DecodedMap>,
        spawnSink: GameMapSpawnSink,
    ): Unit = coroutineScope {
        decodedMaps
            .map { decoded ->
                async {
                    putLocs(
                        builder,
                        MapSingletons.collision,
                        decoded.key,
                        decoded.map,
                        decoded.locs,
                    )
                }
            }
            .awaitAll()
        decodedMaps
            .map { decoded ->
                async {
                    decoded.npcs?.let { putNpcs(decoded.key, it, spawnSink) }
                    decoded.objs?.let { putObjs(decoded.key, it, spawnSink) }
                }
            }
            .awaitAll()
    }

    private fun cacheLocs(builder: GameMapBuilder) {
        for ((zoneKey, zoneBuilder) in builder.zoneBuilders) {
            MapSingletons.locZones.mapLocs[zoneKey] = zoneBuilder.build()
        }
    }

    public fun putMaps(
        collision: CollisionFlagMap,
        square: MapSquareKey,
        mapDef: MapTileSimpleDefinition,
    ) {
        val baseX = square.x * MapSquareGrid.LENGTH
        val baseZ = square.z * MapSquareGrid.LENGTH
        for (level in 0 until CoordGrid.LEVEL_COUNT) {
            for (x in 0 until MapSquareGrid.LENGTH) {
                for (z in 0 until MapSquareGrid.LENGTH) {
                    val flags = mapDef[x, z, level].toInt()
                    if (flags == 0) {
                        continue
                    }

                    var mask = 0
                    if ((flags and MapTileSimpleDefinition.BLOCK_MAP_SQUARE) != 0) {
                        mask = mask or CollisionFlag.BLOCK_WALK
                    }
                    if ((flags and MapTileSimpleDefinition.REMOVE_ROOFS) != 0) {
                        mask = mask or CollisionFlag.ROOF
                    }

                    val absX = baseX + x
                    val absZ = baseZ + z
                    val resolvedLevel = bridgeLevel(mapDef, x, z, level)
                    if (resolvedLevel < 0) {
                        continue
                    }
                    collision[absX, absZ, resolvedLevel] = mask
                }
            }
        }
    }

    public fun putLocs(
        mapBuilder: GameMapBuilder,
        collision: CollisionFlagMap,
        square: MapSquareKey,
        mapDef: MapTileSimpleDefinition,
        locDef: MapLocListDefinition,
    ) {
        with(mapBuilder) {
            for (packedLoc in locDef.spawns.longIterator()) {
                val loc = MapLocDefinition(packedLoc)
                val visualLevel = bridgeLevel(mapDef, loc.localX, loc.localZ, loc.level)
                if (visualLevel < 0) {
                    continue
                }
                val coords = square.toCoords(visualLevel).translate(loc.localX, loc.localZ)
                val zoneGridX = coords.x and ZoneGrid.X_BIT_MASK
                val zoneGridZ = coords.z and ZoneGrid.Z_BIT_MASK
                val zone = computeIfAbsent(ZoneKey.from(coords)) { ZoneBuilder() }
                val layer = LocLayerConstants.of(loc.shape)
                val entity = LocEntity(loc.id, loc.shape, loc.angle)
                val type =
                    ServerCacheManager.getObject(loc.id)
                        ?: error("Invalid loc type: $loc ($square)")
                zone.add(zoneGridX, zoneGridZ, layer, entity)
                collision.toggleLoc(
                    coords = coords,
                    width = type.width,
                    length = type.length,
                    shape = loc.shape,
                    angle = loc.angle,
                    blockWalk = type.blockWalk,
                    blockRange = type.blockRange,
                    breakRouteFinding = type.breakRouteFinding,
                    add = true,
                )
            }
        }
    }

    public fun putNpcs(
        square: MapSquareKey,
        npcs: MapNpcListDefinition,
        spawnSink: GameMapSpawnSink,
    ) {
        val base = square.toCoords(level = 0)
        for (packed in npcs.packedSpawns.intIterator()) {
            val def = MapNpcDefinition(packed)
            val coords = base.translate(def.localX, def.localZ, def.level)
            spawnSink.onNpcSpawn(def, coords)
        }
    }

    public fun putObjs(
        square: MapSquareKey,
        objs: MapObjListDefinition,
        spawnSink: GameMapSpawnSink,
    ) {
        val base = square.toCoords(level = 0)
        for (packed in objs.packedSpawns.longIterator()) {
            val def = MapObjDefinition(packed)
            val coords = base.translate(def.localX, def.localZ, def.level)
            spawnSink.onObjSpawn(def, coords)
        }
    }
}

private class MapBuffer(
    val key: MapSquareKey,
    val map: InlineByteBuf,
    val locs: InlineByteBuf,
    val npcs: InlineByteBuf?,
    val objs: InlineByteBuf?,
    val areas: InlineByteBuf?,
) {
    fun decode(): DecodedMap {
        return DecodedMap(
            key = key,
            map = MapTileDecoder.decode(map),
            locs = MapLocListDecoder.decode(locs),
            npcs = npcs?.let(MapNpcListDecoder::decode),
            objs = objs?.let(MapObjListDecoder::decode),
            areas = areas?.let(MapAreaDecoder::decode),
        )
    }
}

private data class DecodedMap(
    val key: MapSquareKey,
    val map: MapTileSimpleDefinition,
    val locs: MapLocListDefinition,
    val npcs: MapNpcListDefinition?,
    val objs: MapObjListDefinition?,
    val areas: MapAreaDefinition?,
)
