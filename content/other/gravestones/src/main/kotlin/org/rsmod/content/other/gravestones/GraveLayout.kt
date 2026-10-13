package org.rsmod.content.other.gravestones

import org.rsmod.game.entity.Player
import org.rsmod.game.inv.Inventory

/**
 * Where each obj sat in the inventory and equipment when the player died, so looting the grave can
 * put things back ("Looting a gravestone should now automatically sort the inventory to how it was
 * prior to dying", 22 July 2026). A copy of both, inventory slots first, kept in its own inv.
 */
internal object GraveLayout {
    private const val INV_SLOTS = 28
    private const val WORN_SLOTS = 14

    fun record(player: Player) {
        val layout = player.layout
        for (slot in 0 until INV_SLOTS) {
            layout[slot] = player.inv[slot]
        }
        for (slot in 0 until WORN_SLOTS) {
            layout[INV_SLOTS + slot] = player.worn[slot]
        }
    }

    fun clear(player: Player) {
        player.layout.fillNulls()
    }

    fun invSlot(player: Player, obj: Int): Int? =
        (0 until INV_SLOTS).firstOrNull { player.inv[it] == null && player.layout[it]?.id == obj }

    fun wornSlot(player: Player, obj: Int): Int? =
        (0 until WORN_SLOTS).firstOrNull {
            player.worn[it] == null && player.layout[INV_SLOTS + it]?.id == obj
        }

    /** The obj that was worn in [slot] before the death, if the slot is still to be restored. */
    fun wornObj(player: Player, slot: Int): Int? = player.layout[INV_SLOTS + slot]?.id

    fun consumeInv(player: Player, slot: Int) {
        player.layout[slot] = null
    }

    fun consumeWorn(player: Player, slot: Int) {
        player.layout[INV_SLOTS + slot] = null
    }

    private val Player.layout: Inventory
        get() = invMap.getOrPut("inv.gravestone_layout")

    const val WORN_SLOT_COUNT: Int = WORN_SLOTS
}
