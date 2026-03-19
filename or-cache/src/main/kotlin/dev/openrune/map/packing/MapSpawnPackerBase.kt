package dev.openrune.map.packing

import dev.openrune.toml.TomlMapper
import dev.openrune.toml.tomlMapper
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.extension
import kotlin.io.path.isRegularFile
import org.rsmod.map.CoordGrid
import org.rsmod.map.square.MapSquareKey

public abstract class MapSpawnPackerBase<D : Any> {

    protected abstract val resourceDir: Path

    public val toml: TomlMapper = tomlMapper {}

    public fun listTomlFiles(dir: Path): List<Path> {
        Files.list(dir).use { paths ->
            return paths.filter { it.isRegularFile() && it.extension == "toml" }.sorted().toList()
        }
    }

    public fun parseCoordGrid(value: String): CoordGrid {
        val split = value.split('_')
        if (split.size != 5) {
            throw IOException("CoordGrid must contain 5 values separated by '_'. (ex: 0_50_50_0_0)")
        }

        val level = split[0].toInt()
        val mx = split[1].toInt()
        val mz = split[2].toInt()
        val lx = split[3].toInt()
        val lz = split[4].toInt()
        return CoordGrid(level, mx, mz, lx, lz)
    }

    protected abstract fun decodeTomlSpawnFile(path: Path): List<Pair<MapSquareKey, D>>

    protected fun loadAndCollect(): Map<MapSquareKey, D> {
        val files = listTomlFiles(resourceDir)
        val resources = files.flatMap(::decodeTomlSpawnFile)
        return resources.mergeToMap()
    }

    private fun List<Pair<MapSquareKey, D>>.mergeToMap(): Map<MapSquareKey, D> {
        val merged = mutableMapOf<MapSquareKey, D>()
        for ((mapSquare, def) in this) {
            merged.merge(mapSquare, def, ::mergeDefinitions)
        }
        return merged
    }

    protected abstract fun mergeDefinitions(old: D, new: D): D
}
