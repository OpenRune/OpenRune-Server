package dev.openrune.codec.osrs.impl

import dev.openrune.definition.opcode.DefinitionOpcode
import dev.openrune.definition.opcode.OpcodeDefinitionCodec
import dev.openrune.definition.opcode.OpcodeList
import dev.openrune.definition.opcode.OpcodeType
import dev.openrune.types.StatType

class StatTypeServerCodec(val custom: Map<Int, StatType>? = emptyMap()) :
    OpcodeDefinitionCodec<StatType>() {

    override val definitionCodec =
        OpcodeList<StatType>().apply {
            add(DefinitionOpcode(1, OpcodeType.BYTE, StatType::minLevel))
            add(DefinitionOpcode(2, OpcodeType.BYTE, StatType::maxLevel))
            add(DefinitionOpcode(3, OpcodeType.STRING, StatType::displayName))
            add(DefinitionOpcode(4, OpcodeType.BOOLEAN, StatType::unreleased))
        }

    override fun StatType.createData() {
        if (custom == null) return

        val type = custom[id] ?: return

        minLevel = type.minLevel
        maxLevel = type.maxLevel
        displayName = type.displayName
        unreleased = type.unreleased
    }

    override fun createDefinition(): StatType = StatType()
}
