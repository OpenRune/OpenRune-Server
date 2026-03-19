package org.rsmod.server.shared.module

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.databind.ObjectMapper
import com.google.inject.Provider
import jakarta.inject.Inject
import java.nio.file.Files
import java.nio.file.Path
import org.rsmod.api.parsers.json.Json
import org.rsmod.game.map.xtea.XteaMap
import org.rsmod.map.square.MapSquareKey
import org.rsmod.module.ExtendedModule

object CacheStoreModule : ExtendedModule() {
    override fun bind() {
        bindProvider(XteaMapProvider::class.java)
    }
}

private class XteaMapProvider
@Inject
constructor(@Json private val mapper: ObjectMapper) :
    Provider<XteaMap> {
    override fun get(): XteaMap {
        val file = Path.of("./.data/cache/xteas.json")
        val reader = Files.newBufferedReader(file)
        val fileKeys = mapper.readValue(reader, Array<FileXtea>::class.java)
        val keys = fileKeys.associate { MapSquareKey(it.mapsquare) to it.key.toIntArray() }
        return XteaMap(HashMap(keys))
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private data class FileXtea(val mapsquare: Int = 0, val key: List<Int> = emptyList())
}
