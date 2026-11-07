package dev.openrune.tables.skills

import dev.openrune.definition.dbtables.dbTable
import dev.openrune.definition.util.VarType

object Motherlode {

    const val COL_KEY = 0
    const val COL_ORE_ITEM = 1
    const val COL_LEVEL = 2
    const val COL_XP = 3
    const val COL_SUCCESS_RATE_LOW = 4
    const val COL_SUCCESS_RATE_HIGH = 5

    /**
     * Pay-dirt rolls each row from the top down and keeps the first success, so row order is the
     * ore tier order. The last row is the fallback awarded when every other roll fails.
     */
    fun payDirt() =
        dbTable("dbtable.motherlode_paydirt", serverOnly = true) {
            column("key", COL_KEY, VarType.STRING)
            column("ore_item", COL_ORE_ITEM, VarType.OBJ)
            column("level", COL_LEVEL, VarType.INT)
            column("xp", COL_XP, VarType.INT)
            column("success_rate_low", COL_SUCCESS_RATE_LOW, VarType.INT)
            column("success_rate_high", COL_SUCCESS_RATE_HIGH, VarType.INT)

            row("dbrow.motherlode_paydirt_nugget") {
                column(COL_KEY, "NUGGET")
                columnRSCM(COL_ORE_ITEM, "obj.motherlode_nugget")
                column(COL_LEVEL, 30)
                column(COL_XP, 0)
                column(COL_SUCCESS_RATE_LOW, 7)
                column(COL_SUCCESS_RATE_HIGH, 7)
            }

            row("dbrow.motherlode_paydirt_runite") {
                column(COL_KEY, "RUNITE")
                columnRSCM(COL_ORE_ITEM, "obj.runite_ore")
                column(COL_LEVEL, 85)
                column(COL_XP, 750)
                column(COL_SUCCESS_RATE_LOW, -20)
                column(COL_SUCCESS_RATE_HIGH, 5)
            }

            row("dbrow.motherlode_paydirt_adamantite") {
                column(COL_KEY, "ADAMANTITE")
                columnRSCM(COL_ORE_ITEM, "obj.adamantite_ore")
                column(COL_LEVEL, 70)
                column(COL_XP, 450)
                column(COL_SUCCESS_RATE_LOW, -90)
                column(COL_SUCCESS_RATE_HIGH, 50)
            }

            row("dbrow.motherlode_paydirt_mithril") {
                column(COL_KEY, "MITHRIL")
                columnRSCM(COL_ORE_ITEM, "obj.mithril_ore")
                column(COL_LEVEL, 55)
                column(COL_XP, 300)
                column(COL_SUCCESS_RATE_LOW, -19)
                column(COL_SUCCESS_RATE_HIGH, 90)
            }

            row("dbrow.motherlode_paydirt_gold") {
                column(COL_KEY, "GOLD")
                columnRSCM(COL_ORE_ITEM, "obj.gold_ore")
                column(COL_LEVEL, 40)
                column(COL_XP, 150)
                column(COL_SUCCESS_RATE_LOW, -40)
                column(COL_SUCCESS_RATE_HIGH, 126)
            }

            row("dbrow.motherlode_paydirt_coal") {
                column(COL_KEY, "COAL")
                columnRSCM(COL_ORE_ITEM, "obj.coal")
                column(COL_LEVEL, 30)
                column(COL_XP, 150)
                column(COL_SUCCESS_RATE_LOW, 0)
                column(COL_SUCCESS_RATE_HIGH, 0)
            }
        }
}
