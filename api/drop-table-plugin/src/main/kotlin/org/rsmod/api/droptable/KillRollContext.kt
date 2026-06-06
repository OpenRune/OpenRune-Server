package org.rsmod.api.droptable

import dtx.core.ArgKey
import dtx.rs.brimstoneRarityDenominator
import org.rsmod.api.area.checker.AreaChecker
import org.rsmod.game.entity.Npc

public object KillRollContext {
    public val npc: ArgKey<Npc?> = ArgKey("killRollNpc", null)
    public val areaChecker: ArgKey<AreaChecker?> = ArgKey("killRollAreaChecker", null)
}

public fun brimstoneKeyRollDenominator(combatLevel: Int, konarTaskBonus: Boolean): Int =
    brimstoneRarityDenominator(combatLevel, konarTaskBonus)
