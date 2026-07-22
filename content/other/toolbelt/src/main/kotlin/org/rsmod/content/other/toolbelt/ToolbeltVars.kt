package org.rsmod.content.other.toolbelt

import org.rsmod.api.player.vars.VarPlayerIntMapSetter
import org.rsmod.game.entity.Player

object ToolbeltVars {
    const val EMPTY_STORED = 0
    const val SLOT_COUNT = 28

    fun varBitName(slotIndex: Int): String {
        require(slotIndex in 0 until SLOT_COUNT) { "Invalid toolbelt slot: $slotIndex" }
        return "varbit.toolbelt_slot_$slotIndex"
    }

    fun varpName(slotIndex: Int): String {
        require(slotIndex in 0 until SLOT_COUNT) { "Invalid toolbelt slot: $slotIndex" }
        return "varp.toolbelt_slot_$slotIndex"
    }

    fun readStoredAllowedIndex(player: Player, slotIndex: Int): Int =
        player.vars[varBitName(slotIndex)]

    fun setStoredAllowedIndex(player: Player, slotIndex: Int, allowedIndex: Int) {
        val value = if (allowedIndex <= EMPTY_STORED) EMPTY_STORED else allowedIndex
        VarPlayerIntMapSetter.set(player, varBitName(slotIndex), value)
    }

    fun clearSlot(player: Player, slotIndex: Int) {
        setStoredAllowedIndex(player, slotIndex, EMPTY_STORED)
    }
}
