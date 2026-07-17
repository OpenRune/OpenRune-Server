package dev.openrune.net

import com.github.michaelbull.logging.InlineLogger
import dev.openrune.filesystem.Cache
import io.netty.buffer.ByteBuf
import io.netty.buffer.Unpooled
import it.unimi.dsi.fastutil.ints.Int2ObjectMap
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap
import java.nio.file.Path
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.min

object CacheJs5GroupProvider {
    lateinit var huffmanData: ByteArray

    fun provide(archive: Int, group: Int): ByteBuf? {
        return groups[bitpack(archive, group)]
    }

    private val groups: Int2ObjectMap<ByteBuf> = Int2ObjectOpenHashMap(EXPECTED_GROUP_CAPACITY)

    fun load(path: Path) {
        val cache = Cache.load(path)
        huffmanData = cache.data(10, 1)!!

        encodeMasterIndex(cache)

        val raw = ArrayList<RawGroup>(EXPECTED_GROUP_CAPACITY)
        for (index in cache.indices()) {
            for (archive in cache.archives(index)) {
                val data = cache.sector(index, archive) ?: continue
                raw += RawGroup(index, archive, data)
            }
        }
        val pending = ConcurrentHashMap<Int, ByteBuf>(raw.size)
        raw.parallelStream().forEach { group ->
            pending[bitpack(group.archive, group.id)] =
                encodeGroupBuffer(group.archive, group.id, group.data, stripVersion = true)
        }
        for ((bitpack, buf) in pending) {
            groups[bitpack] = buf
        }

        encodeArchiveMasterIndex(cache)

        logger.info { "Loaded ${groups.size} JS5 responses" }
        cache.close()
    }

    private fun encodeMasterIndex(cache: Cache) {
        groups[bitpack(255, 255)] =
            encodeGroupBuffer(255, 255, cache.versionTable, stripVersion = false)
    }

    private fun encodeArchiveMasterIndex(cache: Cache) {
        for (archive in cache.indices()) {
            if (archive == 255) continue
            val data = cache.sector(255, archive) ?: continue
            groups[bitpack(255, archive)] =
                encodeGroupBuffer(255, archive, data, stripVersion = false)
        }
    }

    private fun encodeGroupBuffer(
        archive: Int,
        group: Int,
        data: ByteArray,
        stripVersion: Boolean,
    ): ByteBuf {
        var length = data.size
        if (stripVersion && length >= 2) {
            length -= 2
        }
        require(length >= 1) { "JS5 sector too short for $archive:$group (size=${data.size})" }

        var pos = 0
        val compression = data[pos++].toInt() and 0xFF
        var remaining = length - 1

        val trailingBlocks =
            if (remaining > BYTES_BEFORE_BLOCK) {
                (remaining - BYTES_BEFORE_BLOCK + BYTES_AFTER_BLOCK - 1) / BYTES_AFTER_BLOCK
            } else {
                0
            }
        val capacity = BLOCK_HEADER_SIZE + remaining + trailingBlocks
        val response = Unpooled.buffer(capacity)
        response.writeByte(archive)
        response.writeShort(group)
        response.writeByte(compression)

        var chunk = min(remaining, BYTES_BEFORE_BLOCK)
        response.writeBytes(data, pos, chunk)
        pos += chunk
        remaining -= chunk

        while (remaining > 0) {
            response.writeByte(0xFF)
            chunk = min(remaining, BYTES_AFTER_BLOCK)
            response.writeBytes(data, pos, chunk)
            pos += chunk
            remaining -= chunk
        }

        return Unpooled.unreleasableBuffer(response)
    }

    private data class RawGroup(val archive: Int, val id: Int, val data: ByteArray)

    private const val EXPECTED_GROUP_CAPACITY = 131_072
    private const val BLOCK_SIZE = 512
    private const val BLOCK_HEADER_SIZE = 1 + 2 + 1
    private const val BLOCK_DELIMITER_SIZE = 1
    private const val BYTES_BEFORE_BLOCK = BLOCK_SIZE - BLOCK_HEADER_SIZE
    private const val BYTES_AFTER_BLOCK = BLOCK_SIZE - BLOCK_DELIMITER_SIZE

    private val logger = InlineLogger()

    private fun bitpack(archive: Int, group: Int): Int {
        require(archive and 0xFF.inv() == 0) { "invalid archive $archive:$group" }
        require(group and 0xFFFF.inv() == 0) { "invalid group $archive:$group" }

        return ((archive and 0xFF) shl 16) or (group and 0xFFFF)
    }
}
