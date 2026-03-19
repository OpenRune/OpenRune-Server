package dev.openrune.codec.osrs.impl

import dev.openrune.definition.opcode.OpcodeDefinitionCodec
import dev.openrune.definition.opcode.OpcodeList
import dev.openrune.types.VarConType

class VarConCodec(val custom: Map<Int, VarConType>? = emptyMap()) :
    OpcodeDefinitionCodec<VarConType>() {

    override val definitionCodec = OpcodeList<VarConType>().apply {}

    override fun VarConType.createData() {
        if (custom == null) return

        val type = custom[id] ?: return
    }

    override fun createDefinition(): VarConType = VarConType()
}
