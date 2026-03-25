package org.alter.game.combat

import org.alter.game.model.entity.Pawn

class TestCombatStrategy : CombatStrategy {
    override fun getAttackRange(attacker: Pawn): Int = 1
    override fun getAttackSpeed(attacker: Pawn): Int = 4
    override fun getAttackAnimation(attacker: Pawn): String = "anims.punch"
    override fun getHitDelay(attacker: Pawn, target: Pawn): Int = 1
}
