package org.rsmod.content.skills.farming

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.types.ItemServerType
import org.rsmod.api.invtx.add
import org.rsmod.api.invtx.delete
import org.rsmod.api.invtx.invTransaction
import org.rsmod.api.invtx.select
import org.rsmod.api.player.stat.farmingLvl
import org.rsmod.api.player.vars.VarPlayerIntMapSetter
import org.rsmod.api.table.farming.FarmingSaplingsRow
import org.rsmod.content.generic.locs.water.WateringCans
import org.rsmod.content.interfaces.bank.util.BankTransforms
import org.rsmod.game.entity.Player
import org.rsmod.game.inv.InvObj
import org.rsmod.game.inv.Inventory

internal data class SaplingChain(
    val seed: ItemServerType,
    val dry: ItemServerType,
    val wet: ItemServerType,
    val sapling: ItemServerType,
    val level: Int,
)

object FarmingSaplings {
    private const val CLOCK = "varp.farming_sapling_clock"
    internal val chains by lazy {
        FarmingSaplingsRow.getRow("dbrow.farming_saplings").chains.map {
            SaplingChain(it.t0, it.t1, it.t2, it.t3, it.t4)
        }
    }
    private val dry by lazy { chains.associateBy { it.dry.id } }
    private val wet by lazy { chains.associateBy { it.wet.id } }

    internal fun dry(obj: Int): SaplingChain? = dry[obj]

    internal fun sow(player: Player, chain: SaplingChain, seedSlot: Int, potSlot: Int): Boolean {
        if (player.farmingLvl < chain.level || !player.inv.contains("obj.gardening_trowel")) return false
        return player.invTransaction(player.inv) {
            val inv = select(player.inv)
            delete(inv, chain.seed.id, 1, seedSlot)
            delete(inv, "obj.plantpot_compost".asRSCM(), 1, potSlot)
            add(inv, chain.dry.id, 1, slot = potSlot)
        }.success
    }
    internal fun water(
        player: Player,
        seedlingSlot: Int,
        can: String,
        canSlot: Int,
        millis: Long = System.currentTimeMillis(),
    ): Boolean {
        // Advance old wet pots before introducing one at a crossed boundary.
        grow(player, millis)
        val obj = player.inv[seedlingSlot] ?: return false
        val chain = dry(obj.id) ?: return false
        return WateringCans.water(player, seedlingSlot, can, canSlot, chain.dry.id, chain.wet.id)
    }

    fun hasHumidifyTarget(player: Player): Boolean =
        player.inv.any { obj -> obj != null && dry(obj.id) != null } ||
            WateringCans.CANS.any { WateringCans.needsRefill(player, it) }

    fun humidify(player: Player) {
        grow(player)
        val result = player.invTransaction(player.inv) {
            val inv = select(player.inv)
            for (slot in player.inv.indices) {
                val obj = player.inv[slot] ?: continue
                val chain = dry(obj.id)
                val doses = WateringCans.doses(checkNotNull(ServerCacheManager.getItem(obj.id)).internalName)
                when {
                    chain != null -> {
                        delete(inv, obj.id, 1, slot)
                        add(inv, chain.wet.id, 1, slot = slot)
                    }
                    doses != null && doses < 8 -> {
                        delete(inv, obj.id, 1, slot)
                        add(inv, WateringCans.CANS[8].asRSCM(), 1, slot = slot)
                    }
                }
            }
        }
        if (result.err != null) return
        WateringCans.refill(player, "obj.zeah_wateringcan")
    }


    fun grow(player: Player, millis: Long = System.currentTimeMillis()) {
        val cycle = FarmingClock.cycle(player, 5, millis).toInt()
        val previous = player.vars[CLOCK]
        if (cycle <= previous) return
        if (previous != 0) {
            mature(player.inv)
            player.invMap["inv.bank"]?.let { matureBank(player, it) }
        }
        VarPlayerIntMapSetter.set(player, CLOCK, cycle)
    }

    private fun mature(inventory: Inventory) {
        for (slot in inventory.indices) {
            val obj = inventory[slot] ?: continue
            val chain = wet[obj.id] ?: continue
            inventory[slot] = InvObj(chain.sapling, obj.count)
        }
    }

    private fun matureBank(player: Player, bank: Inventory) {
        for (slot in bank.indices.reversed()) {
            val obj = bank[slot] ?: continue
            val chain = wet[obj.id] ?: continue
            BankTransforms.transform(player, bank, slot, chain.sapling)
        }
    }

}
