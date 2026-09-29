package org.rsmod.api.bosses.spec

import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player

sealed interface Condition {
    data class HpBelow(val fraction: Double) : Condition
    data class HpExact(val hp: Int) : Condition
    data class IncomingHitDamageAtLeast(val damage: Int) : Condition
    data class PlayerEnterRange(val tiles: Int) : Condition
    data class EveryNTicks(val n: Int) : Condition
    data class OnPhaseTick(val n: Int) : Condition
    data class TargetPraying(val type: HitType) : Condition
    data class InPhase(val phase: String) : Condition
    data class AbilityUsed(val ability: String) : Condition
    data class VarnIn(val varn: String, val range: IntRange) : Condition
    data class LastAbility(val ability: String) : Condition
    data class TargetWithin(val distance: Int, val of: TargetExpr.Single = TargetExpr.Centre) : Condition

    /**
     * The target's bearing from the caster's centre is within [halfArc] of the bearing stored in
     * [bearingVarn] turned by [offset] (e.g. `offset = 1024` for "behind" that bearing).
     */
    data class TargetInArc(val bearingVarn: String, val offset: Int, val halfArc: Int) : Condition
    data class Custom(val test: (npc: Npc, target: Player?) -> Boolean) : Condition
    data object OnSpawn : Condition
    data object OnDeath : Condition
    data object Always : Condition
    data object WithinMeleeRange : Condition
    data class Not(val c: Condition) : Condition
    data class And(val a: Condition, val b: Condition) : Condition
    data class Or(val a: Condition, val b: Condition) : Condition

    infix fun and(other: Condition): Condition = And(this, other)
    infix fun or(other: Condition): Condition = Or(this, other)
}
