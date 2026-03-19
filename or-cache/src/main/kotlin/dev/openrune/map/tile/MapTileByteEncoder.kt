package dev.openrune.map.tile

import dev.openrune.cache.MAPS
import dev.openrune.filesystem.Cache
import dev.openrune.map.util.toReadableByteArray
import io.netty.buffer.Unpooled
import kotlin.collections.component1
import kotlin.collections.component2
import kotlin.collections.iterator
import org.rsmod.map.square.MapSquareKey

/*
 * Currently, we do not decode and re-encode `m[x]_[z]` map data before packing. While consistency
 * with other codecs is typically preferred, map files are large and do not require decoding unless
 * merging multiple files - a step not permitted for `m` files.
 *
 * That is why this encoder accepts a raw byte data wrapper from the file instead of
 * `MapTileDefinition`.
 */
public object MapTileByteEncoder {
    public fun encodeAll(cache: Cache, definitions: Map<MapSquareKey, MapTileByteDefinition>) {
        val archive = MAPS
        for ((key, definition) in definitions) {
            val group = "m${key.x}_${key.z}"
            val newBuf = Unpooled.wrappedBuffer(definition.data)
            cache.write(archive, group, newBuf.toReadableByteArray())
        }
    }
}
