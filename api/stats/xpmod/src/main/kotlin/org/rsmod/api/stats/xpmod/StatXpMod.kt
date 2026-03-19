package org.rsmod.api.stats.xpmod

import dev.openrune.types.StatType
import org.rsmod.game.entity.Player

abstract class StatXpMod(private val stat: StatType) : XpMod {
    abstract fun Player.modifier(): Double

    override fun Player.modifier(stat: StatType): Double {
        if (stat == this@StatXpMod.stat) {
            return modifier()
        }
        return 0.0
    }
}
