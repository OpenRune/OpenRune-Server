package org.rsmod.content.other.sawmill.pack

import dev.openrune.definition.dbtables.dbTable
import dev.openrune.definition.util.VarType

object SawmillPlanksTable {
    const val LOGS = 0
    const val PLANK = 1
    const val PRICE = 2

    fun sawmillPlanks() =
        dbTable("dbtable.sawmill_planks", serverOnly = true) {
            column("logs", LOGS, VarType.OBJ)
            column("plank", PLANK, VarType.OBJ)
            column("price", PRICE, VarType.INT)

            fun plank(name: String, logs: String, plank: String, price: Int) {
                row("dbrow.sawmill_plank_$name") {
                    columnRSCM(LOGS, logs)
                    columnRSCM(PLANK, plank)
                    column(PRICE, price)
                }
            }

            plank("wood", "obj.logs", "obj.woodplank", 100)
            plank("oak", "obj.oak_logs", "obj.plank_oak", 250)
            plank("teak", "obj.teak_logs", "obj.plank_teak", 500)
            plank("mahogany", "obj.mahogany_logs", "obj.plank_mahogany", 1500)
            plank("camphor", "obj.camphor_logs", "obj.plank_camphor", 2500)
            plank("ironwood", "obj.ironwood_logs", "obj.plank_ironwood", 5000)
            plank("rosewood", "obj.rosewood_logs", "obj.plank_rosewood", 7500)
        }
}
