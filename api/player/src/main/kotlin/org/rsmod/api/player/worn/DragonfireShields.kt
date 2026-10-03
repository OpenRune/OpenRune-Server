package org.rsmod.api.player.worn

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import org.rsmod.game.inv.InvObj
import org.rsmod.utils.bits.getBits
import org.rsmod.utils.bits.withBits

/** Item-local charges follow the shield through inventory, equipment, banking and persistence. */
public object DragonfireShields {
    public const val MAX_CHARGES: Int = 50

    public enum class Kind(public val charged: String, public val uncharged: String) {
        SHIELD("obj.dragonfire_shield", "obj.dragonfire_shield_uncharged"),
        WARD("obj.dragonfire_ward", "obj.dragonfire_ward_uncharged"),
        WYVERN("obj.wyvern_shield", "obj.wyvern_shield_uncharged"),
    }

    public fun kind(obj: InvObj?): Kind? =
        obj?.let { item -> Kind.entries.firstOrNull {
            item.id == it.charged.asRSCM() || item.id == it.uncharged.asRSCM()
        } }

    public fun charges(obj: InvObj?): Int {
        val kind = kind(obj) ?: return 0
        if (obj!!.id == kind.uncharged.asRSCM()) return 0
        val bits = checkNotNull(ServerCacheManager.getVarObj("varobj.charges_16383".asRSCM())).bits
        return obj.vars.getBits(bits).coerceIn(0, MAX_CHARGES)
    }

    public fun withCharges(obj: InvObj, charges: Int): InvObj {
        require(charges in 0..MAX_CHARGES)
        val kind = requireNotNull(kind(obj))
        val bits = checkNotNull(ServerCacheManager.getVarObj("varobj.charges_16383".asRSCM())).bits
        val id = (if (charges == 0) kind.uncharged else kind.charged).asRSCM()
        return obj.copy(id = id, vars = obj.vars.withBits(bits, charges))
    }
}
