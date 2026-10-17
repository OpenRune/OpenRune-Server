package org.rsmod.content.quest.pack

import dev.openrune.definition.dbtables.dbTable
import dev.openrune.definition.util.VarType

object DaddysHomeCrateTable {
    const val OBJ = 0
    const val COUNT = 1

    fun daddysHomeCrate() =
        dbTable("dbtable.daddys_home_crate", serverOnly = true) {
            column("obj", OBJ, VarType.OBJ)
            column("count", COUNT, VarType.INT)

            fun item(name: String, obj: String, count: Int) {
                row("dbrow.daddys_home_crate_$name") {
                    columnRSCM(OBJ, obj)
                    column(COUNT, count)
                }
            }

            item("planks", "obj.cert_woodplank", 25)
            item("nails", "obj.nails_mithril", 50)
            item("steel_bars", "obj.cert_steel_bar", 5)
            item("oak_planks", "obj.cert_plank_oak", 10)
            item("cloth", "obj.cert_cloth", 8)
            item("house_tablets", "obj.poh_tablet_teleporttohouse", 5)
            item("falador_tablet", "obj.poh_tablet_faladorteleport", 1)
        }
}
