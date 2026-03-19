package dev.openrune.codec.osrs.impl

import dev.openrune.definition.opcode.DefinitionOpcode
import dev.openrune.definition.opcode.OpcodeDefinitionCodec
import dev.openrune.definition.opcode.OpcodeList
import dev.openrune.definition.opcode.OpcodeType
import dev.openrune.definition.opcode.impl.DefinitionOpcodeEntityOps
import dev.openrune.definition.opcode.impl.DefinitionOpcodeParams
import dev.openrune.definition.opcode.impl.DefinitionOpcodeTransforms
import dev.openrune.definition.type.ObjectType
import dev.openrune.revision
import dev.openrune.types.NpcServerType
import dev.openrune.types.ObjectServerType
import dev.openrune.util.DefinitionOpcodeParamMap

class ObjectServerCodec(
    val rev : Int,
    val objects: Map<Int, ObjectType>? = null,
    val custom: Map<Int, ObjectServerType>? = emptyMap(),
    val examines: Map<Int, String> = emptyMap(),
) : OpcodeDefinitionCodec<ObjectServerType>() {

    override val definitionCodec =
        OpcodeList<ObjectServerType>().apply {
            add(DefinitionOpcode(1, OpcodeType.STRING, ObjectServerType::name))
            add(DefinitionOpcode(2, OpcodeType.STRING, ObjectServerType::desc))
            add(DefinitionOpcode(3, OpcodeType.INT, ObjectServerType::width))
            add(DefinitionOpcode(4, OpcodeType.INT, ObjectServerType::length))
            add(DefinitionOpcode(5, OpcodeType.INT, ObjectServerType::category))
            add(DefinitionOpcode(6, OpcodeType.USHORT, ObjectServerType::contentGroup))
            add(DefinitionOpcode(7, OpcodeType.USHORT, ObjectServerType::forceApproachFlags))
            add(DefinitionOpcode(8, OpcodeType.USHORT, ObjectServerType::blockWalk))
            add(DefinitionOpcode(9, OpcodeType.BOOLEAN, ObjectServerType::blockRange))
            add(DefinitionOpcode(10, OpcodeType.BOOLEAN, ObjectServerType::breakRouteFinding))

            add(DefinitionOpcodeEntityOps(11, ObjectServerType::actions, rev))
            add(
                DefinitionOpcodeTransforms(
                    IntRange(12, 13),
                    ObjectServerType::transforms,
                    ObjectServerType::multiVarBit,
                    ObjectServerType::multiVarp,
                    ObjectServerType::multiDefault,
                )
            )
            add(DefinitionOpcodeParamMap(14, ObjectServerType::paramsRaw, ObjectServerType::paramMap))
        }

    override fun ObjectServerType.createData() {
        if (objects == null) return

        val obj = objects[id] ?: return

        name = obj.name
        category = obj.category
        width = obj.sizeX
        length = obj.sizeY
        blockRange = obj.impenetrable
        blockWalk = obj.solid
        forceApproachFlags = obj.clipMask
        breakRouteFinding = obj.isHollow
        multiVarBit = obj.multiVarBit
        multiVarp = obj.multiVarp
        transforms = obj.transforms
        multiDefault = obj.multiDefault

        paramsRaw = obj.params
        actions = obj.actions

        val customData = custom?.get(id)

        if (customData != null) {
            desc = customData.desc.takeIf { it.isNotEmpty() } ?: examines[obj.id] ?: ""
            contentGroup = customData.contentGroup
        }
    }

    override fun createDefinition(): ObjectServerType = ObjectServerType()
}
