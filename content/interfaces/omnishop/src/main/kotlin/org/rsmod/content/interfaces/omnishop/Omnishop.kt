package org.rsmod.content.interfaces.omnishop

import dev.openrune.ServerCacheManager
import dev.openrune.definition.type.widget.IfEvent
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import dev.openrune.types.ItemServerType
import dev.openrune.types.dbcol.DbHelper
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.vars.intVarp
import org.rsmod.game.entity.Player

internal var Player.omnishopSelectedId by intVarp("varp.omnishop_selected_id")
internal var Player.omnishopSelectedSideId by intVarp("varp.omnishop_selected_side_id")
internal var Player.omnishopLastShop by intVarp("varp.omnishop_lastshop")

/** Opens the data-driven omnishop interface for the `dbrow.<shop>` row of `omnishop_shop_data`. */
public fun ProtectedAccess.openOmnishop(shopRow: String) {
    val row = shopRow.asRSCM(RSCMType.DBROW)
    player.omnishopSelectedSideId = -1
    player.omnishopSelectedId = -1
    player.omnishopLastShop = row
    ifOpenMainSidePair(main = "interface.omnishop_main", side = "interface.omnishop_side")
    runClientScript("clientscript.[clientscript,omnishop_main_init]".asRSCM(RSCMType.CLIENTSCRIPT), row, -1, -1)
    runClientScript("clientscript.[clientscript,omnishop_side_init]".asRSCM(RSCMType.CLIENTSCRIPT), row)
    ifSetEvents(
        "component.omnishop_main:list",
        0..LIST_SLOTS,
        IfEvent.Op1,
        IfEvent.Op2,
        IfEvent.Op3,
        IfEvent.Op4,
        IfEvent.Op5,
        IfEvent.Op6,
        IfEvent.Op10,
    )
    ifSetEvents(
        "component.omnishop_side:items",
        0..SIDE_SLOTS,
        IfEvent.Op1,
        IfEvent.Op2,
        IfEvent.Op3,
        IfEvent.Op4,
        IfEvent.Op5,
        IfEvent.Op6,
        IfEvent.Op10,
        IfEvent.Depth1,
        IfEvent.DragTarget,
    )
    ifSetEvents("component.omnishop_side:info_layer", 0..0, IfEvent.Op1)
    ifSetEvents("component.omnishop_main:trigger_buy", -1..-1, IfEvent.ScriptTrigger)
    ifSetEvents("component.omnishop_main:trigger_examine", -1..-1, IfEvent.ScriptTrigger)
    ifSetEvents("component.omnishop_main:trigger_request_info", -1..-1, IfEvent.ScriptTrigger)
    ifSetEvents("component.omnishop_main:dropdown_content", 0..DROPDOWN_SLOTS, IfEvent.Op1)
}

/**
 * Reads omnishop rows straight from the raw column values: several stock columns are typed
 * `namedobj` in the cache, which the generated table classes reject as a literal mismatch.
 */
internal class RawDbRow(id: Int) {
    private val helper = DbHelper.row(id)

    fun ints(column: String): List<Int> =
        runCatching { helper.getColumn(column).column.values }
            .getOrNull()
            ?.map { (it as Number).toInt() }
            ?: emptyList()

    fun int(column: String): Int? = ints(column).firstOrNull()

    fun bool(column: String): Boolean? = int(column)?.let { it == 1 }
}

internal class OmnishopCurrency(val row: Int) {
    private val raw = RawDbRow(row)

    val objs: List<ItemServerType>
        get() = raw.ints(CURRENCY_OBJ).mapNotNull { ServerCacheManager.getItem(it) }

    val pluralName: String
        get() = runCatching { DbHelper.row(row).getColumn(CURRENCY_PLURAL).column.values }
            .getOrNull()
            ?.firstOrNull() as? String ?: "currency"
}

internal class OmnishopStock(private val shop: RawDbRow, private val stock: RawDbRow) {
    val obj: ItemServerType =
        ServerCacheManager.getItem(stock.int(STOCK_OBJ) ?: -1) ?: error("Omnishop stock has no obj")

    val multiplier: Int
        get() = stock.int(STOCK_MULTIPLIER)?.takeIf { it > 0 } ?: 1

    val buyable: Boolean
        get() = stock.bool(STOCK_BUYABLE) ?: true

    val sellable: Boolean
        get() = stock.bool(STOCK_SELLABLE) ?: false

    fun buyCosts(): List<Pair<OmnishopCurrency, Int>> =
        costs(stock.int(STOCK_MOD_BUY) ?: shop.int(SHOP_MOD_BUY) ?: DEFAULT_MOD, minimum = 1)

    fun sellCosts(): List<Pair<OmnishopCurrency, Int>> =
        costs(stock.int(STOCK_MOD_SELL) ?: shop.int(SHOP_MOD_SELL) ?: DEFAULT_MOD, minimum = 0)

    private fun costs(mod: Int, minimum: Int): List<Pair<OmnishopCurrency, Int>> =
        stock.ints(STOCK_COST).chunked(2).filter { it.size == 2 }.map { (currency, base) ->
            val scaled = (base.toLong() * mod.coerceAtLeast(MIN_MOD) / DEFAULT_MOD).toInt()
            val price = if (base == 0) 0 else scaled.coerceAtLeast(minimum)
            OmnishopCurrency(currency) to price
        }

    fun description(): String {
        val param = ServerCacheManager.getParam("param.omnishop_item_info".asRSCM(RSCMType.PARAM))
        val text = param?.let { obj.paramOrNull<String>(it) }
        return text ?: obj.examine
    }

    companion object {
        fun find(shopRow: Int, index: Int): OmnishopStock? {
            val shop = runCatching { RawDbRow(shopRow) }.getOrNull() ?: return null
            val stockRow = shop.ints(SHOP_STOCK).getOrNull(index) ?: return null
            return runCatching { OmnishopStock(shop, RawDbRow(stockRow)) }.getOrNull()
        }

        fun all(shopRow: Int): List<OmnishopStock> {
            val shop = runCatching { RawDbRow(shopRow) }.getOrNull() ?: return emptyList()
            return shop.ints(SHOP_STOCK).mapNotNull { runCatching { OmnishopStock(shop, RawDbRow(it)) }.getOrNull() }
        }
    }
}

private const val LIST_SLOTS = 288
private const val SIDE_SLOTS = 28
private const val DROPDOWN_SLOTS = 8
private const val DEFAULT_MOD = 1000
private const val MIN_MOD = 100

private const val SHOP_STOCK = "dbcol.omnishop_shop_data:omnishop_shop_stock"
private const val SHOP_MOD_BUY = "dbcol.omnishop_shop_data:omnishop_shop_cost_mod_buy"
private const val SHOP_MOD_SELL = "dbcol.omnishop_shop_data:omnishop_shop_cost_mod_sell"
private const val STOCK_OBJ = "dbcol.omnishop_stock_data:omnishop_stock_obj"
private const val STOCK_COST = "dbcol.omnishop_stock_data:omnishop_stock_cost"
private const val STOCK_MOD_BUY = "dbcol.omnishop_stock_data:omnishop_stock_cost_mod_buy"
private const val STOCK_MOD_SELL = "dbcol.omnishop_stock_data:omnishop_stock_cost_mod_sell"
private const val STOCK_MULTIPLIER = "dbcol.omnishop_stock_data:omnishop_stock_transaction_multiplier"
private const val STOCK_BUYABLE = "dbcol.omnishop_stock_data:omnishop_stock_buyable"
private const val STOCK_SELLABLE = "dbcol.omnishop_stock_data:omnishop_stock_sellable"
private const val CURRENCY_OBJ = "dbcol.omnishop_currency_data:omnishop_currency_obj"
private const val CURRENCY_PLURAL = "dbcol.omnishop_currency_data:omnishop_currency_name_plural"
