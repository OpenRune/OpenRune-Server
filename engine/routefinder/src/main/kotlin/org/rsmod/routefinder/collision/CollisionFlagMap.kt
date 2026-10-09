package org.rsmod.routefinder.collision

/**
 * Collision bitflags for every tile in the game world, stored as a two-level index.
 *
 * A flat zone table would be `2048 * 2048 * 4` references - 64MB of mostly-null pointers, since
 * only a few hundred thousand zones exist. Zones are instead grouped into their map square, so the
 * top-level table holds one page per (map square, level) and only populated map squares allocate
 * one. That trades a second indirection for ~62MB and a working set small enough that a route
 * search's local area stays cache-resident.
 */
public class CollisionFlagMap {
    private val pages: Array<Array<IntArray?>?> = arrayOfNulls(MAPSQUARE_COUNT)

    public inline val defaultFlag: Int
        inline get() = DEFAULT_COLLISION_FLAG

    /**
     * Gets the collision bitmask of tile in coordinates ([absoluteX], [absoluteZ], [level]).
     *
     * If the zone respective to the input coordinates has not been allocated, [defaultFlag] will be
     * returned instead.
     */
    public operator fun get(absoluteX: Int, absoluteZ: Int, level: Int): Int {
        val pageIndex = mapSquareIndex(absoluteX, absoluteZ, level)
        val page = pages[pageIndex] ?: return DEFAULT_COLLISION_FLAG
        val tiles = page[zoneSlot(absoluteX, absoluteZ)] ?: return DEFAULT_COLLISION_FLAG
        return tiles[tileIndex(absoluteX, absoluteZ)]
    }

    /**
     * Sets the collision bitmask of tile in coordinates ([absoluteX], [absoluteZ], [level]) to
     * [mask].
     *
     * If the zone respective to the input coordinates has not been previously set, it will be
     * allocated before setting the [mask] bitflag onto the tile.
     */
    public operator fun set(absoluteX: Int, absoluteZ: Int, level: Int, mask: Int) {
        val tiles =
            tilesOrNull(absoluteX, absoluteZ, level)
                ?: allocateIfAbsent(absoluteX, absoluteZ, level)
        tiles[tileIndex(absoluteX, absoluteZ)] = mask
    }

    /**
     * Appends the collision [mask] into the already-existing bitflags located on coordinates
     * ([absoluteX], [absoluteZ], [level]).
     *
     * If the zone respective to the input coordinates has not been previously set, it will be
     * allocated before applying the [mask] bitflag onto the tile.
     */
    public fun add(absoluteX: Int, absoluteZ: Int, level: Int, mask: Int) {
        // If the zone has not been allocated previously - the `set`
        // operator will allocate/initialize it. We do _not_ want the
        // `defaultFlag` value to be used. This is why we don't use
        // the `get` operator and instead redeclare similar code below.
        val tiles = tilesOrNull(absoluteX, absoluteZ, level)
        val currentFlags = tiles?.get(tileIndex(absoluteX, absoluteZ)) ?: 0
        this[absoluteX, absoluteZ, level] = currentFlags or mask
    }

    public fun remove(absoluteX: Int, absoluteZ: Int, level: Int, mask: Int) {
        val tiles = tilesOrNull(absoluteX, absoluteZ, level) ?: return
        val tileIndex = tileIndex(absoluteX, absoluteZ)
        tiles[tileIndex] = tiles[tileIndex] and mask.inv()
    }

    /**
     * Allocates and initializes the collision flags for the zone that can be found in coordinates
     * ([absoluteX], [absoluteZ], [level]). If the zone has already been allocated then nothing will
     * be performed.
     *
     * The x and z-coordinate can range anywhere between 0-7 tiles in respect to the zone base
     * coordinates. For example, calling this method with the arguments (3202, 3204, [level]) will
     * have the same results as calling it with (3200, 3200, [level]).
     *
     * Allocation is synchronized because the game map is decoded one coroutine per map square, and
     * a loc wider than one tile can reach across a map square border into a page another coroutine
     * owns. Reads stay lock-free; the decoder's `awaitAll` publishes them to the game thread.
     */
    public fun allocateIfAbsent(absoluteX: Int, absoluteZ: Int, level: Int): IntArray {
        tilesOrNull(absoluteX, absoluteZ, level)?.let {
            return it
        }
        return synchronized(pages) {
            val pageIndex = mapSquareIndex(absoluteX, absoluteZ, level)
            val page =
                pages[pageIndex]
                    ?: arrayOfNulls<IntArray>(ZONES_PER_MAPSQUARE).also { pages[pageIndex] = it }
            val slot = zoneSlot(absoluteX, absoluteZ)
            page[slot] ?: IntArray(ZONE_TILE_COUNT).also { page[slot] = it }
        }
    }

    /**
     * Deallocates the collision flags for the zone that can be found in coordinates ([absoluteX],
     * [absoluteZ], [level]).
     *
     * The x and z-coordinate can range anywhere between 0-7 tiles in respect to the zone base
     * coordinates. For example, calling this method with the arguments (3202, 3204, [level]) will
     * have the same results as calling it with (3200, 3200, [level]).
     */
    public fun deallocateIfPresent(absoluteX: Int, absoluteZ: Int, level: Int) {
        val page = pages[mapSquareIndex(absoluteX, absoluteZ, level)] ?: return
        page[zoneSlot(absoluteX, absoluteZ)] = null
    }

    public fun isZoneAllocated(absoluteX: Int, absoluteZ: Int, level: Int): Boolean {
        return tilesOrNull(absoluteX, absoluteZ, level) != null
    }

    /** Drops every allocated zone, returning the map to its freshly-constructed state. */
    public fun reset() {
        pages.fill(null)
    }

    /**
     * Replaces this map's zones with [other]'s.
     *
     * Zone tile arrays are shared, not copied, so a tile written through either map afterwards is
     * visible in both; only the page spine is private to each map.
     */
    public fun copyFrom(other: CollisionFlagMap) {
        for (i in pages.indices) {
            pages[i] = other.pages[i]?.copyOf()
        }
    }

    private fun tilesOrNull(absoluteX: Int, absoluteZ: Int, level: Int): IntArray? {
        val page = pages[mapSquareIndex(absoluteX, absoluteZ, level)] ?: return null
        return page[zoneSlot(absoluteX, absoluteZ)]
    }

    public companion object {
        public const val DEFAULT_COLLISION_FLAG: Int = -1

        private const val MAPSQUARE_COUNT: Int = 256 * 256 * 4
        private const val ZONES_PER_MAPSQUARE: Int = 8 * 8
        private const val ZONE_TILE_COUNT: Int = 8 * 8

        private fun tileIndex(x: Int, z: Int): Int = (x and 0x7) or ((z and 0x7) shl 3)

        private fun mapSquareIndex(x: Int, z: Int, level: Int): Int =
            ((x shr 6) and 0xFF) or (((z shr 6) and 0xFF) shl 8) or ((level and 0x3) shl 16)

        private fun zoneSlot(x: Int, z: Int): Int =
            ((x shr 3) and 0x7) or (((z shr 3) and 0x7) shl 3)
    }
}
