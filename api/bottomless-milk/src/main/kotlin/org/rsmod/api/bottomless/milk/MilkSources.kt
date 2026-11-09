package org.rsmod.api.bottomless.milk

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import org.rsmod.api.obj.charges.ObjChargeManager
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.game.inv.InvObj
import org.rsmod.game.inv.Inventory
import org.rsmod.game.inv.isType
import org.rsmod.utils.bits.getBits
import org.rsmod.utils.bits.withBits

// UNVERIFIED: that a bucket of another milk doesn't count as a bucket of milk for quests and cats
// (the wiki lists those uses without saying which milk).
public object MilkSources {
    public const val BUCKET_EMPTY: String = "obj.bottomless_milk_bucket"
    public const val BUCKET_FILLED: String = "obj.bottomless_milk_bucket_filled"
    public const val USES: String = "varobj.charges_16383"
    public const val TYPE: String = "varobj.bottomless_milk_type"
    public const val MAX_USES: Int = 10_000
    public const val MILK: String = "obj.bucket_milk"
    private const val TYPES = "enum.bottomless_milk_types"
    private const val NAMES = "enum.bottomless_milk_names"

    private val charges = ObjChargeManager()

    private val usesBits by lazy { varObjBits(USES) }
    private val typeBits by lazy { varObjBits(TYPE) }

    public val types: Map<Int, String> by lazy {
        enumValues(TYPES).mapValues { RSCM.getReverseMapping(RSCMType.OBJ, (it.value as Number).toInt()) }
    }

    private val names: Map<Int, String> by lazy { enumValues(NAMES).mapValues { it.value as String } }

    public fun typeOf(milk: String): Int? = types.entries.firstOrNull { it.value == milk }?.key

    public fun nameOf(type: Int): String = checkNotNull(names[type]) { "No name for milk type $type." }

    public fun uses(bucket: InvObj?): Int = charges.getCharges(bucket, USES)

    public fun typeIn(bucket: InvObj?): Int? =
        if (bucket.isType(BUCKET_FILLED) && uses(bucket) > 0) bucket!!.vars.getBits(typeBits) else null

    public fun withType(bucket: InvObj, type: Int): InvObj =
        bucket.copy(vars = bucket.vars.withBits(typeBits, type))

    public fun has(inv: Inventory, milk: String = MILK): Boolean =
        milk in inv || bucketSlot(inv, milk) != null

    public fun take(access: ProtectedAccess, milk: String = MILK): Boolean =
        if (milk in access.inv) {
            access.invDel(access.inv, milk).success
        } else {
            pour(access.inv, milk)
        }

    public fun pour(inv: Inventory, milk: String = MILK): Boolean {
        val slot = bucketSlot(inv, milk) ?: return false
        return pourFrom(inv, slot, milk)
    }

    public fun pourFrom(inv: Inventory, slot: Int, milk: String = MILK): Boolean {
        val bucket = inv[slot] ?: return false
        if (typeIn(bucket) != typeOf(milk)) {
            return false
        }
        val left = uses(bucket) - 1
        if (left == 0) {
            empty(inv, slot)
        } else {
            inv[slot] = bucket.copy(vars = bucket.vars.withBits(usesBits, left))
        }
        return true
    }

    public fun empty(inv: Inventory, slot: Int) {
        charges.removeAllCharges(inv, slot, USES)
        inv[slot] = inv[slot]?.copy(vars = 0)
    }

    private fun bucketSlot(inv: Inventory, milk: String): Int? {
        val type = typeOf(milk) ?: return null
        return inv.indices.firstOrNull { typeIn(inv[it]) == type }
    }

    private fun varObjBits(name: String) =
        checkNotNull(ServerCacheManager.getVarObj(name.asRSCM(RSCMType.VAROBJ))).bits

    private fun enumValues(name: String): Map<Int, Any> =
        checkNotNull(ServerCacheManager.getEnum(name.asRSCM(RSCMType.ENUM))) { "Missing $name." }
            .values
            .entries
            .associate { (it.key as Number).toInt() to it.value!! }
}
