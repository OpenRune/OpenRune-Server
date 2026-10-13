package org.rsmod.content.other.gravestones

import org.rsmod.api.player.vars.VarPlayerIntMapSetter
import org.rsmod.game.entity.Player
import org.rsmod.game.inv.InvObj

/**
 * Where each obj sat in the inventory and equipment when the player died, so looting the grave can
 * put things back ("Looting a gravestone should now automatically sort the inventory to how it was
 * prior to dying", 22 July 2026). One varbit per slot holds the obj id plus one, 0 for empty.
 */
internal object GraveLayout {
    private const val INV_SLOTS = 28
    private const val WORN_SLOTS = 14
    private const val MAX_STORED = 0xFFFF

    fun record(player: Player) {
        for (slot in 0 until INV_SLOTS) {
            set(player, slot, player.inv[slot])
        }
        for (slot in 0 until WORN_SLOTS) {
            set(player, INV_SLOTS + slot, player.worn[slot])
        }
    }

    fun clear(player: Player) {
        for (index in 0 until INV_SLOTS + WORN_SLOTS) {
            VarPlayerIntMapSetter.set(player, varbit(index), 0)
        }
    }

    fun invSlot(player: Player, obj: Int): Int? =
        (0 until INV_SLOTS).firstOrNull { player.inv[it] == null && stored(player, it) == obj + 1 }

    fun wornSlot(player: Player, obj: Int): Int? =
        (0 until WORN_SLOTS).firstOrNull {
            player.worn[it] == null && stored(player, INV_SLOTS + it) == obj + 1
        }

    /** The obj that was worn in [slot] before the death, if the slot is still to be restored. */
    fun wornObj(player: Player, slot: Int): Int? =
        stored(player, INV_SLOTS + slot).takeIf { it != 0 }?.minus(1)

    fun consumeInv(player: Player, slot: Int) = VarPlayerIntMapSetter.set(player, varbit(slot), 0)

    fun consumeWorn(player: Player, slot: Int) =
        VarPlayerIntMapSetter.set(player, varbit(INV_SLOTS + slot), 0)

    private fun set(player: Player, index: Int, obj: InvObj?) {
        val stored = obj?.let { it.id + 1 }?.takeIf { it <= MAX_STORED } ?: 0
        VarPlayerIntMapSetter.set(player, varbit(index), stored)
    }

    private fun stored(player: Player, index: Int): Int = player.vars[varbit(index)]

    private fun varbit(index: Int): String = "varbit.gravestone_layout_obj_$index"

    const val WORN_SLOT_COUNT: Int = WORN_SLOTS
}
