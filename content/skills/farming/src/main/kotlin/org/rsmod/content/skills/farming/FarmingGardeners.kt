package org.rsmod.content.skills.farming

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import jakarta.inject.Inject
import jakarta.inject.Singleton
import org.rsmod.api.invtx.delete
import org.rsmod.api.invtx.invTransaction
import org.rsmod.api.invtx.select
import org.rsmod.game.entity.Player

@Singleton
class FarmingGardeners @Inject constructor(private val farming: FarmingService) {
    internal val patches by lazy {
        FarmingServicesData.row.gardeners.groupBy { it.t0.internalName }.mapValues { (_, records) ->
            records.map { record ->
                GardenerPatch(checkNotNull(FarmingPatches.byLoc(record.t1.internalName)), record.t2)
            }.distinctBy { it.patch.varp }
        }
    }
    private val prices by lazy {
        FarmingServicesData.row.removalPrices.associate { PatchKind.of(it.t0) to it.t1 }
    }

    fun removalPrice(patch: PatchDef): Int? = prices[patch.kind]

    fun payment(crop: Crop): List<Pair<String, Int>> = crop.payment.map { (obj, count) ->
        val form = FarmingProduceContainers.forms.firstOrNull { it.obj.internalName == obj }
        val exact = form?.let { FarmingProduceContainers.fullContainer(it.produce.internalName) } ?: obj
        check(form == null || exact == obj) { "Gardener payment is not a full container: $obj" }
        exact to count
    }

    fun protect(player: Player, patch: PatchDef, expected: PatchState): ProtectResult {
        val crop = expected.crop ?: return ProtectResult.EMPTY
        val payment = payment(crop)
        if (payment.isEmpty()) return ProtectResult.UNPAID
        return farming.protect(player, patch, expected = expected) { takePayment(player, payment) }
    }

    internal fun takePayment(player: Player, payment: List<Pair<String, Int>>): Boolean {
        if (payment.isEmpty()) return false
        return player.invTransaction(player.inv) {
            val inv = select(player.inv)
            for ((obj, amount) in payment) {
                require(amount > 0)
                val type = checkNotNull(ServerCacheManager.getItem(obj.asRSCM()))
                val plain = minOf(amount, player.inv.physicalCount(type.internalName))
                if (plain > 0) delete(inv, type.id, plain)
                val noted = amount - plain
                if (noted > 0) {
                    // An impossible unnoted deletion makes the whole multi-item payment roll back.
                    delete(inv, if (type.canCert) type.certlink else type.id, noted)
                }
            }
        }.success
    }

    fun removeTree(player: Player, patch: PatchDef, expected: PatchState): Boolean {
        val price = removalPrice(patch) ?: return false
        return farming.removeTree(player, patch, expected) {
            takePayment(player, listOf("obj.coins" to price))
        }
    }
}

internal class GardenerPatch(val patch: PatchDef, val payOp: Int)
