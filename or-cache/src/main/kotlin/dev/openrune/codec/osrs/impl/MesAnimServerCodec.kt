package dev.openrune.codec.osrs.impl

import dev.openrune.definition.opcode.DefinitionOpcode
import dev.openrune.definition.opcode.OpcodeDefinitionCodec
import dev.openrune.definition.opcode.OpcodeList
import dev.openrune.definition.opcode.OpcodeType
import dev.openrune.types.MesAnimType

class MesAnimServerCodec(val custom: Map<Int, MesAnimType>? = emptyMap()) :
    OpcodeDefinitionCodec<MesAnimType>() {

    override val definitionCodec =
        OpcodeList<MesAnimType>().apply {
            add(DefinitionOpcode(1, OpcodeType.USHORT, MesAnimType::len1))
            add(DefinitionOpcode(2, OpcodeType.USHORT, MesAnimType::len2))
            add(DefinitionOpcode(3, OpcodeType.USHORT, MesAnimType::len3))
            add(DefinitionOpcode(4, OpcodeType.USHORT, MesAnimType::len4))
        }

    override fun MesAnimType.createData() {
        if (custom == null) return

        val seq = custom[id] ?: return

        len1 = seq.len1
        len2 = seq.len2
        len3 = seq.len3
        len4 = seq.len4
    }

    override fun createDefinition(): MesAnimType = MesAnimType()
}
