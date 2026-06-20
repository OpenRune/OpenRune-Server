package org.rsmod.api.death

import org.rsmod.game.entity.Player

public fun interface PvPAttackValidateHook {
    public fun validate(attacker: Player, target: Player): PvPAttackValidateResult
}

public sealed class PvPAttackValidateResult {
    public data object Pass : PvPAttackValidateResult()

    public data class Deny(val message: String) : PvPAttackValidateResult()
}
