package org.rsmod.content.other.sawmill

import dev.openrune.cache.MAPS
import dev.openrune.filesystem.Cache
import dev.openrune.map.GameMapBuilder
import dev.openrune.map.GameMapDecoder
import dev.openrune.map.loc.MapLocListDecoder
import dev.openrune.map.loc.MapLocListDefinition
import dev.openrune.map.tile.MapTileDecoder
import dev.openrune.map.tile.MapTileSimpleDefinition
import dev.openrune.map.util.InlineByteBuf
import org.rsmod.game.loc.LocEntity
import org.rsmod.game.loc.LocZoneKey
import org.rsmod.game.map.LocZoneStorage
import org.rsmod.map.CoordGrid
import org.rsmod.map.square.MapSquareKey
import org.rsmod.map.zone.ZoneKey
import org.rsmod.routefinder.collision.CollisionFlagMap
import org.rsmod.routefinder.flag.CollisionFlag

/** The real map squares the four sawmills stand in, decoded once. */
internal object SawmillMap {
    val squares: List<MapSquareKey> =
        listOf(MapSquareKey(51, 54), MapSquareKey(25, 54), MapSquareKey(51, 95), MapSquareKey(21, 52))

    const val BLOCKED: Int = CollisionFlag.BLOCK_WALK or CollisionFlag.LOC

    data class Placed(val id: Int, val coords: CoordGrid, val shape: Int, val angle: Int)

    class Built(val collision: CollisionFlagMap, val placed: List<Placed>) {
        fun walkable(coords: CoordGrid): Boolean =
            collision[coords.x, coords.z, coords.level] and BLOCKED == 0

        fun flags(coords: CoordGrid): Int = collision[coords.x, coords.z, coords.level]

        fun at(id: Int): List<Placed> = placed.filter { it.id == id }
    }

    private val data =
        mutableListOf<Triple<MapSquareKey, MapTileSimpleDefinition, MapLocListDefinition>>()

    fun load(cache: Cache) {
        data.clear()
        for (square in squares) {
            val group = (square.x shl 8) or square.z
            val tileData = cache.data(MAPS, group, 0) ?: continue
            val locData = cache.data(MAPS, group, 1) ?: continue
            data +=
                Triple(
                    square,
                    MapTileDecoder.decode(InlineByteBuf(tileData)),
                    MapLocListDecoder.decode(InlineByteBuf(locData)),
                )
        }
    }

    fun build(storage: LocZoneStorage? = null): Built {
        val collision = CollisionFlagMap()
        val placed = mutableListOf<Placed>()
        val builder = GameMapBuilder()
        for ((square, tiles, locs) in data) {
            for (level in 0..3) {
                for (x in square.x * 64 until square.x * 64 + 64 step 8) {
                    for (z in square.z * 64 until square.z * 64 + 64 step 8) {
                        collision.allocateIfAbsent(x, z, level)
                    }
                }
            }
            GameMapDecoder.putMaps(collision, square, tiles)
            GameMapDecoder.putLocs(builder, collision, square, tiles, locs)
        }
        for ((packed, zone) in builder.zoneBuilders) {
            val base = ZoneKey(packed).toCoords()
            val built = zone.build()
            if (storage != null) storage.mapLocs[packed] = built
            for (entry in built.byte2IntEntrySet()) {
                val key = LocZoneKey(entry.byteKey)
                val loc = LocEntity(entry.intValue)
                placed += Placed(loc.id, base.translate(key.x, key.z), loc.shape, loc.angle)
            }
        }
        return Built(collision, placed)
    }
}
