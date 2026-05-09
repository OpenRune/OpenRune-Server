package dev.openrune.codec.osrs.impl

import dev.openrune.definition.opcode.DefinitionOpcode
import dev.openrune.definition.opcode.OpcodeDefinitionCodec
import dev.openrune.definition.opcode.OpcodeList
import dev.openrune.definition.opcode.OpcodeType
import dev.openrune.types.VarObjBitType

class VarObjCodec(val custom: Map<Int, VarObjBitType>? = emptyMap()) :
    OpcodeDefinitionCodec<VarObjBitType>() {

    override val definitionCodec =
        OpcodeList<VarObjBitType>().apply {
            add(DefinitionOpcode(1, OpcodeType.SHORT, VarObjBitType::startBit))
            add(DefinitionOpcode(2, OpcodeType.SHORT, VarObjBitType::endBit))
        }

    override fun VarObjBitType.createData() {
        if (custom == null) return

        val type = custom[id] ?: return

        startBit = type.startBit
        endBit = type.endBit
    }

    override fun createDefinition(): VarObjBitType = VarObjBitType()
}
