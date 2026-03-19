package dev.openrune.codec.osrs.impl

import dev.openrune.cache.util.ItemParam
import dev.openrune.definition.*
import dev.openrune.definition.opcode.*
import dev.openrune.definition.type.EnumType
import dev.openrune.definition.type.ItemType
import dev.openrune.definition.opcode.OpcodeType.BOOLEAN.enumType
import dev.openrune.definition.opcode.impl.DefinitionOpcodeListActions
import dev.openrune.definition.opcode.impl.DefinitionOpcodeMap
import dev.openrune.definition.opcode.impl.DefinitionOpcodeParams
import dev.openrune.server.impl.item.ItemRenderDataManager
import dev.openrune.server.impl.item.WeaponTypes
import dev.openrune.server.infobox.InfoBoxItem
import dev.openrune.types.Equipment
import dev.openrune.types.ItemServerType
import dev.openrune.types.Weapon
import kotlin.reflect.KMutableProperty1
import dev.openrune.types.EquipmentStats
import dev.openrune.types.getInt

class ItemServerCodec(
    val items: Map<Int, ItemType>? = null,
    val enums : Map<Int,EnumType>?= null,
    val osrs : Map<Int, InfoBoxItem>? = null,
    val custom : Map<Int,ItemServerType>? = emptyMap()

) : OpcodeDefinitionCodec<ItemServerType>() {

    override val definitionCodec = OpcodeList<ItemServerType>().apply {
        add(DefinitionOpcode(2, OpcodeType.INT, ItemServerType::cost))
        add(DefinitionOpcode(4, OpcodeType.STRING, ItemServerType::name))
        add(DefinitionOpcode(7, OpcodeType.DOUBLE, ItemServerType::weight))
        add(DefinitionOpcode(8, OpcodeType.BOOLEAN, ItemServerType::isTradeable))
        add(DefinitionOpcode(9, OpcodeType.INT, ItemServerType::category))
        add(DefinitionOpcodeListActions(10, OpcodeType.STRING, ItemServerType::options, 5))
        add(DefinitionOpcodeListActions(11, OpcodeType.STRING, ItemServerType::interfaceOptions, 5))
        add(DefinitionOpcode(12, OpcodeType.INT, ItemServerType::noteLinkId))
        add(DefinitionOpcode(13, OpcodeType.INT, ItemServerType::noteTemplateId))
        add(DefinitionOpcode(14, OpcodeType.INT, ItemServerType::placeholderLink))
        add(DefinitionOpcode(15, OpcodeType.INT, ItemServerType::placeholderTemplate))
        add(DefinitionOpcode(16, OpcodeType.INT, ItemServerType::stacks))
        add(DefinitionOpcode(18, OpcodeType.INT, ItemServerType::appearanceOverride1))
        add(DefinitionOpcode(19, OpcodeType.INT, ItemServerType::appearanceOverride2))
        add(DefinitionOpcodeParams(20, ItemServerType::params))

        add(DefinitionOpcode(21, OpcodeType.BYTE, propertyChain(
            ItemServerType::equipment,
            Equipment::equipSlot
        )))

        add(DefinitionOpcode(22, OpcodeType.SHORT, propertyChain(
            ItemServerType::weapon,
            Weapon::specAmount
        )))

        add(DefinitionOpcode(23, OpcodeType.BYTE, propertyChain(
            ItemServerType::weapon,
            Weapon::attackRange)))

        add(DefinitionOpcode(24, OpcodeType.BYTE, propertyChain(
            ItemServerType::weapon,
            Weapon::attackSpeed
        )))

        add(DefinitionOpcodeMap(25, OpcodeType.STRING, OpcodeType.INT, propertyChain(
            ItemServerType::equipment,
            Equipment::requirements
        )))

        add(DefinitionOpcode(26, OpcodeType.STRING, ItemServerType::examine))
        add(DefinitionOpcode(27, OpcodeType.STRING, ItemServerType::destroy))
        add(DefinitionOpcode(28, OpcodeType.BOOLEAN, ItemServerType::alchable))
        add(DefinitionOpcode(29, OpcodeType.INT, ItemServerType::exchangeCost))

        add(DefinitionOpcodeMap(30, OpcodeType.STRING, OpcodeType.INT, propertyChain(
            ItemServerType::equipment,
            Equipment::requirements
        )))


        add(DefinitionOpcode(31, enumType<WeaponTypes>(), propertyChain(
            ItemServerType::weapon,
            Weapon::weaponType
        )))

        add(DefinitionOpcode(32, OpcodeType.STRING, propertyChain(
            ItemServerType::weapon,
            Weapon::weaponTypeRenderData
        )))

        ItemParam.entries.forEachIndexed { index, itemParam ->
            if (itemParam.toProperty() != null) {
                add(DefinitionOpcode(33 + index, OpcodeType.BYTE, propertyChain(
                    ItemServerType::equipment,
                    Equipment::stats,
                    itemParam.toProperty()!!
                )))
            }
        }

        add(DefinitionOpcode(60, OpcodeType.INT, propertyChain(
            ItemServerType::equipment,
            Equipment::equipType
        )))

    }

    private fun ItemParam.toProperty(): KMutableProperty1<EquipmentStats, Int>? = when (this) {
        ItemParam.ATTACK_STAB -> EquipmentStats::attackStab
        ItemParam.ATTACK_SLASH -> EquipmentStats::attackSlash
        ItemParam.ATTACK_CRUSH -> EquipmentStats::attackCrush
        ItemParam.ATTACK_MAGIC -> EquipmentStats::attackMagic
        ItemParam.ATTACK_RANGED -> EquipmentStats::attackRanged
        ItemParam.DEFENCE_STAB -> EquipmentStats::defenceStab
        ItemParam.DEFENCE_SLASH -> EquipmentStats::defenceSlash
        ItemParam.DEFENCE_CRUSH -> EquipmentStats::defenceCrush
        ItemParam.DEFENCE_MAGIC -> EquipmentStats::defenceMagic
        ItemParam.DEFENCE_RANGED -> EquipmentStats::defenceRanged
        ItemParam.MELEE_STRENGTH -> EquipmentStats::meleeStrength
        ItemParam.PRAYER -> EquipmentStats::prayer
        ItemParam.RANGED_STRENGTH -> EquipmentStats::rangedStrength
        ItemParam.MAGIC_STRENGTH -> EquipmentStats::magicStrength
        ItemParam.RANGED_DAMAGE -> EquipmentStats::rangedDamage
        ItemParam.MAGIC_DAMAGE -> EquipmentStats::magicDamage
        ItemParam.DEMON_DAMAGE -> EquipmentStats::demonDamage
        ItemParam.DEGRADEABLE -> EquipmentStats::degradeable
        ItemParam.SILVER_STRENGTH -> EquipmentStats::silverStrength
        ItemParam.CORP_BOOST -> EquipmentStats::corpBoost
        ItemParam.GOLEM_DAMAGE -> EquipmentStats::golemDamage
        ItemParam.KALPHITE_DAMAGE -> EquipmentStats::kalphiteDamage
        else -> null
    }

    private fun getItemRequirements(params: Map<String, Any>?): Map<String, Int> {
        if (params == null) return emptyMap()

        val enum81 = enums?.get(81) ?: return emptyMap()
        val enum108 = enums[108] ?: return emptyMap()
        return ItemParam.entries.filter { it.isSkillParam }.mapNotNull { skillParam ->
            val skillId = params.getInt(skillParam.id)
            val skillNameId = enum81.values.getInt(skillId)
            val skillName = enum108.getString(skillNameId)
            val linkedLevel = params.getInt(skillParam.linkedLevelReqId ?: return@mapNotNull null)
            if (linkedLevel > 0) skillName to linkedLevel else null
        }.toMap()
    }

    override fun ItemServerType.createData() {
        if (items == null) return
        val item = items[id] ?: return
        val osrsData = osrs!![id]
        val customData = custom!![id]

        id = item.id
        examine = customData?.examine?.takeIf { it.isNotEmpty() } ?: osrsData?.examine?.takeIf { it.isNotEmpty() } ?: ""

        destroy = osrsData?.destroy.takeIf { it.isNullOrEmpty() } ?: ""
        alchable = osrsData?.alchable ?: true
        cost = osrsData?.cost?.takeIf { it != -1 } ?: cost
        exchangeCost = osrsData?.exchangeCost?.takeIf { it != -1 } ?: cost

        name = item.name
        weight = item.weight
        isTradeable = item.isTradeable
        category = item.category
        options = item.options
        interfaceOptions = item.interfaceOptions
        noteLinkId = item.noteLinkId
        noteTemplateId = item.noteTemplateId
        placeholderLink = item.placeholderLink
        placeholderTemplate = item.placeholderTemplate
        stacks = item.stacks
        appearanceOverride1 = item.appearanceOverride1
        appearanceOverride2 = item.appearanceOverride2
        params = item.params

        if (item.equipSlot != -1) {

            equipment = Equipment().apply {
                equipSlot = item.equipSlot
                equipType = appearanceOverride1
                stats = EquipmentStats().apply {
                    ItemParam.entries.forEach { stat ->
                        stat.toProperty()?.set(this, params?.getInt(stat.id) ?: 0)
                    }
                }
                requirements = getItemRequirements(params).takeIf { it.isNotEmpty() } ?: osrsData?.itemReq ?: emptyMap()

            }

            if (item.equipSlot == 3 || item.equipSlot == 5) {
                weapon = Weapon().apply {

                    val combatStyle = customData?.weapon?.weaponType?.name
                        ?.takeIf { it.isNotEmpty() && it != "UNARMED" }
                        ?: osrsData?.combatStyle?.takeIf { it.isNotEmpty() }
                        ?: ""

                    fun findWeaponTypeRenderData(id: Int, weaponType: WeaponTypes): String? =
                        ItemRenderDataManager.getItemRenderAnimationByItem(id)?.name
                            ?: ItemRenderDataManager.getItemRenderAnimationById(weaponType.fallbackRenderID)?.name

                    weaponType = WeaponTypes.entries.find { it.name == combatStyle.uppercase().replace(" ", "_") } ?: WeaponTypes.UNARMED
                    weaponTypeRenderData =  findWeaponTypeRenderData(id, weaponType)

                    attackSpeed = params?.getInt(14)?: 4
                    attackRange = params?.getInt(13) ?: osrsData?.attackRange ?: 0
                    specAmount = enums?.get(906)?.values?.takeIf { it.containsKey(id.toString()) }?.getInt(id) ?: -1
                }
            }

        }

    }

    override fun createDefinition(): ItemServerType = ItemServerType()

}
