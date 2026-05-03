package org.rsmod.api.stats.xpmod

import dev.openrune.types.StatType
import jakarta.inject.Inject
import org.rsmod.game.entity.Player

class XpModifiers @Inject constructor(private val mods: Set<XpMod>) {
    fun get(player: Player, stat: String): Double = 1.0 + mods.sumOf { it.modifier(player, stat) }

    private fun XpMod.modifier(player: Player, stat: String): Double = player.modifier(stat)
}
