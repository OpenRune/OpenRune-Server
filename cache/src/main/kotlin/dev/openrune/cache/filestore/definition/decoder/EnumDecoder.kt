package dev.openrune.cache.filestore.definition.decoder

import dev.openrune.cache.filestore.definition.DefinitionDecoder
import dev.openrune.cache.util.Index.ENUM
import dev.openrune.cache.filestore.buffer.Reader
import dev.openrune.cache.filestore.definition.data.EnumDefinition

class EnumDecoder : DefinitionDecoder<EnumDefinition>(ENUM) {

    override fun create(size: Int) = Array(size) { EnumDefinition(it) }

    override fun getFile(id: Int) = id

    override fun EnumDefinition.read(opcode: Int, buffer: Reader) {
        when (opcode) {
            1 -> keyType = buffer.readUnsignedByte()
            2 -> valueType = buffer.readUnsignedByte()
            3 -> defaultString = buffer.readString()
            4 -> defaultInt = buffer.readInt()
            5, 6 -> {
                val count = buffer.readUnsignedShort()
                for (i in 0 until count) {
                    val key = buffer.readInt()
                    if (opcode == 5) {
                        values[key] = buffer.readString()
                    } else {
                        values[key] = buffer.readInt()
                    }
                }
            }
            else -> throw IllegalStateException("Unknown opcode: $opcode in EnumDef")
        }
    }

}