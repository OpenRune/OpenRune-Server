package org.rsmod.api.combat.commons.npc

import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player

/** Target-specific melee reach. The combat script still enforces routing and line of sight. */
public fun interface NpcMeleeRangeHook {
    public fun range(player: Player, target: Npc, weaponRange: Int): Int?
}
