package org.rsmod.api.player.vars

import kotlin.math.max
import org.rsmod.game.entity.Player
import org.rsmod.game.movement.MoveSpeed

// UNVERIFIED: whether the armed state survives logout; it is saved with the player (guess).
private var Player.runAutoEnableArmed by boolVarp("varp.runenergy_autoenable_armed")
private val Player.runAutoEnableThreshold by intVarBit("varbit.runenergy_autoenable")

public fun Player.armRunAutoEnable() {
    runAutoEnableArmed = true
}

public fun Player.disarmRunAutoEnable() {
    runAutoEnableArmed = false
}

// TODO: interaction routes (npc, loc and obj ops) may also re-enable run; only move clicks are
//  confirmed to.
// UNVERIFIED: a ctrl-click that re-enables run walks that click, since ctrl inverts the new run
//  mode; later clicks run (guess).
/**
 * The "Energy threshold to re-enable running" setting is checked only when the player asks to move,
 * never while they stand still, so it can't close an open dialogue on its own. Run is switched on
 * before the request's first step is taken.
 */
public fun Player.autoEnableRunOnMove() {
    if (!runAutoEnableArmed || varMoveSpeed != MoveSpeed.Walk) {
        return
    }
    val thresholdPercent = runAutoEnableThreshold
    if (thresholdPercent <= 0) {
        return
    }
    if (runEnergy < max(MIN_RUN_ENERGY, thresholdPercent * ENERGY_PER_PERCENT)) {
        return
    }
    runAutoEnableArmed = false
    setActiveMoveSpeed(MoveSpeed.Run)
}

private const val ENERGY_PER_PERCENT = 100
private const val MIN_RUN_ENERGY = 100
