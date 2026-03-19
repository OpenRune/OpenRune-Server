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
import kotlin.collections.component1
import kotlin.collections.component2
import kotlin.collections.iterator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.runBlocking
import org.rsmod.game.loc.LocEntity
import org.rsmod.game.map.collision.toggleLoc
import org.rsmod.game.map.xtea.XteaMap
import org.rsmod.map.CoordGrid
import org.rsmod.map.square.MapSquareGrid
import org.rsmod.map.square.MapSquareKey
import org.rsmod.map.zone.ZoneGrid
import org.rsmod.map.zone.ZoneKey
import org.rsmod.routefinder.collision.CollisionFlagMap
import org.rsmod.routefinder.flag.CollisionFlag
import org.rsmod.routefinder.loc.LocLayerConstants

object GameMapDecoder {

    public fun decodeAll(spawnSink: GameMapSpawnSink, cache: Cache, map: XteaMap): Unit =
        runBlocking(Dispatchers.IO) {
            val mapBuffers = cache.readMapBuffers(map)
            val decodedMaps = decodeAll(mapBuffers)

            putMapCollision(decodedMaps)
            putAreas(decodedMaps)

            val mapBuilder = GameMapBuilder()
            putSpawns(mapBuilder, decodedMaps, spawnSink)
            cacheLocs(mapBuilder)
        }

    private fun Cache.readMapBuffers(xteaMap: XteaMap): List<MapBuffer> =
        xteaMap.map { (mapSquareKey, keyArray) ->
            val name = "${mapSquareKey.x}_${mapSquareKey.z}"
            val map = InlineByteBuf(data(MAPS, "m$name")!!)
            val locs = InlineByteBuf(data(MAPS, "l$name", keyArray)!!)
            val npcs = data(MAPS, "n$name")?.let(::InlineByteBuf)
            val objs = data(MAPS, "o$name")?.let(::InlineByteBuf)
            val areas = data(MAPS, "a$name")?.let(::InlineByteBuf)
            MapBuffer(mapSquareKey, map, locs, npcs, objs, areas)
        }

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

    private fun putSpawns(
        builder: GameMapBuilder,
        decodedMaps: List<DecodedMap>,
        spawnSink: GameMapSpawnSink,
    ) {
        for (decoded in decodedMaps) {
            putLocs(builder, MapSingletons.collision, decoded.key, decoded.map, decoded.locs)
            decoded.npcs?.let { putNpcs(decoded.key, it, spawnSink) }
            decoded.objs?.let { putObjs(decoded.key, it, spawnSink) }
        }
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
                    val resolvedLevel =
                        if ((flags and MapTileSimpleDefinition.LINK_BELOW) != 0) {
                            level - 1
                        } else {
                            level
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
                val local = MapSquareGrid(loc.localX, loc.localZ, loc.level)
                val tileFlags = mapDef[local.x, local.z, local.level].toInt()
                val tileAboveFlags =
                    if (local.level >= CoordGrid.LEVEL_COUNT - 1) {
                        tileFlags
                    } else {
                        mapDef[local.x, local.z, local.level + 1].toInt()
                    }
                val resolvedTileFlags =
                    if ((tileAboveFlags and MapTileSimpleDefinition.LINK_BELOW) != 0) {
                        tileAboveFlags
                    } else {
                        tileFlags
                    }
                // Take into account that any tile that has this bit flag will cause locs below
                // it to "visually" go one level down.
                val visualLevel =
                    if ((resolvedTileFlags and MapTileSimpleDefinition.LINK_BELOW) != 0) {
                        loc.level - 1
                    } else {
                        loc.level
                    }
                if (visualLevel < 0) {
                    continue
                }
                val coords =
                    square.toCoords(0.coerceAtLeast(visualLevel)).translate(loc.localX, loc.localZ)
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
    suspend fun decode(): DecodedMap = coroutineScope {
        val mapDef = async { MapTileDecoder.decode(map) }
        val locDef = async { MapLocListDecoder.decode(locs) }
        val npcDef = if (npcs != null) async { MapNpcListDecoder.decode(npcs) } else null
        val objDef = if (objs != null) async { MapObjListDecoder.decode(objs) } else null
        val areaDef = if (areas != null) async { MapAreaDecoder.decode(areas) } else null
        DecodedMap(
            key = key,
            map = mapDef.await(),
            locs = locDef.await(),
            npcs = npcDef?.await(),
            objs = objDef?.await(),
            areas = areaDef?.await(),
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
