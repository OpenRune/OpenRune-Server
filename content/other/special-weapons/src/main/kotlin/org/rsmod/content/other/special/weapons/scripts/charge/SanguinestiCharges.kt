package org.rsmod.content.other.special.weapons.scripts.charge

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import org.rsmod.game.inv.InvObj
import org.rsmod.utils.bits.getBits
import org.rsmod.utils.bits.withBits

internal object SanguinestiCharges {
    const val MAX = 20_000
    const val BLOOD_RUNES = 2
    val variants = listOf(
        "obj.sanguinesti_staff" to "obj.sanguinesti_staff_uncharged",
        "obj.sanguinesti_staff_or" to "obj.sanguinesti_staff_uncharged_or",
    )
    fun variant(item: InvObj?): Pair<String, String>? = item?.let { obj -> variants.firstOrNull { obj.id == it.first.asRSCM() || obj.id == it.second.asRSCM() } }
    private val bits get() = checkNotNull(ServerCacheManager.getVarObj("varobj.tumeken_charges".asRSCM())).bits
    fun count(item: InvObj): Int = if (item.id == requireNotNull(variant(item)).second.asRSCM()) 0 else item.vars.getBits(bits).coerceAtMost(MAX)
    fun write(item: InvObj, amount: Int): InvObj {
        require(amount in 0..MAX)
        val variant = requireNotNull(variant(item))
        return item.copy(id = (if (amount == 0) variant.second else variant.first).asRSCM(), vars = item.vars.withBits(bits, amount))
    }
}
