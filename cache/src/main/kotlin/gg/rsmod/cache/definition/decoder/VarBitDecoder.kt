package gg.rsmod.cache.definition.decoder

import gg.rsmod.cache.definition.DefinitionDecoder
import gg.rsmod.cache.util.Index.VAR_BIT
import gg.rsmod.cache.buffer.Reader
import gg.rsmod.cache.definition.data.VarBitDefinition

class VarBitDecoder : DefinitionDecoder<VarBitDefinition>(VAR_BIT) {

    override fun create(size: Int) = Array(size) { VarBitDefinition(it) }

    override fun getFile(id: Int) = id

    override fun VarBitDefinition.read(opcode: Int, buffer: Reader) {
        if (opcode == 1) {
            varp = buffer.readShort()
            startBit = buffer.readUnsignedByte()
            endBit = buffer.readUnsignedByte()
        }
    }
}