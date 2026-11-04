package org.rsmod.content.skills.farming

import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import org.rsmod.api.invtx.add
import org.rsmod.api.invtx.invTransaction
import org.rsmod.api.invtx.select
import org.rsmod.game.entity.Player
import org.rsmod.game.inv.InvObj
import org.rsmod.objtx.TransactionResultList

/**
 * Harvested produce goes into an open herb sack or reagent pouch while it fits and the rest into
 * the inventory, as one transaction: the whole batch is placed or nothing changes.
 */
object FarmingHarvestStorage {
    private val HARVEST_BAGS = listOf(FarmingBags.herbSack, FarmingBags.reagentPouch)

    fun canAddHarvest(player: Player, produce: String, amount: Int): Boolean =
        prepareHarvest(player, produce, amount).success

    fun addHarvest(player: Player, produce: String, amount: Int): Boolean =
        add(player, produce, amount, autoCommit = true).success

    fun prepareHarvest(player: Player, produce: String, amount: Int): TransactionResultList<InvObj> =
        add(player, produce, amount, autoCommit = false)

    private fun add(player: Player, produce: String, amount: Int, autoCommit: Boolean): TransactionResultList<InvObj> {
        require(amount > 0) { "amount must be positive: $amount" }
        val obj = produce.asRSCM(RSCMType.OBJ)
        val bag = HARVEST_BAGS.firstOrNull { it.isStorable(obj) }
        val held = bag?.openHeld(player)
        val toBag = if (bag != null && held != null) minOf(amount, bag.room(player, held, obj)) else 0
        val toInv = amount - toBag
        val store = if (toBag > 0) bag?.store(player) else null
        return player.invTransaction(player.inv, store, autoCommit) {
            val inv = select(player.inv)
            if (store != null) add(select(store), obj, toBag)
            if (toInv > 0) add(inv, obj, toInv)
        }
    }
}
