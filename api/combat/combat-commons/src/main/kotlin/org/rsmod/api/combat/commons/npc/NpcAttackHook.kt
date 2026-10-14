package org.rsmod.api.combat.commons.npc

import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player

public fun interface NpcAttackHook {
    public fun onAttack(npc: Npc, target: Player)

    public fun replacesAttack(npc: Npc, target: Player): Boolean = false
}
