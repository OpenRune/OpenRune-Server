package dev.openrune.tools

import com.displee.cache.CacheLibrary
import com.displee.cache.index.archive.file.File
import dev.openrune.cache.ANIMATIONS
import dev.openrune.cache.ANIMAYAS
import dev.openrune.cache.ARCHIVE_17
import dev.openrune.cache.CacheDelegate
import dev.openrune.cache.DBTABLEINDEX
import dev.openrune.cache.MODELS
import dev.openrune.cache.MUSIC_JINGLES
import dev.openrune.cache.MUSIC_PATCHES
import dev.openrune.cache.MUSIC_SAMPLES
import dev.openrune.cache.MUSIC_TRACKS
import dev.openrune.cache.SOUNDEFFECTS
import dev.openrune.cache.TEXTURES
import dev.openrune.cache.WORLDMAPAREAS
import dev.openrune.cache.WORLDMAP_GEOGRAPHY
import dev.openrune.cache.WORLDMAP_GROUND
import dev.openrune.filesystem.Cache
import org.alter.ParamMapper.npc.SLAYER_CATEGORIES.SKELETONS

class MinifyServerCache() {


    fun init(loc : String) {
        val cache = CacheLibrary(loc)

        emptyArchive(ANIMATIONS, cache)
        emptyArchive(SKELETONS, cache)
        emptyArchive(SOUNDEFFECTS, cache)
        emptyArchive(MUSIC_TRACKS, cache)
        emptyArchive(MODELS, cache)
        emptyArchive(TEXTURES, cache)
        emptyArchive(MUSIC_JINGLES, cache)
        emptyArchive(MUSIC_SAMPLES, cache)
        emptyArchive(MUSIC_PATCHES, cache)
        emptyArchive(ARCHIVE_17, cache)
        emptyArchive(WORLDMAP_GEOGRAPHY, cache)
        emptyArchive(WORLDMAPAREAS, cache)
        emptyArchive(WORLDMAP_GROUND, cache)
        emptyArchive(DBTABLEINDEX, cache)
        emptyArchive(ANIMAYAS, cache)

        val loc = java.io.File(loc)
        val temp = java.io.File(loc,"temp")
        temp.mkdirs()
        cache.rebuild(temp)
        cache.close()
        temp.copyRecursively(loc,true)
        temp.deleteRecursively()

    }

    fun emptyArchive(id : Int, cache: CacheLibrary) {
        cache.index(id).clear()
        cache.index(id).update()
    }

}