package dev.openrune.codec.osrs.impl

import dev.openrune.definition.opcode.*
import dev.openrune.definition.opcode.OpcodeType.BOOLEAN.enumType
import dev.openrune.definition.opcode.impl.DefinitionOpcodeEntityOps
import dev.openrune.definition.opcode.impl.DefinitionOpcodeListActions
import dev.openrune.definition.opcode.impl.DefinitionOpcodeParams
import dev.openrune.definition.type.ItemType
import dev.openrune.types.ItemServerType
import dev.openrune.types.ObjectServerType
import dev.openrune.util.DefinitionOpcodeParamMap
import dev.openrune.util.WeaponCategory

class ItemServerCodec(
    val rev : Int,
    val items: Map<Int, ItemType>? = null,
    val custom: Map<Int, ItemServerType>? = emptyMap(),
) : OpcodeDefinitionCodec<ItemServerType>() {

    override val definitionCodec =
        OpcodeList<ItemServerType>().apply {
            add(DefinitionOpcode(2, OpcodeType.INT, ItemServerType::cost))
            add(DefinitionOpcode(4, OpcodeType.STRING, ItemServerType::name))
            add(DefinitionOpcode(7, OpcodeType.DOUBLE, ItemServerType::weight))
            add(DefinitionOpcode(8, OpcodeType.BOOLEAN, ItemServerType::stockmarket))
            add(DefinitionOpcode(9, OpcodeType.INT, ItemServerType::category))
            add(DefinitionOpcodeEntityOps(10, ItemServerType::options, rev))
            add(
                DefinitionOpcodeListActions(
                    11,
                    OpcodeType.STRING,
                    ItemServerType::interfaceOptions,
                    5,
                )
            )
            add(
                DefinitionOpcodeListActions(
                    12,
                    OpcodeType.STRING,
                    ItemServerType::interfaceOptions,
                    5,
                )
            )
            add(DefinitionOpcode(13, OpcodeType.INT, ItemServerType::certlink))
            add(DefinitionOpcode(14, OpcodeType.INT, ItemServerType::certtemplate))
            add(DefinitionOpcode(16, OpcodeType.INT, ItemServerType::placeholderLink))
            add(DefinitionOpcode(17, OpcodeType.INT, ItemServerType::placeholderTemplate))
            add(DefinitionOpcode(18, OpcodeType.INT, ItemServerType::stacks))
            add(DefinitionOpcode(19, OpcodeType.INT, ItemServerType::wearpos1))
            add(DefinitionOpcode(20, OpcodeType.INT, ItemServerType::wearpos2))
            add(DefinitionOpcode(21, OpcodeType.INT, ItemServerType::wearpos3))
            add(DefinitionOpcodeParamMap(22, ItemServerType::paramsRaw, ItemServerType::paramMap))
            add(DefinitionOpcode(23, OpcodeType.STRING, ItemServerType::examine))

            add(
                DefinitionOpcode(
                    24,
                    decode = { buf, def, _ ->
                        val count = buf.readUnsignedByte().toInt()
                        val arr = IntArray(count)
                        repeat(count) { i -> arr[i] = buf.readInt() }
                        def.objvar = arr
                    },
                    encode = { buf, def ->
                        buf.writeByte(def.objvar.size)
                        for (v in def.objvar) {
                            buf.writeInt(v)
                        }
                    },
                    shouldEncode = { def -> def.objvar.isNotEmpty() },
                )
            )

            add(DefinitionOpcode(25, OpcodeType.INT, ItemServerType::playerCost))
            add(DefinitionOpcode(26, OpcodeType.INT, ItemServerType::playerCostDerived))
            add(DefinitionOpcode(27, OpcodeType.INT, ItemServerType::playerCostDerivedConst))
            add(DefinitionOpcode(28, OpcodeType.INT, ItemServerType::stockMarketBuyLimit))
            add(DefinitionOpcode(29, OpcodeType.INT, ItemServerType::stockMarketRecalcUsers))
            add(DefinitionOpcode(30, OpcodeType.BOOLEAN, ItemServerType::tradeable))
            add(DefinitionOpcode(31, OpcodeType.INT, ItemServerType::respawnRate))
            add(DefinitionOpcode(32, OpcodeType.INT, ItemServerType::dummyitem))
            add(DefinitionOpcode(33, OpcodeType.INT, ItemServerType::contentGroup))
            add(DefinitionOpcode(34, enumType<WeaponCategory>(), ItemServerType::weaponCategory))
            add(DefinitionOpcode(35, OpcodeType.INT, ItemServerType::transformlink))
            add(DefinitionOpcode(36, OpcodeType.INT, ItemServerType::transformtemplate))
        }

    override fun ItemServerType.createData() {
        if (items == null) return
        val item = items[id] ?: return

        cost = item.cost
        name = item.name
        weight = item.weight
        stockmarket = item.isTradeable
        category = item.category
        options = item.options
        interfaceOptions = item.interfaceOptions
        certlink = item.noteLinkId
        certtemplate = item.noteTemplateId
        placeholderLink = item.placeholderLink
        placeholderTemplate = item.placeholderTemplate
        stacks = item.stacks
        wearpos2 = item.appearanceOverride1
        wearpos1 = item.equipSlot
        wearpos3 = item.appearanceOverride2
        examine = item.examine
        paramsRaw = item.params

        val customData = custom?.get(id)

        if (customData != null) {
            objvar = customData.objvar
            playerCost = customData.playerCost
            playerCostDerived = customData.playerCostDerived
            playerCostDerivedConst = customData.playerCostDerivedConst
            stockMarketBuyLimit = customData.stockMarketBuyLimit
            stockMarketRecalcUsers = customData.stockMarketRecalcUsers
            stockmarket = customData.tradeable
            respawnRate = customData.respawnRate
            dummyitem = customData.dummyitem
            contentGroup = customData.contentGroup
            weaponCategory = customData.weaponCategory
            transformlink = customData.transformlink
            transformtemplate = customData.transformtemplate
        }

        certlink = normalize(certlink)
        certtemplate = normalize(certtemplate)
        placeholderLink = normalize(placeholderLink)
        placeholderTemplate = normalize(placeholderTemplate)
        transformlink = normalize(transformlink)
        transformtemplate = normalize(transformtemplate)


    }

    //Not sure why rsmod uses 0 and not -1 like osrs client but this is what it does
    fun normalize(value: Int): Int = if (value == -1) 0 else value

    override fun createDefinition(): ItemServerType = ItemServerType()
}
