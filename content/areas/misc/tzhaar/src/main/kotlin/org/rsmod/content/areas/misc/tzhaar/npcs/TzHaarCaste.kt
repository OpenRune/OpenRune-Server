package org.rsmod.content.areas.misc.tzhaar.npcs

import org.rsmod.api.player.stat.baseAgilityLvl
import org.rsmod.api.player.stat.baseAttackLvl
import org.rsmod.api.player.stat.baseCraftingLvl
import org.rsmod.api.player.stat.baseDefenceLvl
import org.rsmod.api.player.stat.baseMagicLvl
import org.rsmod.api.player.stat.basePrayerLvl
import org.rsmod.api.player.stat.baseSmithingLvl
import org.rsmod.api.player.stat.baseStrengthLvl
import org.rsmod.game.entity.Player

internal enum class TzHaarCaste(val title: String, val description: String) {
    Mej("JalYt-Mej", "That what you are... you user of mystic powers no?"),
    Ket("JalYt-Ket", "That what you are... you tough and strong no?"),
    Xil("JalYt-Xil", "That what you are... you agile and quick no?"),
    Hur("JalYt-Hur", "That what you are... you skilled at making things no?");

    companion object {
        // https://oldschool.runescape.wiki/w/TzHaar#Titles
        // TODO(content): [verify:in_game] base or boosted levels for the caste sums
        // TODO(content): [system:tzhaar_honorifics] Xo (fire cape shown) and Kal (TzKal-Zuk)
        fun of(player: Player): TzHaarCaste {
            val sums =
                listOf(
                    Mej to player.baseMagicLvl + player.basePrayerLvl,
                    Ket to player.baseStrengthLvl + player.baseDefenceLvl,
                    Xil to player.baseAttackLvl + player.baseAgilityLvl,
                    Hur to player.baseCraftingLvl + player.baseSmithingLvl,
                )
            return sums.maxBy { it.second }.first
        }
    }
}

internal fun Player.tzhaarName(): String = "${TzHaarCaste.of(this).title}-$displayName"
