package org.alter.game.combat

import org.alter.game.model.combat.CombatStyle
import org.alter.game.model.entity.Pawn

data class CombatState(
    val target: Pawn,
    var attackDelay: Int,
    var strategy: CombatStrategy,
    var combatStyle: CombatStyle
) {
    private var ticksSinceLastAttack = 0
    private val combatTimeout = 17

    fun attackDelayReady(): Boolean = attackDelay <= 0
    fun tickDown() { attackDelay--; ticksSinceLastAttack++ }
    fun isExpired(): Boolean = ticksSinceLastAttack >= combatTimeout
    fun resetTimeout() { ticksSinceLastAttack = 0 }
}
