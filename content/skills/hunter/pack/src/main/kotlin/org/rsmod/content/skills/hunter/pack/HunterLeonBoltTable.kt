package org.rsmod.content.skills.hunter.pack

import dev.openrune.definition.dbtables.dbTable
import dev.openrune.definition.util.VarType

object HunterLeonBoltTable {
    const val SPIKE = 0
    const val BOLTS = 1
    const val FEE = 2

    fun leonBolts() =
        dbTable("dbtable.hunter_leon_bolts", serverOnly = true) {
            column("spike", SPIKE, VarType.OBJ)
            column("bolts", BOLTS, VarType.OBJ)
            column("fee", FEE, VarType.INT)

            fun bolt(row: String, spike: String, bolts: String, fee: Int) =
                row(row) {
                    columnRSCM(SPIKE, spike)
                    columnRSCM(BOLTS, bolts)
                    column(FEE, fee)
                }

            bolt("dbrow.hunter_leon_bolts_kebbit", "obj.huntingbeast_spike", "obj.huntingbow_bolts", 20)
            bolt("dbrow.hunter_leon_bolts_long", "obj.huntingbeast_bigspike", "obj.huntingbow_bigbolts", 40)
        }
}
