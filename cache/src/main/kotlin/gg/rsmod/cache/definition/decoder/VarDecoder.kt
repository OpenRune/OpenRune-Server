package gg.rsmod.cache.definition.decoder

import gg.rsmod.cache.definition.DefinitionDecoder
import gg.rsmod.cache.util.Index.VARP
import gg.rsmod.cache.buffer.Reader
import gg.rsmod.cache.definition.data.VarpDefinition

class VarDecoder : DefinitionDecoder<VarpDefinition>(VARP) {

    override fun create(size: Int) = Array(size) { VarpDefinition(it) }

    override fun getFile(id: Int) = id

    override fun VarpDefinition.read(opcode: Int, buffer: Reader) {
        if (opcode == 5) {
            configType = buffer.readUnsignedShort()
        }
    }
}