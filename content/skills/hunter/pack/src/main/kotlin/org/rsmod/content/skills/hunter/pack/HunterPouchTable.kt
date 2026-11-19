package org.rsmod.content.skills.hunter.pack

import dev.openrune.definition.dbtables.dbTable
import dev.openrune.definition.util.VarType

object HunterPouchTable {
    const val CLOSED = 0
    const val OPEN = 1
    const val CAPACITY = 2
    const val KIND = 3

    const val FUR = 0
    const val MEAT = 1

    fun pouches() =
        dbTable("dbtable.hunter_pouches", serverOnly = true) {
            column("closed", CLOSED, VarType.OBJ)
            column("open", OPEN, VarType.OBJ)
            column("capacity", CAPACITY, VarType.INT)
            column("kind", KIND, VarType.INT)

            fun pouch(row: String, closed: String, open: String, capacity: Int, kind: Int) =
                row(row) {
                    columnRSCM(CLOSED, closed)
                    columnRSCM(OPEN, open)
                    column(CAPACITY, capacity)
                    column(KIND, kind)
                }

            pouch("dbrow.hunter_pouch_fur_small", "obj.hg_furpouch_small", "obj.hg_furpouch_small_open", 14, FUR)
            pouch("dbrow.hunter_pouch_fur_medium", "obj.hg_furpouch_med", "obj.hg_furpouch_med_open", 21, FUR)
            pouch("dbrow.hunter_pouch_fur_large", "obj.hg_furpouch_large", "obj.hg_furpouch_large_open", 28, FUR)
            pouch("dbrow.hunter_pouch_meat_small", "obj.hg_meatpouch_small", "obj.hg_meatpouch_small_open", 14, MEAT)
            pouch("dbrow.hunter_pouch_meat_large", "obj.hg_meatpouch_large", "obj.hg_meatpouch_large_open", 28, MEAT)
        }
}
