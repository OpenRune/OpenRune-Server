package org.rsmod.api.droptable

import dtx.core.ArgMap
import dtx.core.RollResult
import dtx.core.Rollable
import dtx.core.RollableHooks
import dtx.impl.chance.ChanceRollable
import kotlin.math.floor
import org.rsmod.game.entity.Player

/**
 * Chance roll whose effective rate grows by [boostPercent] (e.g. +50 → 1.5×). Implemented by
 * shrinking the denominator: `floor(denom * 100 / (100 + boost))`.
 */
public class RateBoostChanceRollable(
    private val numerator: Int,
    private val denominator: Int,
    override val rollable: Rollable<Player, DropRollItem>,
    private val boostPercent: (Player, ArgMap) -> Int,
    private val hooks: RollableHooks<Player, DropRollItem> = RollableHooks.Default(),
) : ChanceRollable<Player, DropRollItem>, RollableHooks<Player, DropRollItem> by hooks {

    override val chance: Double = numerator.toDouble() / denominator.toDouble() * 100.0

    override fun chanceFor(target: Player, otherArgs: ArgMap): Double {
        val boost = boostPercent(target, otherArgs).coerceAtLeast(0)
        if (boost == 0) return chance
        val adjustedDenom = floor(denominator * 100.0 / (100.0 + boost)).toInt().coerceAtLeast(1)
        return numerator.toDouble() / adjustedDenom.toDouble() * 100.0
    }

    override fun selectResult(target: Player, otherArgs: ArgMap): RollResult<DropRollItem> =
        rollable.roll(target, otherArgs)
}
