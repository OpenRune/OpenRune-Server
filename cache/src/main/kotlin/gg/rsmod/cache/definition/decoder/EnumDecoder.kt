package gg.rsmod.cache.definition.decoder

import gg.rsmod.cache.definition.DefinitionDecoder
import gg.rsmod.cache.util.Index.ENUM
import gg.rsmod.cache.buffer.Reader
import gg.rsmod.cache.definition.data.EnumDefinition

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