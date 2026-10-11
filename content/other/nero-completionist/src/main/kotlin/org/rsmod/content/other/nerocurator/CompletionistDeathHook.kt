package org.rsmod.content.other.nerocurator

import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.util.Wearpos
import jakarta.inject.Inject
import org.rsmod.api.death.PlayerDeathContext
import org.rsmod.api.death.PlayerDeathDropHook
import org.rsmod.api.death.PlayerDeathDrops.DeathDropResult
import org.rsmod.api.player.vars.VarPlayerIntMapSetter
import org.rsmod.api.player.worn.DizanasQuiver
import org.rsmod.game.inv.InvObj

internal class CompletionistDeathHook @Inject constructor() : PlayerDeathDropHook {
    override fun neverProtected(item: InvObj) = CompletionistProtection.family(item)?.broken(item) == true
    override fun resolve(context: PlayerDeathContext, result: DeathDropResult): DeathDropResult {
        val returned = mutableListOf<InvObj>()
        val extra = mutableListOf<InvObj>()
        val removed = mutableSetOf<InvObj>()
        for (item in result.lostTradeable + result.lostUntradeable + result.supplyPile) {
            val family = CompletionistProtection.family(item) ?: continue
            val locked = family.locked(item)
            if (!context.isPvpDeath) { if (locked) { returned += item; removed += item }; continue }
            removed += item
            val deep = context.wildernessLevel > 20
            if (family.broken(item)) {
                if (locked || !deep) returned += item else extra += InvObj("obj.coins", CompletionistProtection.DEEP_COINS * item.count)
                continue
            }
            if (!deep || locked) returned += item.copy(id = (if (locked) (if (deep) family.mangled else family.lockedBroken) else family.broken).asRSCM())
            val fee = if (deep) (if (locked) 500000 else CompletionistProtection.DEEP_COINS) else family.repairFee
            extra += InvObj("obj.coins", fee * item.count)
        }
        if (context.isPvpDeath && carriedDizana(context)) DizanasQuiver.stored(context.player)?.let(extra::add)
        return result.copy(kept = result.kept + returned, supplyPile = result.supplyPile.filterNot { it in removed }, lostTradeable = result.lostTradeable.filterNot { it in removed } + extra, lostUntradeable = result.lostUntradeable.filterNot { it in removed })
    }
    override fun prepareCompletion(context: PlayerDeathContext): () -> Unit {
        val dizana = carriedDizana(context)
        val worn = if (context.isPvpDeath) emptyList() else context.player.worn.indices.mapNotNull { slot ->
            val item = context.player.worn[slot] ?: return@mapNotNull null
            if (CompletionistProtection.family(item)?.locked(item) == true) slot to item else null
        }
        var completed = false
        return {
            if (!completed) {
                completed = true
                for ((slot, item) in worn) {
                    val restored = context.player.inv.indices.firstOrNull { context.player.inv[it] == item }
                    if (restored != null && context.player.worn[slot] == null) { context.player.inv[restored] = null; context.player.worn[slot] = item; context.player.rebuildAppearance() }
                }
                if (context.isPvpDeath && dizana) DizanasQuiver.store(context.player, null)
                if (dizana) VarPlayerIntMapSetter.set(context.player, "varbit.worn_items_dizanas_quiver_charged", if (DizanasQuiver.blessed(context.player.worn[Wearpos.Back.slot])) 1 else 0)
            }
        }
    }
    private fun carriedDizana(context: PlayerDeathContext) = (context.player.inv.asSequence() + context.player.worn.asSequence()).filterNotNull().any { CompletionistProtection.family(it)?.key == "dizana" }
}
