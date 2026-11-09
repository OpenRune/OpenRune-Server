package org.rsmod.api.shops.currency

import dev.openrune.rscm.RSCM
import dev.openrune.rscm.RSCMType
import dev.openrune.types.ItemServerType
import dev.openrune.types.dbcol.DbHelper
import dev.openrune.types.dbcol.intOptional
import dev.openrune.types.dbcol.objOptional
import dev.openrune.types.dbcol.string
import org.rsmod.game.entity.Player

public object ShopCurrencyRows {
    public fun all(): List<ShopCurrencyRow> =
        DbHelper.table(TABLE).map { row ->
            ShopCurrencyRow(
                key = row.string("dbcol.shop_currency:key"),
                singularName = row.string("dbcol.shop_currency:singular_name"),
                pluralName = row.string("dbcol.shop_currency:plural_name"),
                obj = row.objOptional("dbcol.shop_currency:obj"),
                varbitId = row.intOptional("dbcol.shop_currency:varbit"),
            )
        }

    public fun toShopCurrency(
        row: ShopCurrencyRow,
        sync: (Player) -> Unit = {},
    ): ShopCurrency {
        val obj = row.obj
        val varbitId = row.varbitId
        return when {
            obj != null && varbitId == null ->
                InvObjShopCurrency(
                    invObj = obj,
                    singularName = row.singularName,
                    pluralName = row.pluralName,
                )
            varbitId != null && obj == null ->
                VarBitShopCurrency(
                    varbit = RSCM.getReverseMapping(RSCMType.VARBIT, varbitId),
                    singularName = row.singularName,
                    pluralName = row.pluralName,
                    sync = sync,
                )
            obj != null && varbitId != null ->
                error("shop_currency row `${row.key}` must set only one of obj or varbit")
            else -> error("shop_currency row `${row.key}` must set obj or varbit")
        }
    }

    private const val TABLE = "dbtable.shop_currency"
}

public data class ShopCurrencyRow(
    public val key: String,
    public val singularName: String,
    public val pluralName: String,
    public val obj: ItemServerType?,
    public val varbitId: Int?,
)
