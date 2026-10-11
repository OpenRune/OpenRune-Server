package org.rsmod.content.other.containers

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.util.Wearpos
import org.rsmod.api.invtx.*
import org.rsmod.api.player.stat.hitpoints
import org.rsmod.api.player.vars.VarPlayerIntMapSetter
import org.rsmod.api.player.worn.DizanasQuiver
import org.rsmod.game.entity.Player
import org.rsmod.game.inv.InvObj
import org.rsmod.game.inv.Inventory

internal object AmmoContainers {
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
internal object BoltPouch {
    const val ITEM = "obj.xbows_bolt_pouch"
    const val MAIN = "interface.xbows_pouch"
    val components = listOf("current_bolt", "bolt_1", "bolt_2", "bolt_3", "bolt_4").map { "component.xbows_pouch:$it" }
    private val types by lazy { ServerCacheManager.getEnum(336)!!.values.mapValues { (it.value as Number).toInt() } }
    fun owned(player: Player) = player.inv.any { it?.id == ITEM.asRSCM() }
    fun contents(player: Player, slot: Int): InvObj? {
        if (slot !in 1..4) return null
        val id = types[player.vars["varbit.xbows_pouch_slot$slot"]] ?: return null
        val count = player.vars["varbit.xbows_pouch_num$slot"].coerceAtLeast(0)
        return if (count == 0) null else InvObj(ServerCacheManager.getItem(id)!!, count)
    }
    fun limit(id: Int) = if (id == "obj.xbows_grapple_tip_bolt_mithril_rope".asRSCM()) 1 else 10_000
    fun accepted(id: Int) = id in types.values
    fun write(player: Player, slot: Int, item: InvObj?) {
        require(slot in 1..4 && (item == null || item.vars == 0 && accepted(item.id) && item.count in 1..limit(item.id)))
        val key = item?.let { obj -> types.entries.single { it.value == obj.id }.key } ?: 0
        VarPlayerIntMapSetter.set(player, "varbit.xbows_pouch_slot$slot", key)
        VarPlayerIntMapSetter.set(player, "varbit.xbows_pouch_num$slot", item?.count ?: 0)
    }
    fun fill(player: Player, invSlot: Int): Int {
        if (!owned(player) || !AmmoContainers.active(player)) return 0
        val input = player.inv[invSlot] ?: return 0
        if (input.vars != 0 || !accepted(input.id)) return 0
        val slot = (1..4).firstOrNull { contents(player, it)?.id == input.id } ?: (1..4).firstOrNull { contents(player, it) == null } ?: return 0
        val old = contents(player, slot); val moved = minOf(input.count, limit(input.id) - (old?.count ?: 0))
        if (moved <= 0 || player.invDel(player.inv, input.id, moved, slot = invSlot).failure) return 0
        write(player, slot, input.copy(count = (old?.count ?: 0) + moved)); return moved
    }
    fun remove(player: Player, slot: Int): Int {
        if (!owned(player) || !AmmoContainers.active(player)) return 0
        val old = contents(player, slot) ?: return 0
        val result = player.invAdd(player.inv, old.id, old.count, strict = false)
        val moved = if (result.success) result.completed() else 0
        if (moved > 0) write(player, slot, if (moved == old.count) null else old.copy(count = old.count - moved))
        return moved
    }
    fun clear(player: Player) { for (slot in 1..4) write(player, slot, null) }
    fun toWorn(player: Player, slot: Int): Boolean {
        if (!owned(player) || DizanasQuiver.owns(player) || !AmmoContainers.active(player)) return false
        val old = player.worn[Wearpos.Quiver.slot]
        if (old != null && accepted(old.id) && old.count > limit(old.id)) return false
        return AmmoContainers.swap(player, contents(player, slot), { it.vars == 0 && accepted(it.id) }) { write(player, slot, it) }
    }
}
