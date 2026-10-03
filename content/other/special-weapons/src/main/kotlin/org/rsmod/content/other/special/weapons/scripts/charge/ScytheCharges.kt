package org.rsmod.content.other.special.weapons.scripts.charge

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import org.rsmod.game.inv.InvObj
import org.rsmod.utils.bits.getBits
import org.rsmod.utils.bits.withBits

internal object ScytheCharges {
    const val MAX = 20_000
    const val BATCH = 100
    const val BLOOD_RUNES = 200
    val variants = listOf("", "_or", "_bl").map { suffix ->
        "obj.scythe_of_vitur$suffix" to "obj.scythe_of_vitur_uncharged$suffix"
    }
    private val bits get() = checkNotNull(ServerCacheManager.getVarObj("varobj.tumeken_charges".asRSCM())).bits
    fun variant(item: InvObj) = variants.firstOrNull { item.id == it.first.asRSCM() || item.id == it.second.asRSCM() }
    fun count(item: InvObj): Int {
        val variant = requireNotNull(variant(item))
        return if (item.id == variant.second.asRSCM()) 0 else item.vars.getBits(bits).coerceIn(0, MAX)
    }
    fun withCharges(item: InvObj, count: Int): InvObj {
        require(count in 0..MAX)
        val variant = requireNotNull(variant(item))
        return item.copy(id = (if (count == 0) variant.second else variant.first).asRSCM(), vars = item.vars.withBits(bits, count))
    }
    fun batches(current: Int, runes: Int, vials: Int): Int = minOf((MAX - current) / BATCH, runes / BLOOD_RUNES, vials).coerceAtLeast(0)
}
