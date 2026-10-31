package org.rsmod.content.skills.farming

import org.rsmod.api.death.PlayerDeathContext
import org.rsmod.api.death.PlayerDeathDropHook
import org.rsmod.api.death.PlayerDeathDrops.DeathDropResult
import org.rsmod.api.death.PlayerDeathHandling
import org.rsmod.api.death.UntradeableHandling
import org.rsmod.api.invtx.delete
import org.rsmod.api.invtx.invTransaction
import org.rsmod.api.invtx.select
import org.rsmod.api.player.vars.boolVarBit
import org.rsmod.game.entity.Player
import org.rsmod.game.inv.InvObj
import org.rsmod.game.type.getInvObj

internal class FarmingBagDeathHook : PlayerDeathDropHook {
    override val priority: Int = 100

    private val Player.emptyHerbSack by boolVarBit("varbit.emptyondeath_herbsack")
    private val Player.emptySeedBox by boolVarBit("varbit.emptyondeath_seedbox")

    override fun prepare(
        context: PlayerDeathContext,
        handling: PlayerDeathHandling,
        result: DeathDropResult,
    ): DeathDropResult {
        val player = context.player
        val stores = buildList(2) {
            for (bag in BAGS) {
                if (bag.variants.none { it in player.inv || it in player.worn }) continue
                val emptySetting =
                    if (bag === FarmingBags.herbSack) player.emptyHerbSack else player.emptySeedBox
                val destroyed =
                    handling.untradeableHandling in DESTROYED &&
                        result.lostUntradeable.any { getInvObj(it).internalName in bag.variants }
                if (context.isPvpDeath || emptySetting || destroyed) {
                    val store = bag.store(player)
                    if (store.isNotEmpty()) add(store)
                }
            }
        }
        if (stores.isEmpty()) return result

        val spilled = mutableListOf<InvObj>()
        val transaction = player.invTransaction(stores.first(), stores.getOrNull(1)) {
            for (store in stores) {
                val from = select(store)
                for (slot in store.indices) {
                    val obj = store[slot] ?: continue
                    delete(from, obj.id, obj.count, slot = slot)
                    spilled += obj
                }
            }
        }
        check(transaction.success) { "Could not empty Farming bags on death: $player" }
        return result.copy(lostTradeable = result.lostTradeable + spilled)
    }

    private companion object {
        val BAGS = listOf(FarmingBags.herbSack, FarmingBags.seedBox)
        val DESTROYED = setOf(UntradeableHandling.COINS, UntradeableHandling.DESTROY)
    }
}
