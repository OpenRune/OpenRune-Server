package org.rsmod.api.player.worn

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import org.rsmod.game.inv.InvObj
import org.rsmod.utils.bits.getBits
import org.rsmod.utils.bits.withBits

/** Uses the existing three per-item blowpipe varobjs; neither quiver ammo nor player attrs. */
public object BlowpipeCharges {
    public const val MAX: Int = 16_383
    public val variants: List<Pair<String, String>> = listOf(
        "obj.toxic_blowpipe_loaded" to "obj.toxic_blowpipe",
        "obj.toxic_blowpipe_loaded_ornament" to "obj.toxic_blowpipe_ornament",
    )
    public val darts: List<String> = listOf("bronze", "iron", "steel", "black", "mithril", "adamant", "rune", "dragon", "amethyst").map { "obj.${it}_dart" }
    public data class Contents(public val dart: Int, public val count: Int, public val scales: Int) {
        public val ready: Boolean get() = dart in 1..darts.size && count > 0 && scales > 0
        public val ammunition: InvObj? get() = if (dart in 1..darts.size && count > 0) InvObj(darts[dart - 1]) else null
    }
    public fun variant(item: InvObj?): Pair<String, String>? = item?.let { obj -> variants.firstOrNull { obj.id == it.first.asRSCM() || obj.id == it.second.asRSCM() } }
    private fun bits(name: String): IntRange = checkNotNull(ServerCacheManager.getVarObj("varobj.snakeboss_blowpipe_$name".asRSCM())).bits
    public fun read(item: InvObj?): Contents {
        if (item == null || variant(item) == null) return Contents(0, 0, 0)
        return Contents(item.vars.getBits(bits("darttype")), item.vars.getBits(bits("dartcount")), item.vars.getBits(bits("flakes")))
    }
    public fun write(item: InvObj, value: Contents): InvObj {
        val variant = requireNotNull(variant(item))
        require(value.dart in 0..darts.size && value.count in 0..MAX && value.scales in 0..MAX)
        require(value.count == 0 || value.dart != 0)
        val vars = item.vars.withBits(bits("darttype"), if (value.count == 0) 0 else value.dart)
            .withBits(bits("dartcount"), value.count).withBits(bits("flakes"), value.scales)
        return item.copy(id = (if (value.count == 0 && value.scales == 0) variant.second else variant.first).asRSCM(), vars = vars)
    }
    public fun consume(item: InvObj, conserveDart: Boolean, consumeScale: Boolean): InvObj {
        val before = read(item)
        require(before.ready)
        return write(item, before.copy(count = before.count - if (conserveDart) 0 else 1, scales = before.scales - if (consumeScale) 1 else 0))
    }
}
