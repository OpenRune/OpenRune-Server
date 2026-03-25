package org.alter.game.combat

import org.alter.game.model.combat.CombatStyle
import org.alter.game.model.entity.Pawn
import org.alter.game.pluginnew.event.IEventManager
import org.alter.game.pluginnew.event.impl.*
import kotlin.random.Random

/**
 * Central combat engine that tracks active combats and processes the
 * event pipeline each game tick.
 */
class CombatSystem(private val eventManager: IEventManager) {

    private val activeCombats = mutableMapOf<Pawn, CombatState>()

    /**
     * Called every world tick. Removes dead/expired entries, then processes
     * attacks for any attacker whose delay has elapsed.
     */
    fun processTick() {
        // Remove dead or expired combats
        activeCombats.entries.removeAll { (pawn, state) ->
            !pawn.isAlive() || state.isExpired()
        }

        // Process each attacker (snapshot to avoid ConcurrentModification)
        for ((attacker, state) in activeCombats.toMap()) {
            if (state.attackDelayReady()) {
                processAttack(attacker, state)
            }
            state.tickDown()
        }
    }

    /**
     * Start combat between [attacker] and [target] using the given [strategy] and [combatStyle].
     */
    fun engage(attacker: Pawn, target: Pawn, strategy: CombatStrategy, combatStyle: CombatStyle) {
        val state = CombatState(target, strategy.getAttackSpeed(attacker), strategy, combatStyle)
        activeCombats[attacker] = state
        eventManager.postAndWait(CombatEngageEvent(attacker, target, combatStyle))
    }

    /**
     * End combat for [attacker] with the given [reason].
     */
    fun disengage(attacker: Pawn, reason: DisengageReason = DisengageReason.MANUAL) {
        val state = activeCombats.remove(attacker) ?: return
        eventManager.postAndWait(CombatDisengageEvent(attacker, state.target, state.combatStyle, reason))
    }

    fun isInCombat(pawn: Pawn): Boolean = activeCombats.containsKey(pawn)

    fun getState(pawn: Pawn): CombatState? = activeCombats[pawn]

    /**
     * Directly set the attack delay for an attacker. Intended for tests.
     */
    fun setAttackDelay(attacker: Pawn, delay: Int) {
        activeCombats[attacker]?.attackDelay = delay
    }

    // ---------------------------------------------------------------
    // Core attack pipeline
    // ---------------------------------------------------------------

    private fun processAttack(attacker: Pawn, state: CombatState) {
        val target = state.target
        val style = state.combatStyle
        val strategy = state.strategy

        // 1. PreAttackEvent -- can be cancelled
        val preAttack = PreAttackEvent(attacker, target, style, strategy)
        eventManager.postAndWait(preAttack)
        if (preAttack.cancelled) return

        // 2. AccuracyRollEvent -- listeners populate rolls
        val accuracyEvent = AccuracyRollEvent(attacker, target, style, attackRoll = 0, defenceRoll = 0)
        eventManager.postAndWait(accuracyEvent)

        val landed = accuracyEvent.hitOverride ?: (accuracyEvent.attackRoll > accuracyEvent.defenceRoll)

        // 3. MaxHitRollEvent -- listeners set maxHit
        val maxHitEvent = MaxHitRollEvent(attacker, target, style, maxHit = 0, landed = landed)
        eventManager.postAndWait(maxHitEvent)

        // 4. DamageCalculatedEvent -- compute damage then let listeners modify
        val rawDamage = if (landed && maxHitEvent.maxHit > 0) Random.nextInt(maxHitEvent.maxHit + 1) else 0
        val damageEvent = DamageCalculatedEvent(attacker, target, style, damage = rawDamage, landed = landed)
        eventManager.postAndWait(damageEvent)

        // 5. PostAttackEvent -- XP, timer resets, ammo, etc.
        val postAttack = PostAttackEvent(attacker, target, style, strategy, damage = damageEvent.damage, landed = landed)
        eventManager.postAndWait(postAttack)

        // Reset state for next attack cycle
        state.attackDelay = strategy.getAttackSpeed(attacker)
        state.resetTimeout()
    }
}
