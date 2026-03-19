package dev.openrune.codec.osrs.impl

import dev.openrune.definition.opcode.DefinitionOpcode
import dev.openrune.definition.opcode.OpcodeDefinitionCodec
import dev.openrune.definition.opcode.OpcodeList
import dev.openrune.definition.opcode.OpcodeType
import dev.openrune.types.ModLevelType

class ModLevelServerCodec(val custom: Map<Int, ModLevelType>? = emptyMap()) :
    OpcodeDefinitionCodec<ModLevelType>() {

    override val definitionCodec =
        OpcodeList<ModLevelType>().apply {
            add(DefinitionOpcode(1, OpcodeType.BYTE, ModLevelType::clientCode))
            add(DefinitionOpcode(2, OpcodeType.STRING, ModLevelType::displayName))
            add(
                DefinitionOpcode(
                    3,
                    decode = { buf, def, _ -> def.accessflags = buf.readLong() },
                    encode = { buf, def -> buf.writeLong(def.accessflags) },
                )
            )
        }

    override fun ModLevelType.createData() {
        if (custom == null) return

        val modLevel = custom[id] ?: return

        clientCode = modLevel.clientCode
        displayName = modLevel.displayName
        accessflags = modLevel.accessflags
    }

    override fun createDefinition(): ModLevelType = ModLevelType()
}
