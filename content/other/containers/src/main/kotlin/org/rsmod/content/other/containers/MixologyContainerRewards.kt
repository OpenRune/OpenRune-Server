package org.rsmod.content.other.containers

import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.types.dbcol.DbHelper
import jakarta.inject.Inject
import org.rsmod.api.invtx.invAdd
import org.rsmod.api.player.ironman.isUltimateIronman
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.stat.hitpoints
import org.rsmod.api.player.vars.VarPlayerIntMapSetter
import org.rsmod.api.script.onOpNpc1
import org.rsmod.api.script.onOpNpc3
import org.rsmod.content.interfaces.omnishop.OmnishopPurchase
import org.rsmod.content.interfaces.omnishop.openOmnishop
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

internal class MixologyContainerRewards @Inject constructor() : OmnishopPurchase {
    override fun handles(shop: Int, stock: Int) = shop == SHOP.asRSCM() && stock in STOCK.map { it.asRSCM() }
    override fun buy(access: ProtectedAccess, stock: Int, requested: Int): Unit = with(access) {
        if (!handles(player.vars["varp.omnishop_lastshop"], stock) || requested <= 0) return@with
        val storage = stock == STORAGE.asRSCM()
        if (storage && (player.vars[PotionStorage.UNLOCKED] != 0 || player.isUltimateIronman)) { mes(if (player.isUltimateIronman) "Ultimate Ironmen cannot use potion storage." else "You already have potion storage."); return@with }
        val row = DbHelper.row(stock)
        val costs = row.getColumn("dbcol.omnishop_stock_data:omnishop_stock_cost").column.values!!.map { (it as Number).toInt() }.chunked(2).map { (currency, price) -> CURRENCIES.entries.single { it.key.asRSCM() == currency }.value to price }
        check(costs.size == 3 && costs.all { it.second > 0 })
        val affordable = costs.minOf { (key, price) -> player.vars[key].coerceAtLeast(0) / price }
        val amount = minOf(if (storage) 1 else requested, affordable, if (storage) 1 else inv.freeSpace())
        if (amount <= 0) { mes(if (affordable == 0) "You don't have enough resin." else "You don't have enough inventory space."); return@with }
        val id = (row.getColumn("dbcol.omnishop_stock_data:omnishop_stock_obj").column.values!!.single() as Number).toInt()
        if (!storage && player.invAdd(inv, id, amount).failure) return@with
        for ((key, price) in costs) VarPlayerIntMapSetter.set(player, key, player.vars[key] - price * amount)
        if (storage) { VarPlayerIntMapSetter.set(player, PotionStorage.UNLOCKED, 1); mes("Potion storage is now available in your bank.") }
    }
    companion object {
        const val SHOP = "dbrow.mixology_rewards"
        const val STORAGE = "dbrow.mixology_stock_potion_storage"
        val STOCK = listOf("dbrow.mixology_stock_reagents_pouch", "dbrow.mixology_stock_prepot", STORAGE)
        val CURRENCIES = mapOf("dbrow.omnishop_currency_mixology_mox" to "varp.mixology_mox_points", "dbrow.omnishop_currency_mixology_aga" to "varp.mixology_aga_points", "dbrow.omnishop_currency_mixology_lye" to "varp.mixology_lye_points")
    }
}

internal class MixologyContainerShopScript : PluginScript() {
    override fun ScriptContext.startup() {
        for (npc in listOf("npc.mm_lab_supervisor_1op", "npc.mm_lab_supervisor_2op")) onOpNpc1(npc) {
            val origin = coords
            if (menu("Supervisor Lalo", hotkeys = true, choices = listOf("View rewards", "Cancel")) == 0 && coords == origin && player.hitpoints > 0 && !player.pendingLogout && !player.loggingOut) openOmnishop(MixologyContainerRewards.SHOP)
        }
        onOpNpc3("npc.mm_lab_supervisor_2op") { if (player.hitpoints > 0 && !player.pendingLogout && !player.loggingOut) openOmnishop(MixologyContainerRewards.SHOP) }
    }
}
