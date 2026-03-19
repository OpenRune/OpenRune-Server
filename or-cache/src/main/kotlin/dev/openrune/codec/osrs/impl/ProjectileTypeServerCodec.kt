package dev.openrune.codec.osrs.impl

import dev.openrune.definition.opcode.DefinitionOpcode
import dev.openrune.definition.opcode.OpcodeDefinitionCodec
import dev.openrune.definition.opcode.OpcodeList
import dev.openrune.definition.opcode.OpcodeType
import dev.openrune.types.ProjAnimType

class ProjectileTypeServerCodec(val custom: Map<Int, ProjAnimType>? = emptyMap()) :
    OpcodeDefinitionCodec<ProjAnimType>() {

    override val definitionCodec =
        OpcodeList<ProjAnimType>().apply {
            add(DefinitionOpcode(1, OpcodeType.SHORT, ProjAnimType::startHeight))
            add(DefinitionOpcode(2, OpcodeType.SHORT, ProjAnimType::endHeight))
            add(DefinitionOpcode(3, OpcodeType.SHORT, ProjAnimType::delay))
            add(DefinitionOpcode(4, OpcodeType.SHORT, ProjAnimType::angle))
            add(DefinitionOpcode(5, OpcodeType.SHORT, ProjAnimType::lengthAdjustment))
            add(DefinitionOpcode(6, OpcodeType.SHORT, ProjAnimType::progress))
            add(DefinitionOpcode(7, OpcodeType.SHORT, ProjAnimType::stepMultiplier))
        }

    override fun ProjAnimType.createData() {
        if (custom == null) return

        val type = custom[id] ?: return

        startHeight = type.startHeight
        endHeight = type.endHeight
        delay = type.delay
        angle = type.angle
        lengthAdjustment = type.lengthAdjustment
        progress = type.progress
        stepMultiplier = type.stepMultiplier
    }

    override fun createDefinition(): ProjAnimType = ProjAnimType()
}
