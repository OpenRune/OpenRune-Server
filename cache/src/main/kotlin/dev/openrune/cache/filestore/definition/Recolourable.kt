package dev.openrune.cache.filestore.definition

import dev.openrune.cache.filestore.buffer.Reader

interface Recolourable {
    var originalColours: ShortArray?
    var modifiedColours: ShortArray?
    var originalTextureColours: ShortArray?
    var modifiedTextureColours: ShortArray?

    fun readColours(buffer: Reader) {
        val length = buffer.readUnsignedByte()
        originalColours = ShortArray(length)
        modifiedColours = ShortArray(length)
        for (count in 0 until length) {
            originalColours!![count] = buffer.readShort().toShort()
            modifiedColours!![count] = buffer.readShort().toShort()
        }
    }

    fun readTextures(buffer: Reader) {
        val length = buffer.readUnsignedByte()
        originalTextureColours = ShortArray(length)
        modifiedTextureColours = ShortArray(length)
        for (count in 0 until length) {
            originalTextureColours!![count] = buffer.readShort().toShort()
            modifiedTextureColours!![count] = buffer.readShort().toShort()
        }
    }

}