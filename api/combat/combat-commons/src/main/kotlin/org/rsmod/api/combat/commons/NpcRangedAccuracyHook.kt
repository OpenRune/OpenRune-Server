package org.rsmod.api.combat.commons

import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player

/**
 * Multiplies a player's ranged accuracy roll against [npc]. Registered hooks are combined by taking
 * the highest multiplier; no hook means `1.0` (no change).
 */
public fun interface NpcRangedAccuracyHook {
    public fun multiplier(player: Player, npc: Npc): Double
}
