package org.rsmod.content.areas.misc.motherlode

import org.rsmod.api.player.vars.VarPlayerIntMapSetter
import org.rsmod.api.player.vars.boolVarBit
import org.rsmod.api.player.vars.intVarBit
import org.rsmod.game.entity.Player

private const val MAX_SACK_TRANSMIT = 255
private const val MAX_HELD_PAYDIRT = 28

private var Player.sackTransmit by intVarBit("varbit.motherlode_sack_transmit")

internal var Player.hasUpperLevel by boolVarBit("varbit.motherlode_upper_level_unlocked")

internal var Player.hasUpperHopper by boolVarBit("varbit.motherlode_upper_hopper_unlocked")

internal var Player.hasLargerSack by boolVarBit("varbit.motherlode_biggersack")

internal var Player.onUpperLevel by boolVarBit("varbit.motherlode_in_restricted_area")

internal val Player.sackCapacity: Int
    get() = if (hasLargerSack) MotherlodeMine.LARGER_SACK_CAPACITY else MotherlodeMine.SACK_CAPACITY

internal fun Player.sackCount(ore: PayDirtOre): Int = vars[ore.sackVarbit]

internal fun Player.setSackCount(ore: PayDirtOre, count: Int) {
    setVar(ore.sackVarbit, count.coerceIn(0, MAX_SACK_TRANSMIT))
}

internal val Player.sackTotal: Int
    get() = PayDirtOre.entries.sumOf { sackCount(it) }

internal fun Player.heldPayDirtCount(ore: PayDirtOre): Int = vars[ore.heldVarbit]

internal val Player.heldPayDirtTotal: Int
    get() = PayDirtOre.entries.sumOf { heldPayDirtCount(it) }

internal fun Player.addHeldPayDirt(ore: PayDirtOre) {
    val count = heldPayDirtCount(ore)
    if (count < MAX_HELD_PAYDIRT) {
        setVar(ore.heldVarbit, count + 1)
    }
}

internal fun Player.clearHeldPayDirt() {
    for (ore in PayDirtOre.entries) {
        setVar(ore.heldVarbit, 0)
    }
}

internal val Player.cleaningPayDirtTotal: Int
    get() = PayDirtOre.entries.sumOf { vars[it.cleaningVarbit] }

/**
 * Moves up to [count] mined pay-dirt into the washing machine, returning how many were moved. Fewer
 * can be moved than asked for when the player picked pay-dirt up from elsewhere.
 */
internal fun Player.movePayDirtToMachine(count: Int): Int {
    var remaining = count
    for (ore in PayDirtOre.entries) {
        if (remaining == 0) {
            break
        }
        val held = heldPayDirtCount(ore)
        if (held == 0) {
            continue
        }
        val moved = minOf(held, remaining)
        setVar(ore.heldVarbit, held - moved)
        addCleaningPayDirt(ore, moved)
        remaining -= moved
    }
    return count - remaining
}

internal fun Player.addCleaningPayDirt(ore: PayDirtOre, count: Int = 1) {
    setVar(ore.cleaningVarbit, vars[ore.cleaningVarbit] + count)
}

/** Removes [count] pay-dirt from the machine and returns the ore each one washed into. */
internal fun Player.takeCleanedPayDirt(count: Int): List<PayDirtOre> {
    val cleaned = mutableListOf<PayDirtOre>()
    for (ore in PayDirtOre.entries) {
        val pending = vars[ore.cleaningVarbit]
        if (pending == 0) {
            continue
        }
        val taken = minOf(pending, count - cleaned.size)
        setVar(ore.cleaningVarbit, pending - taken)
        repeat(taken) { cleaned += ore }
        if (cleaned.size == count) {
            break
        }
    }
    return cleaned
}

internal fun Player.syncMotherlodeVars() {
    sackTransmit = sackTotal.coerceAtMost(MAX_SACK_TRANSMIT)
}

private fun Player.setVar(varbit: String, value: Int) {
    VarPlayerIntMapSetter.set(this, varbit, value)
}
