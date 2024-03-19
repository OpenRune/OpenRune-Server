package dev.openrune.cache.filestore.definition.decoder

import dev.openrune.cache.filestore.definition.DefinitionDecoder
import dev.openrune.cache.util.Index.VARP
import dev.openrune.cache.filestore.buffer.Reader
import dev.openrune.cache.filestore.definition.data.VarpDefinition

class VarDecoder : DefinitionDecoder<VarpDefinition>(VARP) {

    override fun create(size: Int) = Array(size) { VarpDefinition(it) }

    override fun getFile(id: Int) = id

    override fun VarpDefinition.read(opcode: Int, buffer: Reader) {
        if (opcode == 5) {
            configType = buffer.readUnsignedShort()
        }
    }
}