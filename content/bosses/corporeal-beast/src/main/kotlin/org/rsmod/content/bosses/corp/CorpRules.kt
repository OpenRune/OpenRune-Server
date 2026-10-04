package org.rsmod.content.bosses.corp

import org.rsmod.map.CoordGrid

internal object CorpRules {
    const val BOSS = "npc.corp_beast"
    const val CORE = "npc.dark_core"
    val LOBBY = CoordGrid(2966, 4254, 2)
    fun inRoom(tile: CoordGrid, origin: CoordGrid): Boolean = tile.level == origin.level &&
        tile.x in 2976..3009 && tile.z in (origin.z - 16)..(origin.z + 15)
    fun style(meleeRange: Boolean, roll: Int): Int = if (meleeRange && roll < 40) 0 else
        1 + ((roll - if (meleeRange) 40 else 0) * 3 / if (meleeRange) 60 else 100).coerceIn(0, 2)
    fun splashMaximum(centre: CoordGrid, target: CoordGrid, main: Boolean): Int {
        if (centre.level != target.level || kotlin.math.abs(centre.x - target.x) > 1 || kotlin.math.abs(centre.z - target.z) > 1) return 0
        return if (centre == target) { if (main) 40 else 30 } else { if (main) 30 else 20 }
    }
    fun meleeMax(strength: Int): Int = ((strength.coerceAtLeast(0) + 8) * 64 + 320) / 640
    fun crowdHeal(players: Int): Int = if (players >= 8) players * 5 else 0
}
