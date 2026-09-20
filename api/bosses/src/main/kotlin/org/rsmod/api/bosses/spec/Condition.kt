package org.rsmod.api.bosses.spec

import org.rsmod.game.entity.Npc

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
    data class Custom(val test: (Npc) -> Boolean) : Condition
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
