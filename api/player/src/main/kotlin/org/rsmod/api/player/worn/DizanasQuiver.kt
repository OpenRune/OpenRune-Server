package org.rsmod.api.player.worn

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.types.ItemServerType
import dev.openrune.types.aconverted.CategoryType
import dev.openrune.util.Wearpos
import org.rsmod.api.config.refs.params
import org.rsmod.api.player.stat.baseRangedLvl
import org.rsmod.api.player.vars.VarPlayerIntMapSetter
import org.rsmod.game.entity.Player
import org.rsmod.game.inv.InvObj
import org.rsmod.game.type.getOrNull

/** Native account ammo vars own the shared extra slot; charges follow the individual cape. */
public object DizanasQuiver {
    public const val AMMO: String = "varp.dizanas_quiver_temp_ammo"
    public const val COUNT: String = "varp.dizanas_quiver_temp_ammo_amount"
    public const val MAX_CHARGES: Int = 20_000
    public const val BLESSING: Int = 150_000
    public val pairs: List<Pair<String, String>> = listOf(
        "obj.dizanas_quiver_charged" to "obj.dizanas_quiver_uncharged",
        "obj.dizanas_quiver_charged_trouver" to "obj.dizanas_quiver_uncharged_trouver",
    )
    public val infinite: List<String> = listOf("obj.dizanas_quiver_infinite", "obj.dizanas_quiver_infinite_trouver", "obj.skillcape_max_dizanas", "obj.skillcape_max_dizanas_trouver")
    public val usable: List<String> = pairs.flatMap { listOf(it.first, it.second) } + infinite
    public fun recognises(item: InvObj?): Boolean = item != null && usable.any { it.asRSCM() == item.id }
    public fun owns(player: Player): Boolean = recognises(player.worn[Wearpos.Back.slot]) || player.inv.any(::recognises)
    public fun blessed(item: InvObj?): Boolean = item != null && infinite.any { it.asRSCM() == item.id }

    public data class Charges(public val count: Int, public val progress: Int)

    // Current item vars are 32 bits. The two constrained values fit in an unsigned word:
    // spent * 20001 + current <= 150000 * 20001 < 2^32. No precision is discarded.
    public fun charges(item: InvObj?): Charges {
        if (!recognises(item) || blessed(item)) return Charges(0, 0)
        val encoded = item!!.vars.toLong() and 0xffffffffL
        val count = (encoded % (MAX_CHARGES + 1)).toInt()
        val progress = encoded / (MAX_CHARGES + 1) + count
        return if (progress <= BLESSING) Charges(count, progress.toInt()) else Charges(0, 0)
    }
    public fun withCharges(item: InvObj, value: Charges): InvObj {
        require(value.count in 0..MAX_CHARGES && value.progress in value.count..BLESSING)
        val pair = requireNotNull(pairs.firstOrNull { item.id == it.first.asRSCM() || item.id == it.second.asRSCM() })
        val encoded = (value.progress - value.count).toLong() * (MAX_CHARGES + 1) + value.count
        return item.copy(id = (if (value.count == 0) pair.second else pair.first).asRSCM(), vars = encoded.toInt())
    }
    public fun storable(type: ItemServerType): Boolean = type.certtemplate == 0 && (
        type.isCategoryType("category.arrows") || type.isCategoryType("category.dragon_arrow") ||
            type.isCategoryType("category.crossbow_bolt") || type.id == "obj.barrows_karil_ammo".asRSCM() ||
            type.id == "obj.xbows_grapple_tip_bolt_mithril_rope".asRSCM() ||
            type.id == "obj.sunlight_antelope_bolt".asRSCM() || type.id == "obj.moonlight_antelope_bolt".asRSCM())
    public fun stored(player: Player): InvObj? {
        val count = player.vars[COUNT]
        val type = ServerCacheManager.getItem(player.vars[AMMO])?.takeIf { storable(it) } ?: return null
        return if (count > 0) InvObj(type, count) else null
    }
    public fun store(player: Player, item: InvObj?) {
        require(item == null || item.count > 0 && item.vars == 0 && storable(ServerCacheManager.getItem(item.id)!!))
        VarPlayerIntMapSetter.set(player, AMMO, item?.id ?: -1)
        VarPlayerIntMapSetter.set(player, COUNT, item?.count ?: 0)
    }

    /** Uses the same native required-ammo, bone-weapon and level rules as normal combat. */
    public fun compatible(weapon: ItemServerType, ammo: ItemServerType): Boolean {
        val bow = weapon.isCategoryType("category.bow")
        val crossbow = weapon.isCategoryType("category.crossbow")
        if (!bow && !crossbow) return false
        val required = weapon.paramOrNull(params.required_ammo) ?: CategoryType((if (bow) "category.arrows" else "category.crossbow_bolt").asRSCM())
        val alternative = bow && required.isType("category.arrows") && ammo.isCategoryType("category.dragon_arrow")
        return (ammo.isCategory(required) || alternative) && ammo.param(params.levelrequire) <= weapon.param(params.levelrequire) &&
            (!crossbow || ammo.param(params.bone_weapon) == 0 || weapon.param(params.bone_weapon) != 0)
    }
    public fun selected(player: Player): InvObj? {
        val ordinary = player.worn[Wearpos.Quiver.slot]
        if (!recognises(player.worn[Wearpos.Back.slot]) || player.baseRangedLvl < 75) return ordinary
        val weapon = getOrNull(player.worn[Wearpos.RightHand.slot]) ?: return ordinary
        if (ordinary != null && compatible(weapon, ServerCacheManager.getItem(ordinary.id)!!)) return ordinary
        val extra = stored(player) ?: return ordinary
        return if (compatible(weapon, ServerCacheManager.getItem(extra.id)!!)) extra else ordinary
    }
    public fun usingExtra(player: Player): Boolean = owns(player) && selected(player)?.let { it != player.worn[Wearpos.Quiver.slot] && it == stored(player) } == true
    public fun sunfire(player: Player): Boolean {
        val cape = player.worn[Wearpos.Back.slot]
        if (!recognises(cape) || !blessed(cape) && charges(cape).count == 0) return false
        val weapon = getOrNull(player.worn[Wearpos.RightHand.slot]) ?: return false
        val ammo = getOrNull(selected(player)) ?: return false
        return storable(ammo) && compatible(weapon, ammo)
    }
    public fun consumeCharge(player: Player) {
        val item = player.worn[Wearpos.Back.slot] ?: return
        if (!sunfire(player) || blessed(item)) return
        val before = charges(item)
        player.worn[Wearpos.Back.slot] = withCharges(item, before.copy(count = before.count - 1))
        VarPlayerIntMapSetter.set(player, "varbit.worn_items_dizanas_quiver_charged", if (before.count > 1) 1 else 0)
    }
}
