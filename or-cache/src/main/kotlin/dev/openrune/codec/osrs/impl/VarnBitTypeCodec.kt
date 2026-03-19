package dev.openrune.codec.osrs.impl

import dev.openrune.definition.opcode.DefinitionOpcode
import dev.openrune.definition.opcode.OpcodeDefinitionCodec
import dev.openrune.definition.opcode.OpcodeList
import dev.openrune.definition.opcode.OpcodeType
import dev.openrune.types.VarnBitType

class VarnBitTypeCodec(val custom: Map<Int, VarnBitType>? = emptyMap()) :
    OpcodeDefinitionCodec<VarnBitType>() {

    override val definitionCodec =
        OpcodeList<VarnBitType>().apply {
            add(DefinitionOpcode(1, OpcodeType.SHORT, VarnBitType::varn))
            add(DefinitionOpcode(2, OpcodeType.SHORT, VarnBitType::lsb))
            add(DefinitionOpcode(3, OpcodeType.SHORT, VarnBitType::msb))
        }

    override fun VarnBitType.createData() {
        if (custom == null) return

        val type = custom[id] ?: return

        varn = type.varn
        lsb = type.lsb
        msb = type.msb
    }

    override fun createDefinition(): VarnBitType = VarnBitType()
}
