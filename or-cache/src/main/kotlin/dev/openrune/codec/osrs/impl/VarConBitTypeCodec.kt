package dev.openrune.codec.osrs.impl

import dev.openrune.definition.opcode.DefinitionOpcode
import dev.openrune.definition.opcode.OpcodeDefinitionCodec
import dev.openrune.definition.opcode.OpcodeList
import dev.openrune.definition.opcode.OpcodeType
import dev.openrune.types.VarConBitType

class VarConBitTypeCodec(val custom: Map<Int, VarConBitType>? = emptyMap()) :
    OpcodeDefinitionCodec<VarConBitType>() {

    override val definitionCodec =
        OpcodeList<VarConBitType>().apply {
            add(DefinitionOpcode(1, OpcodeType.SHORT, VarConBitType::varcon))
            add(DefinitionOpcode(2, OpcodeType.SHORT, VarConBitType::lsb))
            add(DefinitionOpcode(3, OpcodeType.SHORT, VarConBitType::msb))
        }

    override fun VarConBitType.createData() {
        if (custom == null) return

        val type = custom[id] ?: return

        varcon = type.varcon
        lsb = type.lsb
        msb = type.msb
    }

    override fun createDefinition(): VarConBitType = VarConBitType()
}
