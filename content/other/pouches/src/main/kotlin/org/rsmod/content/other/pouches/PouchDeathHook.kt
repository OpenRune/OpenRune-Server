package org.rsmod.content.other.pouches

import dev.openrune.types.util.UncheckedType
import jakarta.inject.Inject
import org.rsmod.api.death.PlayerDeathContext
import org.rsmod.api.death.PlayerDeathDropHook
import org.rsmod.api.death.PlayerDeathDrops.DeathDropResult
import org.rsmod.api.player.SupplyItems
import org.rsmod.content.other.pouches.lootingbag.*
import org.rsmod.content.other.pouches.runepouch.*
import org.rsmod.game.inv.InvObj
import org.rsmod.game.type.getInvObj

/** Preview selection is pure; stored contents are cleared only after an actual death. */
@OptIn(UncheckedType::class)
class PouchDeathHook @Inject constructor() : PlayerDeathDropHook {
    override fun neverProtected(item: InvObj): Boolean = item.id in RunePouches.ids && item.id !in RunePouches.lockedIds
    override fun resolve(context: PlayerDeathContext, result: DeathDropResult): DeathDropResult {
        val player = context.player
        val bag = if (player.hasLootingBag()) player.lootingBag.objs.filterNotNull() else emptyList()
        val bagDrops = bag.filter { !context.isPvpDeath || !SupplyItems.isFoodOrPotion(getInvObj(it)) }
        val runes = if (emptyRunes(context)) player.runePouchSlots()?.let { slots ->
            val contents = player.readRunePouch(slots)
            (0 until slots).mapNotNull { slot ->
                val rune = RunePouchRunes.byCompact(contents.types[slot])
                if (rune != null && contents.counts[slot] > 0) InvObj(rune.id, contents.counts[slot]) else null
            }
        }.orEmpty() else emptyList()
        val destroyBag = context.isPvpDeath || context.inWilderness
        val returned = if (context.isPvpDeath) (result.supplyPile + result.lostTradeable + result.lostUntradeable).filter { it.id in RunePouches.lockedIds } else emptyList()
        fun List<InvObj>.retained() = filterNot { destroyBag && it.id in LootingBags.ids || context.isPvpDeath && it.id in RunePouches.ids }
        return result.copy(
            kept = result.kept.filterNot { destroyBag && it.id in LootingBags.ids || context.isPvpDeath && it.id in RunePouches.ids && it.id !in RunePouches.lockedIds } + returned,
            supplyPile = result.supplyPile.retained(),
            lostTradeable = result.lostTradeable.retained() + bagDrops + runes,
            lostUntradeable = result.lostUntradeable.retained(),
        )
    }

    private fun emptyRunes(context: PlayerDeathContext): Boolean =
        context.isPvpDeath || context.player.vars["varbit.emptyondeath_runepouch"] > 0

    override fun prepareCompletion(context: PlayerDeathContext): () -> Unit {
        val player = context.player
        val hadBag = player.hasLootingBag()
        val locks = RunePouchLocks.remembered(player)
        var completed = false
        val runeSlots = if (emptyRunes(context)) player.runePouchSlots() else null
        return completion@{
            if (completed) return@completion
            completed = true
            if (locks.values.any { it > 0 }) player.attr[RunePouchLocks.KEY] = locks
            if (hadBag) { player.lootingBag.fillNulls(); player.saveLootingBag() }
            if (runeSlots != null) {
                val contents = player.readRunePouch(RunePouches.DIVINE_SLOTS)
                contents.clear()
                player.writeRunePouch(contents)
            }
        }
    }
}
