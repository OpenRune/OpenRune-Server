package org.rsmod.api.death

import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player

public fun interface NpcAttackValidateHook {
    public fun validate(player: Player, npc: Npc): NpcAttackValidateResult
}

public sealed class NpcAttackValidateResult {
    public data object Pass : NpcAttackValidateResult()

    public data class Deny(val message: String) : NpcAttackValidateResult()

    /**
     * Allow attacking this npc in single-way combat even if the player is already engaged in PvN
     * combat (superior slayer monsters).
     */
    public data object BypassSingleWayPvnRestriction : NpcAttackValidateResult()
}
