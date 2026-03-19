package dev.openrune.codec.osrs.impl

import dev.openrune.definition.opcode.DefinitionOpcode
import dev.openrune.definition.opcode.OpcodeDefinitionCodec
import dev.openrune.definition.opcode.OpcodeList
import dev.openrune.definition.opcode.OpcodeType.BOOLEAN.enumType
import dev.openrune.types.WalkTriggerPriority
import dev.openrune.types.WalkTriggerType

class WalkTriggerTypeCodec(val custom: Map<Int, WalkTriggerType>? = emptyMap()) :
    OpcodeDefinitionCodec<WalkTriggerType>() {

    override val definitionCodec =
        OpcodeList<WalkTriggerType>().apply {
            add(DefinitionOpcode(1, enumType<WalkTriggerPriority>(), WalkTriggerType::priority))
        }

    override fun WalkTriggerType.createData() {
        if (custom == null) return

        val type = custom[id] ?: return

        priority = type.priority
    }

    override fun createDefinition(): WalkTriggerType = WalkTriggerType()
}
