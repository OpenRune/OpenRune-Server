package dev.openrune.tools

import com.displee.cache.CacheLibrary
import dev.openrune.cache.ANIMATIONS
import dev.openrune.cache.ANIMAYAS
import dev.openrune.cache.DEFAULTS
import dev.openrune.cache.MODELS
import dev.openrune.cache.MUSIC_JINGLES
import dev.openrune.cache.MUSIC_PATCHES
import dev.openrune.cache.MUSIC_TRACKS
import dev.openrune.cache.SKELETONS
import dev.openrune.cache.SOUNDEFFECTS
import dev.openrune.cache.TEXTURES
import dev.openrune.cache.VORBIS
import dev.openrune.cache.WORLDMAPAREAS
import dev.openrune.cache.WORLDMAP_GEOGRAPHY
import dev.openrune.cache.WORLDMAP_GROUND

/**
 * Indices the server never reads. The server cache build leaves them empty while it is seeded from the
 * live cache (see `serverEmptyIndices` on the cache tool), so [init] is only needed to strip an existing
 * server cache by hand.
 */
class MinifyServerCache() {

    fun init(loc: String) {
        val cache = CacheLibrary(loc)

        STRIPPED_INDICES.forEach { emptyArchive(it, cache) }

        val loc = java.io.File(loc)
        val temp = java.io.File(loc, "temp")
        temp.mkdirs()
        cache.rebuild(temp)
        cache.close()
        temp.copyRecursively(loc, true)
        temp.deleteRecursively()
    }

    fun emptyArchive(id: Int, cache: CacheLibrary) {
        val index = cache.index(id)
        index.clear()
        // clear() does not flag the reference table; without this update() leaves it as it was.
        index.flag()
        index.update()
    }

    companion object {
        val STRIPPED_INDICES: Set<Int> = setOf(
            ANIMATIONS,
            SKELETONS,
            SOUNDEFFECTS,
            MUSIC_TRACKS,
            MODELS,
            TEXTURES,
            MUSIC_JINGLES,
            VORBIS,
            MUSIC_PATCHES,
            DEFAULTS,
            WORLDMAP_GEOGRAPHY,
            WORLDMAPAREAS,
            WORLDMAP_GROUND,
            ANIMAYAS,
        )
    }
}
