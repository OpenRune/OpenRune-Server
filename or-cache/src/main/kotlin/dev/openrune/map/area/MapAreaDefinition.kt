package dev.openrune.map.area

import it.unimi.dsi.fastutil.bytes.Byte2ObjectMap
import it.unimi.dsi.fastutil.bytes.Byte2ObjectOpenHashMap
import it.unimi.dsi.fastutil.shorts.Short2ObjectMap
import it.unimi.dsi.fastutil.shorts.Short2ObjectOpenHashMap
import it.unimi.dsi.fastutil.shorts.ShortArraySet
import it.unimi.dsi.fastutil.shorts.ShortSet
import kotlin.collections.iterator
import org.rsmod.game.area.polygon.PolygonMapSquare

public data class MapAreaDefinition(
    val mapSquareAreas: ShortSet,
    val zoneAreas: Byte2ObjectMap<ShortSet>,
    val coordAreas: Short2ObjectMap<ShortSet>,
    val includes: Short2ObjectMap<ShortSet>,
    val excludes: Short2ObjectMap<ShortSet>,
) {
    public companion object {

        public fun from(
            polygon: PolygonMapSquare,
            includes: Map<Short, ShortArray>,
            excludes: Map<Short, ShortArray> = emptyMap(),
        ): MapAreaDefinition {
            return polygon.toMapAreaDefinition(includes, excludes)
        }

        private fun PolygonMapSquare.toMapAreaDefinition(
            includesByArea: Map<Short, ShortArray>,
            excludesByArea: Map<Short, ShortArray>,
        ): MapAreaDefinition {
            val flippedZoneAreas = Byte2ObjectOpenHashMap<ShortSet>()
            for ((area, bitset) in zoneAreas) {
                var index = bitset.nextSetBit(0)
                while (index != -1) {
                    val zoneKey = index.toByte()
                    val areaSet = flippedZoneAreas.getOrPut(zoneKey) {
                        ShortArraySet()
                    }
                    areaSet.add(area)
                    index = bitset.nextSetBit(index + 1)
                }
            }

            val flippedCoordAreas = Short2ObjectOpenHashMap<ShortSet>()
            for ((area, bitset) in coordAreas) {
                var index = bitset.nextSetBit(0)
                while (index != -1) {
                    val areaSet = flippedCoordAreas.getOrPut(index.toShort()) {
                        ShortArraySet()
                    }
                    areaSet.add(area)
                    index = bitset.nextSetBit(index + 1)
                }
            }

            val flippedIncludes = Short2ObjectOpenHashMap<ShortSet>()
            for (area in mapSquareAreas) {
                val refs = includesByArea[area] ?: continue
                flippedIncludes[area] = ShortArraySet(refs)
            }

            val flippedExcludes = Short2ObjectOpenHashMap<ShortSet>()
            for (area in mapSquareAreas) {
                val refs = excludesByArea[area] ?: continue
                flippedExcludes[area] = ShortArraySet(refs)
            }

            return MapAreaDefinition(
                mapSquareAreas = mapSquareAreas,
                zoneAreas = flippedZoneAreas,
                coordAreas = flippedCoordAreas,
                includes = flippedIncludes,
                excludes = flippedExcludes,
            )
        }

        public fun merge(
            edit: MapAreaDefinition,
            base: MapAreaDefinition,
        ): MapAreaDefinition {
            val mergedMapSquares =
                ShortArraySet(base.mapSquareAreas.size + edit.mapSquareAreas.size)
            mergedMapSquares.addAll(base.mapSquareAreas)
            mergedMapSquares.addAll(edit.mapSquareAreas)

            val mergedZoneAreas =
                Byte2ObjectOpenHashMap<ShortSet>(
                    base.zoneAreas.size + edit.zoneAreas.size
                )

            for ((zone, areas) in base.zoneAreas) {
                mergedZoneAreas[zone] = ShortArraySet(areas)
            }

            for ((zone, areas) in edit.zoneAreas) {
                val merged = mergedZoneAreas.getOrPut(zone) {
                    ShortArraySet()
                }
                merged.addAll(areas)
            }

            val mergedCoordAreas =
                Short2ObjectOpenHashMap<ShortSet>(
                    base.coordAreas.size + edit.coordAreas.size
                )

            for ((coord, areas) in base.coordAreas) {
                mergedCoordAreas[coord] = ShortArraySet(areas)
            }

            for ((coord, areas) in edit.coordAreas) {
                val merged = mergedCoordAreas.getOrPut(coord) {
                    ShortArraySet()
                }
                merged.addAll(areas)
            }

            val mergedIncludes =
                Short2ObjectOpenHashMap<ShortSet>(
                    base.includes.size + edit.includes.size
                )

            for ((area, refs) in base.includes) {
                mergedIncludes[area] = ShortArraySet(refs)
            }

            for ((area, refs) in edit.includes) {
                val merged = mergedIncludes.getOrPut(area) {
                    ShortArraySet()
                }
                merged.addAll(refs)
            }

            val mergedExcludes =
                Short2ObjectOpenHashMap<ShortSet>(
                    base.excludes.size + edit.excludes.size
                )

            for ((area, refs) in base.excludes) {
                mergedExcludes[area] = ShortArraySet(refs)
            }

            for ((area, refs) in edit.excludes) {
                val merged = mergedExcludes.getOrPut(area) {
                    ShortArraySet()
                }
                merged.addAll(refs)
            }

            return MapAreaDefinition(
                mapSquareAreas = mergedMapSquares,
                zoneAreas = mergedZoneAreas,
                coordAreas = mergedCoordAreas,
                includes = mergedIncludes,
                excludes = mergedExcludes,
            )
        }
    }
}
