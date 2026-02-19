package dev.openrune.codec.osrs.impl

import dev.openrune.definition.opcode.DefinitionOpcode
import dev.openrune.definition.opcode.OpcodeDefinitionCodec
import dev.openrune.definition.opcode.OpcodeList
import dev.openrune.definition.opcode.OpcodeType
import dev.openrune.definition.opcode.OpcodeType.BOOLEAN.enumType
import dev.openrune.definition.opcode.propertyChain
import dev.openrune.definition.type.InventoryType
import dev.openrune.server.impl.item.WeaponTypes
import dev.openrune.types.HealthBarServerType
import dev.openrune.types.InvScope
import dev.openrune.types.InvStackType
import dev.openrune.types.InvStock
import dev.openrune.types.InventoryServerType
import dev.openrune.types.InventoryServerType.Companion.pack
import dev.openrune.types.ItemServerType
import dev.openrune.types.Weapon


class InventoryServerCodec(
    val types: Map<Int, InventoryType>? = null,
    val custom : Map<Int, InventoryServerType>? = emptyMap()

) : OpcodeDefinitionCodec<InventoryServerType>() {

    override val definitionCodec = OpcodeList<InventoryServerType>().apply {
        add(DefinitionOpcode(1, OpcodeType.USHORT, InventoryServerType::size))
        add(DefinitionOpcode(2, OpcodeType.USHORT, InventoryServerType::flags))
        add(DefinitionOpcode(3, enumType<InvStackType>(), InventoryServerType::stack))
        add(DefinitionOpcode(4, enumType<InvScope>(), InventoryServerType::scope))
    }

    override fun InventoryServerType.createData() {
        if (types == null) return
        val inventoryType = types[id]?: return
        size = inventoryType.size
        val customData = custom!![id]

        if (customData != null) {
            scope = customData.scope
            stack = customData.stack
            flags = customData.flags
        }

    }


    override fun createDefinition(): InventoryServerType = InventoryServerType()

}
