package org.rsmod.api.death

import org.rsmod.game.entity.Player

public fun interface PvPAttackValidateHook {
    public fun validate(attacker: Player, target: Player): PvPAttackValidateResult
}

public sealed class PvPAttackValidateResult {
    public data object Pass : PvPAttackValidateResult()

    /** Singles-plus allows PvP to displace an NPC, while retaining one-player combat checks. */
    public data object BypassSingleWayPvnRestriction : PvPAttackValidateResult()

    public data class Deny(val message: String) : PvPAttackValidateResult()
}
