package org.rsmod.api.npc.stat

import kotlin.math.max
import kotlin.math.min
import org.rsmod.api.config.refs.params
import org.rsmod.game.entity.Npc

private const val ATTACK = "stat.attack"
private const val STRENGTH = "stat.strength"
private const val DEFENCE = "stat.defence"
private const val RANGED = "stat.ranged"
private const val MAGIC = "stat.magic"

public fun Npc.stat(stat: String): Int =
    when (stat) {
        ATTACK -> attackLvl
        STRENGTH -> strengthLvl
        DEFENCE -> defenceLvl
        RANGED -> rangedLvl
        MAGIC -> magicLvl
        else -> unsupported(stat)
    }

public fun Npc.statBase(stat: String): Int =
    when (stat) {
        ATTACK -> baseAttackLvl
        STRENGTH -> baseStrengthLvl
        DEFENCE -> baseDefenceLvl
        RANGED -> baseRangedLvl
        MAGIC -> baseMagicLvl
        else -> unsupported(stat)
    }

/**
 * The lowest level [stat] can be reduced to. Only [DEFENCE] can have a minimum, set per npc type
 * through `param.min_defence`; every other stat bottoms out at `0`.
 */
public fun Npc.statMin(stat: String): Int {
    if (stat != DEFENCE) return 0
    return paramOrNull(params.min_defence) ?: 0
}

/**
 * Decreases the npc's current [stat] level.
 *
 * The result never falls below [statMin]. A stat that is already below its minimum is left as is
 * rather than raised.
 *
 * @param constant The fixed amount to subtract from the current stat level.
 * @param percent The percentage (`0`-`100`) of the npc's **base** stat level to subtract.
 */
public fun Npc.statSub(stat: String, constant: Int, percent: Int) {
    require(constant >= 0) { "Constant `$constant` must be positive." }
    require(percent in 0..100) { "Percent must be an integer from 0-100. (0%-100%)" }

    val current = stat(stat)
    val calculated = current - (constant + (statBase(stat) * percent) / 100)
    setStat(stat, max(calculated, min(current, statMin(stat))))
}

/**
 * Decreases the npc's current [stat] level, but never below `base - drain`, so repeated calls do
 * not keep stacking. [statMin] still applies.
 *
 * @see statSub
 */
public fun Npc.statDrain(stat: String, constant: Int, percent: Int) {
    require(constant >= 0) { "Constant `$constant` must be positive." }
    require(percent in 0..100) { "Percent must be an integer from 0-100. (0%-100%)" }

    val base = statBase(stat)
    val drain = constant + (base * percent) / 100
    val current = stat(stat)
    val cappedDrain = current - max(base - drain, current - drain)
    if (cappedDrain > 0) {
        statSub(stat, cappedDrain, 0)
    }
}

/**
 * Restores the npc's [stat] level towards its **base** level by [constant] plus [percent] of the
 * base level. Never exceeds the base level and never lowers the current level.
 */
public fun Npc.statHeal(stat: String, constant: Int, percent: Int) {
    require(constant >= 0) { "Constant `$constant` must be positive." }
    require(percent in 0..100) { "Percent must be an integer from 0-100. (0%-100%)" }

    val base = statBase(stat)
    val current = stat(stat)
    val restored = min(base, current + constant + (base * percent) / 100)
    if (restored > current) {
        setStat(stat, restored)
    }
}

/**
 * Raises the npc's [stat] level by [constant] plus [percent] of the base level, up to
 * `base + boost`. Never lowers the current level.
 */
public fun Npc.statBoost(stat: String, constant: Int, percent: Int) {
    require(constant >= 0) { "Constant `$constant` must be positive." }
    require(percent in 0..100) { "Percent must be an integer from 0-100. (0%-100%)" }

    val base = statBase(stat)
    val boost = constant + (base * percent) / 100
    val current = stat(stat)
    val boosted = min(base + boost, current + boost)
    if (boosted > current) {
        setStat(stat, boosted)
    }
}

private fun Npc.setStat(stat: String, level: Int) {
    when (stat) {
        ATTACK -> attackLvl = level
        STRENGTH -> strengthLvl = level
        DEFENCE -> defenceLvl = level
        RANGED -> rangedLvl = level
        MAGIC -> magicLvl = level
        else -> unsupported(stat)
    }
}

private fun unsupported(stat: String): Nothing =
    error("Npc stat `$stat` is not supported. Hitpoints go through the hit pipeline.")
