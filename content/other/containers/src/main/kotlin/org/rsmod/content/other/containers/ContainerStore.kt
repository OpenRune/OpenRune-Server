package org.rsmod.content.other.containers

import dev.openrune.ServerCacheManager
import jakarta.inject.Inject
import jakarta.inject.Singleton
import org.rsmod.api.invtx.invAdd
import org.rsmod.api.invtx.invDel
import org.rsmod.api.invtx.invTransfer
import org.rsmod.game.entity.Player
import org.rsmod.game.inv.InvObj
import org.rsmod.game.inv.Inventory

@Singleton
internal class ContainerStore @Inject constructor() {
    fun owns(player: Player, spec: ContainerSpec): Boolean = player.inv.any { it?.id == spec.id } || player.worn.any { it?.id == spec.id }
    fun inventoryName(spec: ContainerSpec): String? = when (spec.family) { "tackle_box" -> "inv.tackle_box"; "huntsman_kit" -> "inv.huntsmans_kit"; else -> null }
    fun storedObjects(player: Player, spec: ContainerSpec): List<InvObj> = inventoryName(spec)?.let { player.invMap[it]?.filterNotNull()?.map { it.copy() } }.orEmpty()
    fun contents(player: Player, spec: ContainerSpec): Map<String, Int> {
        val name = inventoryName(spec) ?: return player.attr[spec.stateKey].orEmpty()
        return player.invMap[name]?.filterNotNull()?.groupBy { it.id.toString() }?.mapValues { (_, objects) -> objects.sumOf { it.count.toLong() }.coerceAtMost(Int.MAX_VALUE.toLong()).toInt() }.orEmpty()
    }
    fun clear(player: Player, spec: ContainerSpec) { inventoryName(spec)?.let { player.invMap[it]?.fillNulls() }; player.attr.remove(spec.stateKey) }
    fun count(player: Player, spec: ContainerSpec, item: Int): Int = contents(player, spec)[item.toString()] ?: 0
    fun room(player: Player, spec: ContainerSpec, item: Int): Int = ContainerCapacity.remaining(spec, contents(player, spec), item)
    fun update(player: Player, spec: ContainerSpec, item: Int, delta: Int) {
        if (delta == 0) return
        val copy = contents(player, spec).toMutableMap()
        val next = (copy[item.toString()] ?: 0).toLong() + delta
        check(next in 0..Int.MAX_VALUE.toLong())
        if (next == 0L) copy.remove(item.toString()) else copy[item.toString()] = next.toInt()
        val name = inventoryName(spec)
        if (name == null) { if (copy.isEmpty()) player.attr.remove(spec.stateKey) else player.attr[spec.stateKey] = copy; return }
        val stored = player.invMap.getOrPut(name)
        val slots = stored.indices.filter { stored[it]?.id == item }
        if (delta >= 0) {
            val slot = slots.firstOrNull() ?: stored.indices.firstOrNull { stored[it] == null } ?: error("Full $name")
            stored[slot] = (stored[slot] ?: InvObj(ServerCacheManager.getItem(item)!!)).copy(count = next.toInt())
        } else {
            var remaining = -delta
            for (slot in slots) { val obj = stored[slot]!!; val take = minOf(remaining, obj.count); stored[slot] = if (obj.count == take) null else obj.copy(count = obj.count - take); remaining -= take; if (remaining == 0) break }
        }
    }
    fun store(player: Player, spec: ContainerSpec, item: Int, requested: Int): Int {
        if (!owns(player, spec) || requested <= 0) return 0
        val type = ServerCacheManager.getItem(item) ?: return 0
        if (type.certtemplate != 0) return 0
        val native = inventoryName(spec)
        if (native != null) return storeNative(player, spec, item, requested, player.invMap.getOrPut(native))
        val have = player.inv.objs.sumOf { if (it?.id == item) it.count.toLong() else 0L }
        val move = minOf(requested.toLong(), have, room(player, spec, item).toLong()).toInt()
        if (move == 0) return 0
        val take = player.invDel(player.inv, item, move, autoCommit = false)
        if (take.failure) return 0
        take.commitAll(); update(player, spec, item, move)
        return move
    }
    fun withdraw(player: Player, spec: ContainerSpec, item: Int, requested: Int, target: Inventory): Int {
        if (!owns(player, spec) || requested <= 0) return 0
        val native = inventoryName(spec)
        if (native != null) {
            val stored = player.invMap[native] ?: return 0
            var remaining = requested; var moved = 0
            for (slot in stored.indices) {
                val obj = stored[slot]?.takeIf { it.id == item } ?: continue
                val before = obj.count
                val tx = player.invTransfer(stored, slot, minOf(remaining, before), target, strict = false)
                if (tx.failure) continue
                val actual = before - (stored[slot]?.count ?: 0); moved += actual; remaining -= actual
                if (remaining <= 0) break
            }
            return moved
        }
        val move = minOf(requested, count(player, spec, item))
        if (move == 0) return 0
        val add = player.invAdd(target, item, move, strict = false, autoCommit = false)
        if (add.failure) return 0
        val actual = add.completed()
        add.commitAll(); update(player, spec, item, -actual)
        return actual
    }
    private fun storeNative(player: Player, spec: ContainerSpec, item: Int, requested: Int, stored: Inventory): Int {
        var remaining = minOf(requested, room(player, spec, item)); var moved = 0
        if (remaining == 0) return 0
        for (slot in player.inv.indices) {
            val obj = player.inv[slot]?.takeIf { it.id == item } ?: continue
            // The native shared stack cannot represent two different charge states for one type.
            if (stored.any { it?.id == obj.id && it.vars != obj.vars }) continue
            val before = obj.count
            val tx = player.invTransfer(player.inv, slot, minOf(remaining, before), stored, strict = false)
            if (tx.failure) continue
            val actual = before - (player.inv[slot]?.count ?: 0); moved += actual; remaining -= actual
            if (remaining <= 0) break
        }
        return moved
    }
}
