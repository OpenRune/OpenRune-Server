package dev.openrune.map.npc

import dev.openrune.cache.MAPS
import dev.openrune.filesystem.Cache
import dev.openrune.map.util.toReadableByteArray
import io.netty.buffer.ByteBuf
import io.netty.buffer.PooledByteBufAllocator
import kotlin.collections.component1
import kotlin.collections.component2
import kotlin.collections.iterator
import org.rsmod.map.square.MapSquareKey

public object MapNpcListEncoder {

    public fun encodeAll(cache: Cache, spawns: Map<MapSquareKey, MapNpcListDefinition>) {
        val buffer = PooledByteBufAllocator.DEFAULT.buffer()
        val archive = MAPS
        for ((key, definition) in spawns) {
            val group = (key.x shl 8) or (key.z and 0xFF)
            val newBuf = buffer.clear().apply { encode(definition, this) }
            cache.write(archive, group,5, newBuf.toReadableByteArray())
        }
        buffer.release()
    }

    public fun encode(definition: MapNpcListDefinition, data: ByteBuf): Unit =
        with(definition) {
            check(packedSpawns.size <= 65535) {
                "Map npc spawn size exceeds limit: ${packedSpawns.size} / 65535"
            }
            data.writeShort(packedSpawns.size)
            for (packed in packedSpawns.intIterator()) {
                data.writeInt(packed)
            }
        }
}
