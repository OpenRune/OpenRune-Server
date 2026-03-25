package org.alter.combat.adapter

import org.alter.game.combat.CombatStrategy
import org.alter.game.model.entity.Pawn

/**
 * Adapter that bridges a legacy combat strategy into the new [CombatStrategy] interface
 * so that the new CombatSystem can use old strategy implementations during migration.
 *
 * The old CombatStrategy interface (in game-plugins) has three methods:
 *   - getAttackRange(pawn): Int
 *   - canAttack(pawn, target): Boolean
 *   - attack(pawn, target): Unit
 *
 * The new interface needs getAttackRange, getAttackSpeed, getAttackAnimation, and
 * getHitDelay. The old strategies bake speed, animation, and hit-delay inside their
 * attack() method, so they cannot be extracted without refactoring. Instead, the
 * CombatSystemBootstrap will supply extracted lambdas when constructing this adapter.
 *
 * All legacy methods are accepted as lambdas rather than importing the old interface
 * directly, keeping the content module free of a game-plugins dependency.
 *
 * [canAttack] is exposed as a public method but is NOT part of the new interface — it
 * will be called by a PreAttackEvent listener in CombatSystemBootstrap to cancel attacks
 * when the legacy strategy rejects them.
 */
class LegacyStrategyAdapter(
    private val legacyAttackRange: (Pawn) -> Int,
    private val legacyCanAttack: (Pawn, Pawn) -> Boolean,
    private val attackSpeed: (Pawn) -> Int,
    private val attackAnimation: (Pawn) -> String,
    private val hitDelay: (Pawn, Pawn) -> Int,
) : CombatStrategy {

    override fun getAttackRange(attacker: Pawn): Int = legacyAttackRange(attacker)
    override fun getAttackSpeed(attacker: Pawn): Int = attackSpeed(attacker)
    override fun getAttackAnimation(attacker: Pawn): String = attackAnimation(attacker)
    override fun getHitDelay(attacker: Pawn, target: Pawn): Int = hitDelay(attacker, target)

    /**
     * Delegates to the legacy canAttack check. Not part of the new interface;
     * intended to be called from a PreAttackEvent listener.
     */
    fun canAttack(attacker: Pawn, target: Pawn): Boolean = legacyCanAttack(attacker, target)
}
