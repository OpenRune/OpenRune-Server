package org.rsmod.content.skills.farming

import dev.openrune.rscm.RSCM.asRSCM
import org.rsmod.api.invtx.add
import org.rsmod.api.invtx.delete
import org.rsmod.api.invtx.invTransaction
import org.rsmod.api.invtx.select
import org.rsmod.api.player.stat.basePrayerLvl
import org.rsmod.api.player.stat.cookingLvl
import org.rsmod.api.player.stat.farmingLvl
import org.rsmod.game.entity.Player
import org.rsmod.game.inv.InvObj
import org.rsmod.objtx.TransactionResultList

internal data class GrapeHarvest(val produce: String, val saveThreshold: Int, val blessed: Boolean)

object FarmingGrapes {
    const val BLESSING = "obj.grape_blessing"
    const val GRAPES = "obj.grapes"
    const val ZAMORAK_GRAPES = "obj.zamorak_grapes"
    const val SALTPETRE = "obj.hosidius_saltpetre"
    const val FERTILISER = "obj.hosidius_fertiliser"

    fun canBless(player: Player): Boolean =
        player.basePrayerLvl >= FarmingTitheData.row.blessingPrayer &&
            player.cookingLvl >= FarmingTitheData.row.blessingCooking &&
            player.inv.physicalCount(BLESSING) > 0

    internal fun prepare(player: Player, state: PatchState, secateurs: Boolean, attas: Boolean): GrapeHarvest {
        val blessed = canBless(player)
        val flags = (if (blessed) 1 else 0) or (if (state.fertilised) 2 else 0) or
            (if (secateurs) 4 else 0) or (if (attas) 8 else 0)
        val saves = FarmingTitheData.row.grapeSaves
        val offset = (saves.indices step 3).first { saves[it] == flags }
        val level = player.farmingLvl.coerceIn(1, 99)
        val threshold = grapeSaveThreshold(level, saves[offset + 1], saves[offset + 2])
        return GrapeHarvest(if (blessed) ZAMORAK_GRAPES else GRAPES, threshold, blessed)
    }

    internal fun prepareHarvest(player: Player, harvest: GrapeHarvest): TransactionResultList<InvObj> =
        player.invTransaction(player.inv, autoCommit = false) {
            val inv = select(player.inv)
            if (harvest.blessed) delete(inv, BLESSING.asRSCM(), 1)
            add(inv, harvest.produce.asRSCM(), 1)
        }

    fun prepareSoil(player: Player, farming: FarmingService, patch: PatchDef, obj: String, slot: Int): Boolean {
        if (patch.kind != PatchKind.GRAPE_VINE || obj != SALTPETRE && obj != FERTILISER) return false
        val state = farming.state(player, patch)
        if (state.health == Health.DEAD) return false
        val fertiliser = obj == FERTILISER
        if (fertiliser && state.fertilised) return false
        if (!fertiliser &&
            (state.cropIndex != 0 || state.grapePrepared || !player.inv.contains("obj.gardening_trowel"))
        ) return false
        val payment = player.invTransaction(player.inv, autoCommit = false) {
            val inv = select(player.inv)
            delete(inv, obj.asRSCM(), 1, slot)
            if (fertiliser) add(inv, "obj.bucket_empty".asRSCM(), 1, slot = slot)
        }
        if (!payment.success) return false
        val next = if (fertiliser) state.copy(fertilised = true) else state.copy(grapePrepared = true)
        return farming.replace(player, patch, state, next) {
            payment.commitAll()
            true
        }
    }
}

internal fun grapeSaveThreshold(level: Int, low: Int, high: Int): Int =
    1 + Math.floorDiv(low * (99 - level.coerceIn(1, 99)) + high * (level.coerceIn(1, 99) - 1) + 49, 98)
