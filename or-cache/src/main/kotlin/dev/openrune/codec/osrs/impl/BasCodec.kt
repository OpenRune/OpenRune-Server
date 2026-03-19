package dev.openrune.codec.osrs.impl

import dev.openrune.definition.opcode.DefinitionOpcode
import dev.openrune.definition.opcode.OpcodeDefinitionCodec
import dev.openrune.definition.opcode.OpcodeList
import dev.openrune.definition.opcode.OpcodeType
import dev.openrune.types.BasType

class BasCodec(val custom: Map<Int, BasType>? = emptyMap()) : OpcodeDefinitionCodec<BasType>() {

    override val definitionCodec =
        OpcodeList<BasType>().apply {
            add(DefinitionOpcode(1, OpcodeType.SHORT, BasType::readyAnim))
            add(DefinitionOpcode(2, OpcodeType.SHORT, BasType::turnOnSpot))
            add(DefinitionOpcode(3, OpcodeType.SHORT, BasType::walkForward))
            add(DefinitionOpcode(4, OpcodeType.SHORT, BasType::walkBack))
            add(DefinitionOpcode(5, OpcodeType.SHORT, BasType::walkLeft))
            add(DefinitionOpcode(6, OpcodeType.SHORT, BasType::walkRight))
            add(DefinitionOpcode(7, OpcodeType.SHORT, BasType::running))
        }

    override fun BasType.createData() {
        if (custom == null) return

        val type = custom[id] ?: return

        readyAnim = type.readyAnim
        turnOnSpot = type.turnOnSpot
        walkForward = type.walkForward
        walkBack = type.walkBack
        walkLeft = type.walkLeft
        walkRight = type.walkRight
        running = type.running
    }

    override fun createDefinition(): BasType = BasType()
}
