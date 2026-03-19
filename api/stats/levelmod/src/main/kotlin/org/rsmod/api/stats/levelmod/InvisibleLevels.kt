package org.rsmod.api.stats.levelmod

import dev.openrune.types.StatType
import jakarta.inject.Inject
import org.rsmod.game.entity.Player

class InvisibleLevels @Inject constructor(mods: Set<InvisibleLevelMod>) {
    private val statMods = mods.groupByTo(HashMap()) { it.stat.id }

    fun get(player: Player, stat: StatType): Int {
        val mods = statMods[stat.id] ?: return 0
        return mods.sumOf { mod -> mod[player] }
    }

    private operator fun InvisibleLevelMod.get(player: Player): Int = player.calculateBoost()
}
