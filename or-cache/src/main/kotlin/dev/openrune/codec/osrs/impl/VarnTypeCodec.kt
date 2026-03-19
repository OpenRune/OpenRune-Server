package dev.openrune.codec.osrs.impl

import dev.openrune.definition.opcode.DefinitionOpcode
import dev.openrune.definition.opcode.OpcodeDefinitionCodec
import dev.openrune.definition.opcode.OpcodeList
import dev.openrune.definition.opcode.OpcodeType
import dev.openrune.types.VarnType

class VarnTypeCodec(val custom: Map<Int, VarnType>? = emptyMap()) :
    OpcodeDefinitionCodec<VarnType>() {

    override val definitionCodec =
        OpcodeList<VarnType>().apply {
            add(DefinitionOpcode(1, OpcodeType.BOOLEAN, VarnType::bitProtect))
        }

    override fun VarnType.createData() {
        if (custom == null) return

        val type = custom[id] ?: return

        bitProtect = type.bitProtect
    }

    override fun createDefinition(): VarnType = VarnType()
}
