package org.rsmod.api.player.worn

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import org.rsmod.game.inv.InvObj
import org.rsmod.utils.bits.getBits
import org.rsmod.utils.bits.withBits

public object AyakCharges {
    public const val MAX: Int = 50_000
    public const val CHARGED: String = "obj.eye_of_ayak"
    public const val EMPTY: String = "obj.eye_of_ayak_uncharged"
    public enum class Source(public val recipe: List<Pair<String, Int>>) {
        Runes(listOf("obj.deathrune" to 2, "obj.chaosrune" to 1)),
        Tears(listOf("obj.demon_tear" to 1)),
    }
    public fun accepts(item: InvObj?): Boolean = item != null && (item.id == CHARGED.asRSCM() || item.id == EMPTY.asRSCM())
    private fun bits(symbol: String): IntRange = checkNotNull(ServerCacheManager.getVarObj(symbol.asRSCM())).bits
    public fun count(item: InvObj): Int {
        require(accepts(item))
        return if (item.id == EMPTY.asRSCM()) 0 else item.vars.getBits(bits("varobj.ayak_charges")).coerceAtMost(MAX)
    }
    public fun source(item: InvObj): Source = if (item.vars.getBits(bits("varobj.ayak_tear_charges")) == 1) Source.Tears else Source.Runes
    public fun write(item: InvObj, count: Int, source: Source = source(item)): InvObj {
        require(accepts(item) && count in 0..MAX)
        val vars = item.vars.withBits(bits("varobj.ayak_charges"), count)
            .withBits(bits("varobj.ayak_tear_charges"), if (count > 0 && source == Source.Tears) 1 else 0)
        return item.copy(id = (if (count == 0) EMPTY else CHARGED).asRSCM(), vars = vars)
    }
    public fun baseMaxHit(magic: Int): Int = (magic / 3 - 6).coerceAtLeast(1)
}
