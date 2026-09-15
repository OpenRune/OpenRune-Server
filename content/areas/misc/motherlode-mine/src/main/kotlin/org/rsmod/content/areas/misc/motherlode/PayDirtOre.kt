package org.rsmod.content.areas.misc.motherlode

import org.rsmod.api.attr.AttributeKey
import org.rsmod.api.random.GameRandom
import org.rsmod.api.utils.skills.SkillingSuccessRate

internal enum class PayDirtOre(
    val obj: String,
    val level: Int,
    val xp: Double,
    private val low: Int,
    private val high: Int,
) {
    Nugget(MotherlodeMine.NUGGET, level = 30, xp = 0.0, low = 7, high = 7),
    Runite("obj.runite_ore", level = 85, xp = 75.0, low = -20, high = 5),
    Adamantite("obj.adamantite_ore", level = 70, xp = 45.0, low = -90, high = 50),
    Mithril("obj.mithril_ore", level = 55, xp = 30.0, low = -19, high = 90),
    Gold("obj.gold_ore", level = 40, xp = 15.0, low = -40, high = 126),
    Coal("obj.coal", level = 30, xp = 15.0, low = 0, high = 0);

    val sackKey: AttributeKey<Int> =
        AttributeKey(persistenceKey = "motherlode_sack_${name.lowercase()}")

    companion object {
        private const val MAX_LEVEL = 99

        /** Rolls from the top tier down; anything that fails every roll becomes coal. */
        fun roll(miningLevel: Int, random: GameRandom): PayDirtOre {
            val level = miningLevel.coerceIn(1, MAX_LEVEL)
            for (ore in entries) {
                if (ore == Coal) {
                    break
                }
                if (level < ore.level) {
                    continue
                }
                val rate = SkillingSuccessRate.successRate(ore.low, ore.high, level, MAX_LEVEL)
                if (rate > random.randomDouble()) {
                    return ore
                }
            }
            return Coal
        }

        fun fromOrdinal(ordinal: Int): PayDirtOre = entries.getOrElse(ordinal) { Coal }
    }
}
