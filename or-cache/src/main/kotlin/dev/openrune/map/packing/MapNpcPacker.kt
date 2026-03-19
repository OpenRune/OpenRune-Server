package dev.openrune.map.packing

import dev.openrune.filesystem.Cache
import dev.openrune.map.npc.MapNpcDefinition
import dev.openrune.map.npc.MapNpcListDefinition
import dev.openrune.map.npc.MapNpcListEncoder
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.toml.decode
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap
import it.unimi.dsi.fastutil.ints.IntArrayList
import java.nio.file.Path
import org.rsmod.map.square.MapSquareGrid
import org.rsmod.map.square.MapSquareKey

public object MapNpcPacker : MapSpawnPackerBase<MapNpcListDefinition>() {
    override val resourceDir: Path = Path.of("../.data/raw-cache/map/npcs")

    public fun encodeCacheMapNpc(cache: Cache) {
        MapNpcListEncoder.encodeAll(cache, loadAndCollect())
    }

    override fun decodeTomlSpawnFile(path: Path): List<Pair<MapSquareKey, MapNpcListDefinition>> {
        val parsed = toml.decode<TomlNpcSpawnFile>(path)

        val grouped = Int2ObjectOpenHashMap<IntArrayList>()
        for (spawn in parsed.spawn) {
            val npc = spawn.npc.asRSCM()
            val coords = parseCoordGrid(spawn.coords)
            val grid = MapSquareGrid.from(coords)
            val def = MapNpcDefinition(npc, localX = grid.x, localZ = grid.z, level = grid.level)
            val mapSquare = MapSquareKey.from(coords)
            val spawnList = grouped.computeIfAbsent(mapSquare.id) { IntArrayList() }
            spawnList.add(def.packed)
        }
        return grouped.map { Pair(MapSquareKey(it.key), MapNpcListDefinition(it.value)) }
    }

    override fun mergeDefinitions(
        old: MapNpcListDefinition,
        new: MapNpcListDefinition,
    ): MapNpcListDefinition = MapNpcListDefinition.merge(old, new)

    private data class TomlNpcSpawnFile(val spawn: List<TomlNpcSpawn>)

    private data class TomlNpcSpawn(val npc: String, val coords: String)
}
