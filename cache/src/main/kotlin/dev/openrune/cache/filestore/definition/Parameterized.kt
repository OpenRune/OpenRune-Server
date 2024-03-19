package dev.openrune.cache.filestore.definition

import dev.openrune.cache.filestore.buffer.Reader
import it.unimi.dsi.fastutil.ints.Int2ObjectArrayMap

interface Parameterized {

    var params: Map<Int, Any>?

    fun readParameters(buffer: Reader) {
        val length = buffer.readUnsignedByte()
        if (length == 0) {
            return
        }
        val params = Int2ObjectArrayMap<Any>()
        for (i in 0 until length) {
            val string = buffer.readUnsignedBoolean()
            val id = buffer.readUnsignedMedium()
            params[id] = if (string) buffer.readString() else buffer.readInt()
        }
        this.params = params
    }

}