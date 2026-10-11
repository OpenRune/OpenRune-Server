package org.rsmod.content.other.pouches.runepouch

import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.types.ItemServerType
import jakarta.inject.Inject
import org.rsmod.api.attr.AttributeKey
import org.rsmod.api.player.hook.PlayerObjTakeStorageHook
import org.rsmod.game.entity.Player
import org.rsmod.game.inv.InvObj
import org.rsmod.game.inv.Inventory
import org.rsmod.game.obj.Obj

/** Remember paid copies without granting an extra lock to duplicate pouches. */
internal object RunePouchLocks {
    val KEY = AttributeKey<Map<String, Int>>(persistenceKey = "rune_pouch_trouver_locks")
    private fun stored(player: Player) = player.inv.filterNotNull() + player.invMap["inv.bank"]?.filterNotNull().orEmpty()
    fun remembered(player: Player) = listOf(RunePouches.REGULAR, RunePouches.DIVINE).associateWith { symbol -> maxOf(player.attr[KEY]?.get(symbol) ?: 0, stored(player).filter { it.id == RunePouches.locked(symbol)?.asRSCM() }.sumOf { it.count }) }
    fun change(player: Player, before: Map<String, Int>, symbol: String, delta: Int) { player.attr[KEY] = before + (symbol to ((before[symbol] ?: 0) + delta).coerceAtLeast(0)) }
    fun rememberRemoved(player: Player, item: InvObj) {
        if (item.id !in RunePouches.lockedIds) return
        val symbol = RunePouches.unlocked(item.id) ?: return
        val owned = stored(player).filter { it.id == item.id }.sumOf { it.count }
        val before = remembered(player); player.attr[KEY] = before + (symbol to maxOf(before[symbol] ?: 0, owned + item.count))
    }
    fun restore(player: Player) {
        val before = remembered(player)
        if (before.values.all { it <= 0 }) return
        player.attr[KEY] = before
        for ((symbol, paid) in before) {
            var missing = paid - stored(player).filter { it.id == RunePouches.locked(symbol)?.asRSCM() }.sumOf { it.count }
            for (slot in player.inv.indices) {
                if (missing <= 0) break
                val item = player.inv[slot]?.takeIf { it.count == 1 && it.id == symbol.asRSCM() } ?: continue
                player.inv[slot] = item.copy(id = RunePouches.locked(symbol)!!.asRSCM()); missing--
            }
        }
    }
}

internal class RunePouchPickupLocks @Inject constructor() : PlayerObjTakeStorageHook {
    override fun inventory(player: Player, obj: Obj, objType: ItemServerType): Inventory? = if (obj.type in RunePouches.ids) player.inv else null
    override fun committed(player: Player, obj: Obj, objType: ItemServerType) { RunePouchLocks.restore(player) }
}
