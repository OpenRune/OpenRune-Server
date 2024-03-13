package gg.rsmod.cache

import com.github.michaelbull.logging.InlineLogger
import gg.rsmod.cache.definition.data.NPCDefinition
import gg.rsmod.cache.definition.decoder.NPCDecoder

object CacheManager {

    var npcs : Array<NPCDefinition> = emptyArray()

    fun init() {
        val cache = timed("cache") { Cache.load(false) }
        timed("npcLoader") {
            npcs = NPCDecoder().load(cache)
            println(getNpc(5512).name)
        }
    }

    private fun getNpc(id : Int) = npcs[id]


    private val logger = InlineLogger("TimedLoader")

    fun <R> timed(name: String, block: () -> R): R {
        val start = System.currentTimeMillis()
        val result = block.invoke()
        val duration = System.currentTimeMillis() - start
        logger.info { "Loaded $name in ${duration}ms" }
        return result
    }


}

fun main() {
    CacheManager.init()
}