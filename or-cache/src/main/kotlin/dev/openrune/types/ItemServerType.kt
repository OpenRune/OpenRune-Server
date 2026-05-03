package dev.openrune.types

import dev.openrune.ParamMap
import dev.openrune.ServerCacheManager
import dev.openrune.TypedParamType
import dev.openrune.definition.Definition
import dev.openrune.definition.EntityOpsDefinition
import dev.openrune.definition.type.ParamType
import dev.openrune.resolve
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import dev.openrune.seralizer.ParamSerializer
import dev.openrune.toml.rsconfig.RsTableHeaders
import dev.openrune.toml.serialization.TomlField
import dev.openrune.types.aconverted.CategoryType
import dev.openrune.util.Dummyitem
import dev.openrune.util.WeaponCategory

@RsTableHeaders("item")
data class ItemServerType(
    override var id: Int = -1,
    var cost: Int = -1,
    var name: String = "",
    var weight: Double = 0.0,
    var stockmarket: Boolean = false,
    var category: Int = -1,
    var options: EntityOpsDefinition = EntityOpsDefinition().op(2, "Take"),
    var interfaceOptions: MutableList<String?> = mutableListOf(null, null, null, null, "Drop"),
    var certlink: Int = 0,
    var certtemplate: Int = 0,
    var placeholderLink: Int = 0,
    var placeholderTemplate: Int = 0,
    var stacks: Int = 0,
    var wearpos1: Int = -1,
    var wearpos2: Int = -1,
    var wearpos3: Int = -1,
    var examine: String = "",
    @param:TomlField(["params"],serializer = ParamSerializer::class)
    var paramsRaw: MutableMap<Int, Any>? = null,
    var objvar: IntArray = intArrayOf(),
    var playerCost: Int = 0,
    var playerCostDerived: Int = 0,
    var playerCostDerivedConst: Int = 0,
    var stockMarketBuyLimit: Int = 0,
    var stockMarketRecalcUsers: Int = 0,
    var tradeable: Boolean = true,
    var respawnRate: Int = 100,
    var dummyitem: Int = -1,
    var contentGroup: Int = -1,
    var weaponCategory: WeaponCategory = WeaponCategory.Unarmed,
    var transformlink: Int = 0,
    var transformtemplate: Int = 0,
) : Definition {

    var paramMap: ParamMap? = null

    public fun <T : Any> param(type: TypedParamType<T>): T = paramMap.resolve(type)

    public fun <T : Any> paramOrNull(type: ParamType): T? = paramMap?.get(type)

    public fun <T : Any> paramOrNull(type: TypedParamType<T>): T? = paramMap?.get(type)

    public fun hasParam(type: TypedParamType<*>): Boolean = paramOrNull(type) != null

    public val lowercaseName: String
        get() = name.lowercase()

    public val stackable: Boolean
        get() = stacks == 1

    public val isStackable: Boolean
        get() = (stackable || certtemplate > 0) && objvar.isEmpty()

    public val hasPlaceholder: Boolean
        get() = placeholderLink > 0 && placeholderTemplate == 0

    public val isPlaceholder: Boolean
        get() = placeholderTemplate != 0

    public val canCert: Boolean
        get() = !stackable && certlink > 0 && certtemplate == 0 && objvar.isEmpty()

    public val isCert: Boolean
        get() = certtemplate != 0

    public val hasTransformation: Boolean
        get() = transformlink > 0 && transformtemplate == 0

    public val isTransformation: Boolean
        get() = transformtemplate != 0

    public val isDummyItem: Boolean
        get() = dummyitem != -1

    public val resolvedDummyitem: Dummyitem?
        get() = Dummyitem[dummyitem]

    public val highAlch: Int
        get() = cost * 60 / 100

    public val lowAlch: Int
        get() = cost * 40 / 100

    public val isEquipable: Boolean
        get() = wearpos1 != -1 && (interfaceOptions[1] == "Wield" || interfaceOptions[1] == "Wear")

    public fun hasOp(interactionOp: Int): Boolean {
        val text = options.getOpOrNull(interactionOp - 1) ?: return false
        val invalid = text.isBlank() || text.equals("hidden", ignoreCase = true)
        return !invalid
    }

    public fun hasInvOp(slot: Int): Boolean {
        val text = interfaceOptions.getOrNull(slot - 1) ?: return false
        return text.isNotBlank()
    }

    public fun isAnyType(type1: ItemServerType, type2: ItemServerType): Boolean {
        return (type1.id == id || type2.id == id)
    }

    public fun isAnyType(
        type1: String,
        type2: String,
        type3: String,
    ): Boolean {
        return (type1.asRSCM(RSCMType.OBJ) == id || type2.asRSCM(RSCMType.OBJ) == id || type3.asRSCM(RSCMType.OBJ) == id)
    }

    public fun isAnyType(
        type1: String,
        type2: String,
        type3: String,
        type4: String,
    ): Boolean {
        return (type1.asRSCM(RSCMType.OBJ) == id || type2.asRSCM(RSCMType.OBJ) == id || type3.asRSCM(RSCMType.OBJ) == id || type4.asRSCM(RSCMType.OBJ) == id)
    }

    public fun isAnyType(
        type1: String,
        type2: String,
        vararg types: String,
    ): Boolean {
        return type1.asRSCM(RSCMType.OBJ) == id || type2.asRSCM(RSCMType.OBJ) == id || types.any { it.asRSCM(RSCMType.OBJ) == id }
    }

    public fun isContentType(content: String): Boolean {
        return contentGroup == content.asRSCM(RSCMType.CONTENT)
    }

    public fun isCategory(cat: CategoryType): Boolean {
        return category == cat.id
    }

    public fun isCategoryType(cat: String): Boolean {
        return category == cat.asRSCM(RSCMType.CATEGORY)
    }

    public fun isType(other: ItemServerType): Boolean {
        return this.id == other.id
    }

    public fun isType(other: String): Boolean {
        return this.id == other.asRSCM(RSCMType.OBJ)
    }

}
