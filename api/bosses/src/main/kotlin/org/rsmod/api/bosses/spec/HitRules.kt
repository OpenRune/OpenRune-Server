package org.rsmod.api.bosses.spec

/**
 * Runs [effect] when a player's hit lands on the boss (as the hitsplat shows), with the attacker
 * as the target. [withObj] limits it to hits whose secondary obj (the spell or ammo) is one of
 * those; [requires] is checked against the attacker.
 */
data class HitReaction(
    val effect: Effect,
    val withObj: List<String> = emptyList(),
    val requires: Condition = Condition.Always,
)

/**
 * A rule for player hits about to land on the boss. Rules are checked in declaration order
 * against the attacker and only the first match applies its [actions].
 */
data class IncomingRule(val condition: Condition, val actions: List<IncomingAction>)

sealed interface IncomingAction {
    /** When set, the action only applies to hits of this style. */
    val style: HitType?

    data class Cap(val max: Int, override val style: HitType? = null) : IncomingAction

    data class ScalePercent(val percent: Int, override val style: HitType? = null) : IncomingAction

    /**
     * Raises a hit below [percent] of the attacker's max hit to a roll between that floor and the
     * max. Only [HitType.Ranged] and [HitType.Melee] have a player max hit to measure against.
     */
    data class FloorPercentOfMaxHit(val percent: Int, override val style: HitType) : IncomingAction

    /** Runs [effect] with the attacker as the target, before the hit's damage is settled. */
    data class Run(val effect: Effect) : IncomingAction {
        override val style: HitType?
            get() = null
    }
}
