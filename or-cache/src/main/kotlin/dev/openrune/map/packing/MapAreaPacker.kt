package dev.openrune.map.packing

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import dev.openrune.filesystem.Cache
import dev.openrune.map.area.MapAreaDefinition
import dev.openrune.map.area.MapAreaEncoder
import dev.openrune.rscm.RSCM.asRSCM
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

    private fun listJsonFiles(dir: Path): List<Path> {
        Files.list(dir).use { paths ->
            return paths.filter { it.isRegularFile() && it.extension == "json" }.sorted().toList()
        }
    }

    public fun encodeCacheMapArea(cache: Cache) {
        val areas = loadAndCollect()
        MapAreaEncoder.encodeAll(cache, areas)
    }

    private fun loadAndCollect(): Map<MapSquareKey, MapAreaDefinition> {
        val files = listJsonFiles(Path.of("../.data/raw-cache/map/area"))
        val gson = Gson()
        return files.flatMap { path -> loadAndCollect(path, gson) }.mergeToMap()
    }

    private fun List<MapAreaEntry>.mergeToMap(): Map<MapSquareKey, MapAreaDefinition> {
        val merged = mutableMapOf<MapSquareKey, MapAreaDefinition>()
        for ((mapSquare, def) in this) {
            merged.merge(mapSquare, def, MapAreaDefinition.Companion::merge)
        }
        return merged
    }

    private fun loadAndCollect(path: Path, gson: Gson): List<MapAreaEntry> {
        val mapAreas = loadMapAreas(path, gson)
        val areas = collectAreas(mapAreas)
        return toAreaConfigList(areas)
    }

    private fun loadMapAreas(path: Path, gson: Gson): Array<JsonMapArea> {
        val input = Files.readString(path)
        val type = object : TypeToken<Array<JsonMapArea>>() {}.type
        return gson.fromJson(input, type)
    }

    private fun toAreaConfigList(polygonArea: PolygonArea): List<MapAreaEntry> {
        return polygonArea.mapSquares.map { (square, polygon) ->
            val areaDef = MapAreaDefinition.from(polygon)
            MapAreaEntry(square, areaDef)
        }
    }

    private fun collectAreas(mapAreas: Array<JsonMapArea>): PolygonArea {
        val builderLists = mutableMapOf<MapSquareKey, PolygonMapSquareBuilder>()
        for (mapArea in mapAreas) {
            val areaId = mapArea.areaId.asRSCM().toShort()
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

    private data class JsonMapArea(
        val name: String,
        val areaId: String,
        val levels: List<Int>,
        val polygons: List<JsonPolygon>,
    )

    private data class JsonPolygon(val vertices: List<Point>) {
        fun coords(): List<CoordGrid> = vertices.map { CoordGrid(it.x, it.z) }
    }

    private data class Point(val x: Int, val z: Int)
}
