package dev.openrune.map.packing

import dev.openrune.filesystem.Cache
import dev.openrune.map.obj.MapObjDefinition
import dev.openrune.map.obj.MapObjListDefinition
import dev.openrune.map.obj.MapObjListEncoder
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.toml.decode
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap
import it.unimi.dsi.fastutil.longs.LongArrayList
import java.nio.file.Path
import org.rsmod.map.square.MapSquareGrid
import org.rsmod.map.square.MapSquareKey

public object MapObjPacker : MapSpawnPackerBase<MapObjListDefinition>() {

    override val resourceDir: Path = Path.of("../.data/raw-cache/map/objs")

    public fun encodeCacheMapObj(cache: Cache) {
        MapObjListEncoder.encodeAll(cache, loadAndCollect())
    }

    override fun decodeTomlSpawnFile(path: Path): List<Pair<MapSquareKey, MapObjListDefinition>> {
        val parsed = toml.decode<TomlObjSpawnFile>(path)

        val grouped = Int2ObjectOpenHashMap<LongArrayList>()
        for (spawn in parsed.spawn) {
            val obj = spawn.obj.asRSCM()
            val coords = parseCoordGrid(spawn.coords)
            val grid = MapSquareGrid.from(coords)
            val def =
                MapObjDefinition(
                    id = obj,
                    count = spawn.count,
                    localX = grid.x,
                    localZ = grid.z,
                    level = grid.level,
                )
            val mapSquare = MapSquareKey.from(coords)
            val spawnList = grouped.computeIfAbsent(mapSquare.id) { LongArrayList() }
            spawnList.add(def.packed)
        }

        return grouped.map { Pair(MapSquareKey(it.key), MapObjListDefinition(it.value)) }
    }

    override fun mergeDefinitions(
        old: MapObjListDefinition,
        new: MapObjListDefinition,
    ): MapObjListDefinition = MapObjListDefinition.merge(old, new)

    private data class TomlObjSpawnFile(val spawn: List<TomlObjSpawn>)

    private data class TomlObjSpawn(val obj: String, val count: Int = 1, val coords: String)
}
