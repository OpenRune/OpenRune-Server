package org.rsmod.api.bottomless.milk

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import org.rsmod.api.obj.charges.ObjChargeManager
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.game.inv.Inventory
import org.rsmod.game.inv.isType
import org.rsmod.utils.bits.withBits

/**
 * Milk for anything that also takes it from a bottomless milk bucket, one use at a time. Only the
 * places live allows call this; others take the item alone
 * (https://oldschool.runescape.wiki/w/Bottomless_milk_bucket).
 *
 * TODO(content): [system:bottomless_milk] Chocolatey milk, milky mixture and hangover cure.
 */
public object MilkSources {
    public const val BUCKET_EMPTY: String = "obj.bottomless_milk_bucket"
    public const val BUCKET_FILLED: String = "obj.bottomless_milk_bucket_filled"
    public const val USES: String = "varobj.charges_16383"
    public const val MAX_USES: Int = 10_000
    public const val MILK: String = "obj.bucket_milk"

    private val charges = ObjChargeManager()

    private val usesBits by lazy {
        checkNotNull(ServerCacheManager.getVarObj(USES.asRSCM(RSCMType.VAROBJ))).bits
    }

    public fun has(inv: Inventory, milk: String = MILK): Boolean =
        milk in inv || bucketSlot(inv, milk) != null

    /** Takes one [milk], from the item first and then from a bottomless milk bucket. */
    public fun take(access: ProtectedAccess, milk: String = MILK): Boolean =
        if (milk in access.inv) {
            access.invDel(access.inv, milk).success
        } else {
            pour(access.inv, milk)
        }

    /** One use from a bottomless milk bucket; the last use leaves it empty. */
    public fun pour(inv: Inventory, milk: String = MILK): Boolean {
        val slot = bucketSlot(inv, milk) ?: return false
        val bucket = inv[slot] ?: return false
        val left = charges.getCharges(bucket, USES) - 1
        if (left == 0) {
            charges.removeAllCharges(inv, slot, USES)
        } else {
            inv[slot] = bucket.copy(vars = bucket.vars.withBits(usesBits, left))
        }
        return true
    }

    private fun bucketSlot(inv: Inventory, milk: String): Int? {
        if (milk != MILK) {
            return null
        }
        return inv.indices.firstOrNull {
            inv[it].isType(BUCKET_FILLED) && charges.getCharges(inv[it], USES) > 0
        }
    }
}
