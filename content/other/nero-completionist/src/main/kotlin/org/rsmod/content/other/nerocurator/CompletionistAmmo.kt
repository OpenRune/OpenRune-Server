package org.rsmod.content.other.nerocurator

import dev.openrune.ServerCacheManager
import dev.openrune.util.Wearpos
import org.rsmod.api.invtx.*
import org.rsmod.api.player.stat.hitpoints
import org.rsmod.api.player.worn.DizanasQuiver
import org.rsmod.game.entity.Player
import org.rsmod.game.inv.InvObj
import org.rsmod.game.inv.Inventory

internal object CompletionistAmmo {
    fun active(player: Player) = player.hitpoints > 0 && !player.pendingLogout && !player.loggingOut
    fun fillQuiver(player: Player, inventory: Inventory, slot: Int): Int {
        if (!active(player) || !DizanasQuiver.owns(player)) return 0
        val input = inventory[slot] ?: return 0
        if (input.vars != 0 || !DizanasQuiver.storable(ServerCacheManager.getItem(input.id)!!)) return 0
        val stored = DizanasQuiver.stored(player)
        if (stored != null && stored.id != input.id) return 0
        val moved = minOf(input.count, Int.MAX_VALUE - (stored?.count ?: 0))
        if (moved <= 0 || player.invDel(inventory, input.id, moved, slot = slot).failure) return 0
        DizanasQuiver.store(player, input.copy(count = moved + (stored?.count ?: 0))); return moved
    }
    fun emptyQuiver(player: Player): Int {
        if (!active(player) || !DizanasQuiver.owns(player)) return 0
        val item = DizanasQuiver.stored(player) ?: return 0
        val result = player.invAdd(player.inv, item.id, item.count, strict = false)
        val moved = if (result.success) result.completed() else 0
        if (moved > 0) DizanasQuiver.store(player, if (moved == item.count) null else item.copy(count = item.count - moved))
        return moved
    }

    /** All physical output commits before replacing the native shared extra-ammo counter. */
    fun swap(player: Player, extra: InvObj?, accepts: (InvObj) -> Boolean = { it.vars == 0 && DizanasQuiver.storable(ServerCacheManager.getItem(it.id)!!) }, replace: (InvObj?) -> Unit): Boolean {
        if (!active(player) || extra == null) return false
        val old = player.worn[Wearpos.Quiver.slot]
        val storable = old?.let(accepts) == true
        val result = player.invTransaction(player.worn, player.inv) {
            val equipment = select(player.worn); val inventory = select(player.inv)
            if (old != null) { delete(equipment, old.id, old.count, slot = Wearpos.Quiver.slot); if (!storable) add(inventory, old.id, old.count, old.vars) }
            add(equipment, extra.id, extra.count, extra.vars, slot = Wearpos.Quiver.slot)
        }
        if (result.failure) return false
        replace(if (storable) old else null); player.rebuildAppearance(); return true
    }
}

/** Four native slots, including slot four used by the worn extra-ammo widget. */
