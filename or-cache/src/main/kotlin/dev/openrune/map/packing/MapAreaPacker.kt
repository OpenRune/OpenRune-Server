package dev.openrune.map.packing

import dev.openrune.filesystem.Cache
import dev.openrune.map.area.MapAreaDefinition
import dev.openrune.map.area.MapAreaEncoder
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.toml.decode
import dev.openrune.toml.tomlMapper
import java.nio.file.Files
import java.nio.file.Path
import kotlin.collections.iterator
import kotlin.io.path.extension
import kotlin.io.path.isRegularFile
import org.rsmod.game.area.polygon.PolygonArea
import org.rsmod.game.area.polygon.PolygonMapSquareBuilder
import org.rsmod.game.area.util.PolygonMapSquareClipper
import org.rsmod.map.CoordGrid
import org.rsmod.map.square.MapSquareGrid
import org.rsmod.map.square.MapSquareKey

public object MapAreaPacker {

    private val toml = tomlMapper {}
    private val areaDir: Path = Path.of("../.data/raw-cache/map/area")

    private fun listTomlFiles(dir: Path): List<Path> {
        Files.list(dir).use { paths ->
            return paths.filter { it.isRegularFile() && it.extension == "toml" }.sorted().toList()
        }
    }

    public fun encodeCacheMapArea(cache: Cache) {
        val areas = loadAndCollect()
        MapAreaEncoder.encodeAll(cache, areas)
    }

    private fun loadAndCollect(): Map<MapSquareKey, MapAreaDefinition> {
        val files = listTomlFiles(areaDir)
        return files.flatMap(::loadAndCollect).mergeToMap()
    }

    private fun List<MapAreaEntry>.mergeToMap(): Map<MapSquareKey, MapAreaDefinition> {
        val merged = mutableMapOf<MapSquareKey, MapAreaDefinition>()
        for ((mapSquare, def) in this) {
            merged.merge(mapSquare, def, MapAreaDefinition.Companion::merge)
        }
        return merged
    }

    private fun loadAndCollect(path: Path): List<MapAreaEntry> {
        val mapAreas = loadMapAreas(path)
        val areas = collectAreas(mapAreas)
        return toAreaConfigList(areas, mapAreas)
    }

    private fun loadMapAreas(path: Path): List<TomlMapArea> {
        return toml.decode<TomlMapAreaFile>(path).area
    }

    private fun toAreaConfigList(
        polygonArea: PolygonArea,
        mapAreas: List<TomlMapArea>,
    ): List<MapAreaEntry> {
        val includesById =
            mapAreas.associate { area ->
                val id = area.area_id.asRSCM().toShort()
                val includes = area.includes.map { it.asRSCM().toShort() }.toShortArray()

                id to includes
            }

        val excludesById =
            mapAreas.associate { area ->
                val id = area.area_id.asRSCM().toShort()
                val excludes = area.excludes.map { it.asRSCM().toShort() }.toShortArray()

                id to excludes
            }

        return polygonArea.mapSquares.map { (square, polygon) ->
            val areaDef = MapAreaDefinition.from(
                polygon,
                includesById,
                excludesById,
            )
            MapAreaEntry(square, areaDef)
        }
    }

    private fun collectAreas(mapAreas: List<TomlMapArea>): PolygonArea {
        val builderLists = mutableMapOf<MapSquareKey, PolygonMapSquareBuilder>()
        for (mapArea in mapAreas) {
            val areaId = mapArea.area_id.asRSCM().toShort()
            val levels = mapArea.levels.toSet()

            for (polygon in mapArea.polygons) {
                val clipped = PolygonMapSquareClipper.closeAndClip(polygon.coords())
                for ((mapSquare, polygonVertices) in clipped) {
                    val builder = builderLists.getOrPut(mapSquare) { PolygonMapSquareBuilder() }
                    builder.polygon(areaId, levels) {
                        for (vertex in polygonVertices) {
                            val localX = vertex.x % MapSquareGrid.LENGTH
                            val localZ = vertex.z % MapSquareGrid.LENGTH
                            vertex(localX, localZ)
                        }
                    }
                }
            }
        }

        val groupedSquares = builderLists.mapValues { it.value.build() }
        return PolygonArea(groupedSquares)
    }

    private data class MapAreaEntry(val square: MapSquareKey, val areas: MapAreaDefinition)

    private data class TomlMapAreaFile(val area: List<TomlMapArea>)

    private data class TomlMapArea(
        val name: String,
        val area_id: String,
        val levels: List<Int>,
        val polygons: List<TomlPolygon> = emptyList(),
        val includes: List<String> = emptyList(),
        val excludes: List<String> = emptyList(),
    )

    private data class TomlPolygon(val vertices: List<List<Int>>) {
        fun coords(): List<CoordGrid> =
            vertices.map { vertex ->
                require(vertex.size == 2) { "vertex must be [x, z]" }
                CoordGrid(vertex[0], vertex[1])
            }
    }
}
