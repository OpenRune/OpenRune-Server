package dev.openrune.codec.osrs.impl

import dev.openrune.definition.opcode.DefinitionOpcode
import dev.openrune.definition.opcode.OpcodeDefinitionCodec
import dev.openrune.definition.opcode.OpcodeList
import dev.openrune.definition.opcode.OpcodeType
import dev.openrune.definition.opcode.OpcodeType.BOOLEAN.enumType
import dev.openrune.definition.type.InventoryType
import dev.openrune.types.InvScope
import dev.openrune.types.InvStock
import dev.openrune.types.InventoryServerType

class InventoryServerCodec(
    val types: Map<Int, InventoryType>? = null,
    val custom: Map<Int, InventoryServerType>? = emptyMap(),
) : OpcodeDefinitionCodec<InventoryServerType>() {

    override val definitionCodec =
        OpcodeList<InventoryServerType>().apply {
            add(DefinitionOpcode(1, OpcodeType.USHORT, InventoryServerType::size))
            add(DefinitionOpcode(2, OpcodeType.USHORT, InventoryServerType::flags))

            add(DefinitionOpcode(4, enumType<InvScope>(), InventoryServerType::scope))
            add(
                DefinitionOpcode(
                    5,
                    decode = { buf, def, _ ->
                        def.stock =
                            List(buf.readByte().toInt()) {
                                InvStock(
                                    buf.readInt(),
                                    buf.readShort().toInt(),
                                    buf.readShort().toInt(),
                                )
                            }
                    },
                    encode = { buf, def ->
                        buf.writeByte(def.stock.size)
                        def.stock.forEach {
                            buf.writeInt(it.obj)
                            buf.writeShort(it.count)
                            buf.writeShort(it.restockCycles)
                        }
                    },
                )
            )
        }

    override fun InventoryServerType.createData() {
        if (types == null) return
        val inventoryType = types[id] ?: return
        size = inventoryType.size
        val customData = custom?.get(id)

        if (customData != null) {
            scope = customData.scope
            stack = customData.stack
            flags = customData.flags
            stock = customData.stock
        }
    }

    override fun createDefinition(): InventoryServerType = InventoryServerType()
}
