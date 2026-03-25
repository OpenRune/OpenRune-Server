package org.alter.game.pluginnew.event.impl

import org.alter.game.combat.DisengageReason
import org.alter.game.model.combat.CombatStyle
import org.alter.game.model.entity.Pawn
import org.alter.game.pluginnew.event.Event

abstract class CombatEvent(
    open val attacker: Pawn,
    open val target: Pawn,
    open val combatStyle: CombatStyle
) : Event

class CombatEngageEvent(
    override val attacker: Pawn,
    override val target: Pawn,
    override val combatStyle: CombatStyle
) : CombatEvent(attacker, target, combatStyle)

class CombatDisengageEvent(
    override val attacker: Pawn,
    override val target: Pawn,
    override val combatStyle: CombatStyle,
    val reason: DisengageReason
) : CombatEvent(attacker, target, combatStyle)

class PreAttackEvent(
    override val attacker: Pawn,
    override val target: Pawn,
    override val combatStyle: CombatStyle,
    var cancelled: Boolean = false,
    var cancelReason: String? = null
) : CombatEvent(attacker, target, combatStyle)

class AccuracyRollEvent(
    override val attacker: Pawn,
    override val target: Pawn,
    override val combatStyle: CombatStyle,
    var attackRoll: Int,
    var defenceRoll: Int,
    var hitOverride: Boolean? = null
) : CombatEvent(attacker, target, combatStyle)

class MaxHitRollEvent(
    override val attacker: Pawn,
    override val target: Pawn,
    override val combatStyle: CombatStyle,
    var maxHit: Int,
    val landed: Boolean
) : CombatEvent(attacker, target, combatStyle)

class DamageCalculatedEvent(
    override val attacker: Pawn,
    override val target: Pawn,
    override val combatStyle: CombatStyle,
    var damage: Int,
    val landed: Boolean
) : CombatEvent(attacker, target, combatStyle)

class HitAppliedEvent(
    override val attacker: Pawn,
    override val target: Pawn,
    override val combatStyle: CombatStyle,
    val damage: Int,
    val landed: Boolean
) : CombatEvent(attacker, target, combatStyle)

class PostAttackEvent(
    override val attacker: Pawn,
    override val target: Pawn,
    override val combatStyle: CombatStyle,
    val damage: Int,
    val landed: Boolean
) : CombatEvent(attacker, target, combatStyle)
