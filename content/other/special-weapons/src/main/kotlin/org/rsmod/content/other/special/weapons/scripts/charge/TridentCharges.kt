package org.rsmod.content.other.special.weapons.scripts.charge

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import org.rsmod.game.inv.InvObj
import org.rsmod.utils.bits.getBits
import org.rsmod.utils.bits.withBits

/** Persistent, item-local state. The existing powered-staff varobj supplies its 15-bit layout. */
internal object TridentCharges {
    data class Kind(val charged: String, val empty: String, val max: Int, val toxic: Boolean, val full: String? = null) {
        val symbols get() = listOfNotNull(charged, empty, full)
        val recipe get() = listOf("obj.deathrune" to 1, "obj.chaosrune" to 1, "obj.firerune" to 5,
            if (toxic) "obj.snakeboss_scale" to 1 else "obj.coins" to 10)

        // Coins pay for charging and are not recovered on uncharge.
        val refund get() = recipe.filterNot { it.first == "obj.coins" }
    }

    val kinds = buildList {
        for (orn in listOf(false, true)) {
            val suffix = if (orn) "_orn" else ""
            add(Kind("obj.tots_charged$suffix", "obj.tots_uncharged$suffix", 2500, false, "obj.tots$suffix"))
            add(Kind("obj.tots_i_charged$suffix", "obj.tots_i_uncharged$suffix", 20000, false))
            add(Kind("obj.toxic_tots_charged$suffix", "obj.toxic_tots_uncharged$suffix", 2500, true))
            add(Kind("obj.toxic_tots_i_charged$suffix", "obj.toxic_tots_i_uncharged$suffix", 20000, true))
        }
    }

    fun kind(obj: InvObj?): Kind? = obj?.let { item -> kinds.firstOrNull { k -> k.symbols.any { it.asRSCM() == item.id } } }
    private val bits get() = checkNotNull(ServerCacheManager.getVarObj("varobj.tumeken_charges".asRSCM())).bits

    fun count(obj: InvObj): Int {
        val kind = requireNotNull(kind(obj))
        if (obj.id == kind.empty.asRSCM()) return 0
        // Tradable full tridents encode their contents in their item identity, not vars.
        if (kind.full?.asRSCM() == obj.id) return kind.max
        return obj.vars.getBits(bits).coerceIn(0, kind.max)
    }

    fun withCharges(obj: InvObj, count: Int): InvObj {
        val kind = requireNotNull(kind(obj))
        require(count in 0..kind.max)
        val symbol = when {
            count == 0 -> kind.empty
            count == kind.max && kind.full != null -> kind.full
            else -> kind.charged
        }
        return obj.copy(id = symbol.asRSCM(), vars = obj.vars.withBits(bits, count))
    }
}
