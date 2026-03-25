package org.alter.combat.adapter

import org.alter.game.model.combat.CombatStyle
import org.alter.game.model.combat.isMelee
import org.alter.game.model.entity.Pawn
import org.alter.game.pluginnew.event.EventListener
import org.alter.game.pluginnew.event.impl.AccuracyRollEvent
import org.alter.game.pluginnew.event.impl.MaxHitRollEvent

/**
 * Adapter that bridges the old combat formula singletons into the new event
 * pipeline as priority-0 listeners on [AccuracyRollEvent] and [MaxHitRollEvent].
 *
 * The old formulas return a single accuracy probability (0.0–1.0) rather than
 * separate attack/defence rolls, so the adapter bypasses the roll comparison
 * entirely by setting [AccuracyRollEvent.hitOverride] directly.
 *
 * All formula logic is accepted as lambdas rather than importing the old formula
 * classes directly, keeping the content module free of a game-plugins dependency.
 *
 * This class does NOT extend [org.alter.game.pluginnew.PluginEvent] because it
 * requires constructor arguments and therefore cannot be auto-discovered by
 * [org.alter.game.pluginnew.PluginManager] (which requires no-arg constructors).
 * Instead, listeners are registered immediately via [EventListener.on] when
 * [register] is called. The [CombatSystemBootstrap] is responsible for
 * constructing and registering this adapter.
 *
 * The [meleeActive], [rangedActive], and [magicActive] flags allow individual
 * styles to be deactivated at runtime (e.g. when a per-style migration is
 * complete and a new dedicated listener has been registered at a higher priority).
 */
class LegacyFormulaAdapter(
    private val meleeAccuracy: (Pawn, Pawn) -> Double,
    private val meleeMaxHit: (Pawn, Pawn) -> Int,
    private val rangedAccuracy: (Pawn, Pawn) -> Double,
    private val rangedMaxHit: (Pawn, Pawn) -> Int,
    private val magicAccuracy: (Pawn, Pawn) -> Double,
    private val magicMaxHit: (Pawn, Pawn) -> Int,
) {
    var meleeActive = true
    var rangedActive = true
    var magicActive = true

    /**
     * Registers all six listeners (accuracy + max-hit for each of melee,
     * ranged, and magic) with the [EventManager][org.alter.game.pluginnew.event.EventManager].
     *
     * Must be called exactly once, typically from [CombatSystemBootstrap.init].
     */
    fun register() {
        // Melee accuracy
        EventListener.on<AccuracyRollEvent> {
            where { combatStyle.isMelee() && meleeActive }
            priority(0)
            then {
                val accuracy = meleeAccuracy(attacker, target)
                hitOverride = Math.random() < accuracy
            }
        }

        // Melee max hit
        EventListener.on<MaxHitRollEvent> {
            where { combatStyle.isMelee() && meleeActive }
            priority(0)
            then { maxHit = meleeMaxHit(attacker, target) }
        }

        // Ranged accuracy
        EventListener.on<AccuracyRollEvent> {
            where { combatStyle == CombatStyle.RANGED && rangedActive }
            priority(0)
            then {
                val accuracy = rangedAccuracy(attacker, target)
                hitOverride = Math.random() < accuracy
            }
        }

        // Ranged max hit
        EventListener.on<MaxHitRollEvent> {
            where { combatStyle == CombatStyle.RANGED && rangedActive }
            priority(0)
            then { maxHit = rangedMaxHit(attacker, target) }
        }

        // Magic accuracy
        EventListener.on<AccuracyRollEvent> {
            where { combatStyle == CombatStyle.MAGIC && magicActive }
            priority(0)
            then {
                val accuracy = magicAccuracy(attacker, target)
                hitOverride = Math.random() < accuracy
            }
        }

        // Magic max hit
        EventListener.on<MaxHitRollEvent> {
            where { combatStyle == CombatStyle.MAGIC && magicActive }
            priority(0)
            then { maxHit = magicMaxHit(attacker, target) }
        }
    }
}
