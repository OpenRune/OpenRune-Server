package dev.openrune.types

import dev.openrune.ServerCacheManager
import dev.openrune.definition.Definition
import dev.openrune.definition.type.ItemType
import dev.openrune.definition.type.ParamType
import dev.openrune.server.impl.item.WeaponTypes
import org.alter.rscm.RSCM
import org.alter.rscm.RSCM.asRSCM
import org.alter.rscm.RSCMType

data class ItemServerType(
    override var id: Int = -1,
    var cost: Int = -1,
    var name: String = "",
    var weight: Double = 0.0,
    var isTradeable: Boolean = false,
    var category: Int = -1,
    var options: MutableList<String?> = mutableListOf(null, null, "Take", null, null),
    var interfaceOptions: MutableList<String?> = mutableListOf(null, null, null, null, "Drop"),
    var noteLinkId: Int = -1,
    var noteTemplateId: Int = -1,
    var placeholderLink: Int = -1,
    var dummyitem : Int = 0,
    var placeholderTemplate: Int = -1,
    var stacks: Int = 0,
    var appearanceOverride1: Int = -1,
    var appearanceOverride2: Int = -1,
    var examine : String = "",
    var destroy: String = "",
    var alchable: Boolean = true,
    var exchangeCost: Int = -1,

    var transformlink : Int = -1,
    var transformtemplate: Int = -1,
    var equipment: Equipment? = null,
    var weapon: Weapon? = null,
    var params: MutableMap<String, Any>? = null,
) : Definition {

    public val resolvedDummyitem: Dummyitem?
        get() = Dummyitem[dummyitem]


    val stackable: Boolean
        get() = stacks == 1 || noteTemplateId > 0

    val noted: Boolean
        get() = noteTemplateId > 0

    /**
     * Whether or not the object is a placeholder.
     */
    val isPlaceholder
        get() = placeholderTemplate > 0 && placeholderLink > 0

    public val hasPlaceholder: Boolean
        get() = placeholderLink > 0 && placeholderTemplate == 0

    public val canCert: Boolean
        get() = !stackable && noteLinkId > 0 && noteTemplateId == 0

    public val isCert: Boolean
        get() = noteTemplateId != 0

    public val isTransformation: Boolean
        get() = transformtemplate != 0


    fun param(param: String): Int {
        RSCM.requireRSCM(RSCMType.PARAM, param)

        val key = param.asRSCM().toString()
        return params?.get(key) as? Int ?: 0
    }

    public fun isCategoryType(cat: String): Boolean {
        RSCM.requireRSCM(RSCMType.CATEGORY,cat)
        return category == cat.asRSCM()
    }

    companion object {
        public fun placeholder(type: ItemServerType): ItemServerType {
            if (!type.hasPlaceholder) {
                return type
            }
            val link = type.placeholderLink
            return ServerCacheManager.getItem(link) ?: throw NoSuchElementException("Type is missing in the map: $link.")
        }

        public fun untransform(type: ItemServerType): ItemServerType {
            if (!type.isTransformation) {
                return type
            }
            val link = type.transformlink
            return ServerCacheManager.getItem(link) ?: throw NoSuchElementException("Type is missing in the map: $link.")
        }

    }

}

data class EquipmentStats(
    var attackStab: Int = 0,
    var attackSlash: Int = 0,
    var attackCrush: Int = 0,
    var attackMagic: Int = 0,
    var attackRanged: Int = 0,
    var defenceStab: Int = 0,
    var defenceSlash: Int = 0,
    var defenceCrush: Int = 0,
    var defenceMagic: Int = 0,
    var defenceRanged: Int = 0,
    var meleeStrength: Int = 0,
    var prayer: Int = 0,
    var rangedStrength: Int = 0,
    var magicStrength: Int = 0,
    var rangedDamage: Int = 0,
    var magicDamage: Int = 0,
    var demonDamage: Int = 0,
    var degradeable: Int = 0,
    var silverStrength: Int = 0,
    var corpBoost: Int = 0,
    var golemDamage: Int = 0,
    var kalphiteDamage: Int = 0,
    var undead: Int = 0,
    var slayer: Int = 0,
    var undeadMeleeOnly : Boolean = true,
    var slayerMeleeOnly : Boolean = true
)


data class Equipment(
    var equipSlot: Int = -1,
    var equipType : Int = -1,
    var requirements: Map<String, Int> = emptyMap(),
    var stats: EquipmentStats? = null
) {

    //val equipmentOptions: List<String?> by lazy {
      //  (0 until 7).map { cachedParams.getString(451 + it).takeIf { it.isNotEmpty() } }
    //}
}


data class Weapon(
    var weaponTypeRenderData: String? = "default_player",
    var weaponType: WeaponTypes = WeaponTypes.UNARMED,
    var attackSpeed: Int = 0,
    var attackRange: Int = 0,
    var specAmount: Int = -1
) {
    fun hasSpec() = specAmount != -1
}


private fun Map<String, Any?>.getString(key: Int): String =
    this[key.toString()]?.toString() ?: ""

fun Map<String, Any?>.getInt(key: Int): Int = when (val v = this[key.toString()]) {
    is Int -> v
    is Number -> v.toInt()
    is String -> v.toIntOrNull() ?: 0
    else -> 0
}

public enum class Dummyitem(public val id: Int) {
    /** Cannot be added into inventories or dropped on floor. */
    GraphicOnly(id = 1),
    /** Can be added into inventories, but cannot be dropped on floor. */
    InvOnly(id = 2);

    public companion object {
        public operator fun get(id: Int): Dummyitem? =
            when (id) {
                GraphicOnly.id -> GraphicOnly
                InvOnly.id -> InvOnly
                else -> null
            }
    }
}

