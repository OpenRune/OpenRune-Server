package org.rsmod.content.areas.misc.motherlode

import org.rsmod.api.attr.AttributeKey
import org.rsmod.api.player.vars.boolVarBit
import org.rsmod.api.player.vars.intVarBit
import org.rsmod.game.entity.Player

internal val UPPER_LEVEL_UNLOCKED = AttributeKey<Boolean>(persistenceKey = "motherlode_upper_level")
internal val UPPER_HOPPER_UNLOCKED = AttributeKey<Boolean>(persistenceKey = "motherlode_upper_hopper")
internal val LARGER_SACK_UNLOCKED = AttributeKey<Boolean>(persistenceKey = "motherlode_larger_sack")
internal val ON_UPPER_LEVEL = AttributeKey<Boolean>(persistenceKey = "motherlode_on_upper_level")

/** Ore rolled for each pay-dirt in the inventory; the ore is decided when the pay-dirt is mined. */
internal val HELD_PAYDIRT = AttributeKey<MutableList<Int>>(persistenceKey = "motherlode_held_paydirt")
internal val CLEANING_PAYDIRT =
    AttributeKey<MutableList<Int>>(persistenceKey = "motherlode_cleaning_paydirt")

private var Player.sackTransmit by intVarBit("varbit.motherlode_sack_transmit")
private var Player.largerSackTransmit by boolVarBit("varbit.motherlode_biggersack")
private var Player.restrictedAreaTransmit by boolVarBit("varbit.motherlode_in_restricted_area")

internal val Player.hasUpperLevel: Boolean
    get() = attr[UPPER_LEVEL_UNLOCKED] == true

internal val Player.hasUpperHopper: Boolean
    get() = attr[UPPER_HOPPER_UNLOCKED] == true

internal val Player.hasLargerSack: Boolean
    get() = attr[LARGER_SACK_UNLOCKED] == true

internal val Player.sackCapacity: Int
    get() = if (hasLargerSack) MotherlodeMine.LARGER_SACK_CAPACITY else MotherlodeMine.SACK_CAPACITY

internal var Player.onUpperLevel: Boolean
    get() = attr[ON_UPPER_LEVEL] == true
    set(value) {
        attr[ON_UPPER_LEVEL] = value
        restrictedAreaTransmit = value
    }

internal fun Player.sackCount(ore: PayDirtOre): Int = attr[ore.sackKey] ?: 0

internal fun Player.setSackCount(ore: PayDirtOre, count: Int) {
    if (count <= 0) {
        attr.remove(ore.sackKey)
    } else {
        attr[ore.sackKey] = count
    }
}

internal val Player.sackTotal: Int
    get() = PayDirtOre.entries.sumOf { sackCount(it) }

internal fun Player.heldPayDirt(): MutableList<Int> = attr.getOrPut(HELD_PAYDIRT) { mutableListOf() }

internal fun Player.cleaningPayDirt(): MutableList<Int> =
    attr.getOrPut(CLEANING_PAYDIRT) { mutableListOf() }

internal val Player.cleaningCount: Int
    get() = attr[CLEANING_PAYDIRT]?.size ?: 0

internal fun Player.syncMotherlodeVars() {
    sackTransmit = sackTotal.coerceAtMost(MAX_SACK_TRANSMIT)
    largerSackTransmit = hasLargerSack
    restrictedAreaTransmit = onUpperLevel
}

internal fun Player.markUnlocked(key: AttributeKey<Boolean>) {
    attr[key] = true
    syncMotherlodeVars()
}

private const val MAX_SACK_TRANSMIT = 255
