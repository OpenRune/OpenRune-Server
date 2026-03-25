package org.alter.game.combat

import org.alter.game.model.entity.Pawn

interface CombatStrategy {
    fun getAttackRange(attacker: Pawn): Int
    fun getAttackSpeed(attacker: Pawn): Int
    fun getAttackAnimation(attacker: Pawn): String   // RSCM name, NOT int
    fun getHitDelay(attacker: Pawn, target: Pawn): Int
}
