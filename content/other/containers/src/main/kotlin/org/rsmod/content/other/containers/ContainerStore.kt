package org.rsmod.content.other.containers

import dev.openrune.ServerCacheManager
import jakarta.inject.Inject
import jakarta.inject.Singleton
import org.rsmod.api.invtx.invAdd
import org.rsmod.api.invtx.invDel
import org.rsmod.game.entity.Player
import org.rsmod.game.inv.Inventory

@Singleton
internal class ContainerStore @Inject constructor() {
    fun owns(player: Player, spec: ContainerSpec): Boolean = player.inv.any { it?.id == spec.id } || player.worn.any { it?.id == spec.id }
    fun contents(player: Player, spec: ContainerSpec): Map<String, Int> = player.attr[spec.stateKey].orEmpty()
    fun count(player: Player, spec: ContainerSpec, item: Int): Int = contents(player, spec)[item.toString()] ?: 0
    fun room(player: Player, spec: ContainerSpec, item: Int): Int = ContainerCapacity.remaining(spec, contents(player, spec), item)
    fun update(player: Player, spec: ContainerSpec, item: Int, delta: Int) {
        val copy = contents(player, spec).toMutableMap()
        val next = (copy[item.toString()] ?: 0).toLong() + delta
        check(next in 0..Int.MAX_VALUE.toLong())
        if (next == 0L) copy.remove(item.toString()) else copy[item.toString()] = next.toInt()
        if (copy.isEmpty()) player.attr.remove(spec.stateKey) else player.attr[spec.stateKey] = copy
    }
    fun store(player: Player, spec: ContainerSpec, item: Int, requested: Int): Int {
        if (!owns(player, spec) || requested <= 0) return 0
        val type = ServerCacheManager.getItem(item) ?: return 0
        if (type.certtemplate != 0) return 0
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
        val move = minOf(requested, count(player, spec, item))
        if (move == 0) return 0
        val add = player.invAdd(target, item, move, strict = false, autoCommit = false)
        if (add.failure) return 0
        val actual = add.completed()
        add.commitAll(); update(player, spec, item, -actual)
        return actual
    }
}
