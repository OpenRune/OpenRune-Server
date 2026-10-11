package org.rsmod.content.other.containers

import jakarta.inject.Inject
import org.rsmod.api.death.PlayerDeathContext
import org.rsmod.api.death.PlayerDeathDropHook
import org.rsmod.api.death.PlayerDeathDrops.DeathDropResult
import org.rsmod.api.player.vars.VarPlayerIntMapSetter
import org.rsmod.api.player.worn.DizanasQuiver
import org.rsmod.game.inv.InvObj

internal class QuiverDeathHook @Inject constructor() : PlayerDeathDropHook {
    override fun neverProtected(item: InvObj) = QuiverForms.family(item)?.broken(item) == true
    override fun resolve(context: PlayerDeathContext, result: DeathDropResult): DeathDropResult {
        val lost = result.lostTradeable + result.lostUntradeable + result.supplyPile
        val returned = mutableListOf<InvObj>()
        val additions = mutableListOf<InvObj>()
        if (context.isPvpDeath) {
            for (item in lost) {
                val family = QuiverForms.family(item) ?: continue
                if (family.broken(item)) {
                    if (family.locked(item) || context.wildernessLevel <= 20) returned += item
                    else additions += InvObj("obj.coins", family.deepCoins * item.count)
                    continue
                }
                family.lost(item, context.wildernessLevel > 20)?.let(returned::add)
                val fee = if (context.wildernessLevel > 20) (if (family.locked(item)) 500000 else family.deepCoins) else family.repairFee
                additions += InvObj("obj.coins", fee * item.count)
                val splinters = family.charges(item).count * item.count
                if (splinters > 0) additions += InvObj(DizanasQuiverScript.SPLINTER, splinters)
            }
            if (QuiverForms.carried(context.player).any { QuiverForms.family(it) != null }) DizanasQuiver.stored(context.player)?.let(additions::add)
        }
        fun List<InvObj>.retained() = if (context.isPvpDeath) filterNot { QuiverForms.family(it) != null } else this
        return result.copy(kept = result.kept + returned, supplyPile = result.supplyPile.retained(), lostTradeable = result.lostTradeable.retained() + additions, lostUntradeable = result.lostUntradeable.retained())
    }
    override fun prepareCompletion(context: PlayerDeathContext): () -> Unit {
        val carried = QuiverForms.carried(context.player).any { QuiverForms.family(it) != null }
        val remembered = QuiverLocks.remembered(context.player)
        val equippedKept = if (context.isPvpDeath) emptyList() else context.player.worn.indices.mapNotNull { slot ->
            val item = context.player.worn[slot] ?: return@mapNotNull null
            val family = QuiverForms.family(item) ?: return@mapNotNull null
            if (family.locked(item) && !family.broken(item)) slot to item else null
        }
        var completed = false
        return {
            if (!completed) {
                completed = true
                if (remembered.values.any { it > 0 }) context.player.attr[QuiverLocks.KEY] = remembered
                for ((slot, item) in equippedKept) {
                    val inventorySlot = context.player.inv.indices.firstOrNull { context.player.inv[it] == item }
                    if (inventorySlot != null && context.player.worn[slot] == null) {
                        context.player.inv[inventorySlot] = null
                        context.player.worn[slot] = item
                        context.player.rebuildAppearance()
                    }
                }
                if (carried) {
                    val cape = context.player.worn[1]
                    val charged = DizanasQuiver.recognises(cape) && (DizanasQuiver.blessed(cape) || DizanasQuiver.charges(cape).count > 0)
                    VarPlayerIntMapSetter.set(context.player, "varbit.worn_items_dizanas_quiver_charged", if (charged) 1 else 0)
                }
                if (context.isPvpDeath && carried) DizanasQuiver.store(context.player, null)
            }
        }
    }
}
