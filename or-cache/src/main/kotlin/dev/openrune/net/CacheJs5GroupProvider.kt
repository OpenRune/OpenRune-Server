package dev.openrune.net

import com.github.michaelbull.logging.InlineLogger
import dev.openrune.filesystem.Cache
import io.netty.buffer.ByteBuf
import io.netty.buffer.Unpooled
import it.unimi.dsi.fastutil.ints.Int2LongOpenHashMap
import java.nio.file.Path
import kotlin.math.min

/**
 * Pre-encodes every JS5 group response at boot so connecting clients can be served without
 * touching the cache files again.
 *
 * All responses share one contiguous direct buffer and an int->(offset, length) index, rather than
 * a heap [ByteBuf] per group. The payload bytes are the same either way, but keeping ~118k of them
 * off the Java heap stops the collector from having to trace and copy a quarter gigabyte that never
 * changes, and it removes three objects of per-group bookkeeping. It also halves the boot-time heap
 * spike, since the encoded form no longer has to coexist with the raw sectors on the heap.
 */
object CacheJs5GroupProvider {
    lateinit var huffmanData: ByteArray

    private val index = Int2LongOpenHashMap(EXPECTED_GROUP_CAPACITY).apply { defaultReturnValue(-1L) }

    private var storage: ByteBuf? = null

    fun provide(archive: Int, group: Int): ByteBuf? {
        val packed = index.get(bitpack(archive, group))
        if (packed == -1L) {
            return null
        }
        val store = storage ?: return null
        val offset = (packed ushr OFFSET_SHIFT).toInt()
        val length = (packed and LENGTH_MASK).toInt()
        return store.slice(offset, length)
    }

    fun load(path: Path) {
        val cache = Cache.load(path)
        huffmanData = cache.data(10, 1)!!

        val pending = ArrayList<PendingGroup>(EXPECTED_GROUP_CAPACITY)
        pending += PendingGroup(255, 255, cache.versionTable, stripVersion = false)
        for (archiveIndex in cache.indices()) {
            for (group in cache.archives(archiveIndex)) {
                val data = cache.sector(archiveIndex, group) ?: continue
                pending += PendingGroup(archiveIndex, group, data, stripVersion = true)
            }
        }
        for (archiveIndex in cache.indices()) {
            if (archiveIndex == 255) continue
            val data = cache.sector(255, archiveIndex) ?: continue
            pending += PendingGroup(255, archiveIndex, data, stripVersion = false)
        }

        var total = 0L
        for (entry in pending) {
            entry.offset = total.toInt()
            entry.length = encodedLength(entry.payloadLength())
            total += entry.length
        }
        require(total <= Int.MAX_VALUE) { "JS5 payload too large to store contiguously: $total" }

        val store = Unpooled.directBuffer(total.toInt(), total.toInt())
        // Absolute writes into disjoint regions, so groups can be encoded in parallel without
        // sharing the buffer's writer index.
        pending.parallelStream().forEach { entry -> encodeInto(store, entry) }
        store.writerIndex(total.toInt())

        for (entry in pending) {
            index.put(
                bitpack(entry.archive, entry.group),
                (entry.offset.toLong() shl OFFSET_SHIFT) or entry.length.toLong(),
            )
        }

        storage = Unpooled.unreleasableBuffer(store)
        logger.info {
            "Loaded ${index.size} JS5 responses (${total / 1024 / 1024}MB off-heap)"
        }
        cache.close()
    }

    private class PendingGroup(
        val archive: Int,
        val group: Int,
        val data: ByteArray,
        val stripVersion: Boolean,
    ) {
        var offset: Int = 0
        var length: Int = 0

        fun payloadLength(): Int {
            var length = data.size
            if (stripVersion && length >= 2) {
                length -= 2
            }
            require(length >= 1) { "JS5 sector too short for $archive:$group (size=${data.size})" }
            return length
        }
    }

    private fun encodedLength(payloadLength: Int): Int {
        val remaining = payloadLength - 1
        val trailingBlocks =
            if (remaining > BYTES_BEFORE_BLOCK) {
                (remaining - BYTES_BEFORE_BLOCK + BYTES_AFTER_BLOCK - 1) / BYTES_AFTER_BLOCK
            } else {
                0
            }
        return BLOCK_HEADER_SIZE + remaining + trailingBlocks
    }

    private fun encodeInto(store: ByteBuf, entry: PendingGroup) {
        val data = entry.data
        var writeIndex = entry.offset
        var pos = 0
        val compression = data[pos++].toInt() and 0xFF
        var remaining = entry.payloadLength() - 1

        store.setByte(writeIndex++, entry.archive)
        store.setShort(writeIndex, entry.group)
        writeIndex += 2
        store.setByte(writeIndex++, compression)

        var chunk = min(remaining, BYTES_BEFORE_BLOCK)
        store.setBytes(writeIndex, data, pos, chunk)
        writeIndex += chunk
        pos += chunk
        remaining -= chunk

        while (remaining > 0) {
            store.setByte(writeIndex++, 0xFF)
            chunk = min(remaining, BYTES_AFTER_BLOCK)
            store.setBytes(writeIndex, data, pos, chunk)
            writeIndex += chunk
            pos += chunk
            remaining -= chunk
        }

        check(writeIndex - entry.offset == entry.length) {
            "JS5 encode length mismatch for ${entry.archive}:${entry.group}"
        }
    }

    private const val EXPECTED_GROUP_CAPACITY = 131_072
    private const val OFFSET_SHIFT = 32
    private const val LENGTH_MASK = 0xFFFFFFFFL
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
