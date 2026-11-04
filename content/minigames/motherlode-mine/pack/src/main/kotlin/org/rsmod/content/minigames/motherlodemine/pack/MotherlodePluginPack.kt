package org.rsmod.content.minigames.motherlodemine.pack

import dev.openrune.definition.dbtables.dbTable
import dev.openrune.definition.util.VarType
import dev.openrune.pack.PluginPack

class MotherlodePluginPack : PluginPack() {
    override fun dbTables() = listOf(
        dbTable("dbtable.motherlode_ores", serverOnly = true) {
            column("ore", 0, VarType.OBJ)
            column("level", 1, VarType.INT)
            column("success_low", 2, VarType.INT)
            column("success_high", 3, VarType.INT)
            column("xp", 4, VarType.INT)
            column("roll_order", 5, VarType.INT)
            column("sack_order", 6, VarType.INT)
            row("dbrow.motherlode_runite") {
                columnRSCM(0, "obj.runite_ore")
                column(1, 85)
                column(2, -20)
                column(3, 5)
                column(4, 750)
                column(5, 1)
                column(6, 1)
            }
            row("dbrow.motherlode_adamantite") {
                columnRSCM(0, "obj.adamantite_ore")
                column(1, 70)
                column(2, -90)
                column(3, 50)
                column(4, 450)
                column(5, 2)
                column(6, 2)
            }
            row("dbrow.motherlode_mithril") {
                columnRSCM(0, "obj.mithril_ore")
                column(1, 55)
                column(2, -19)
                column(3, 90)
                column(4, 300)
                column(5, 3)
                column(6, 3)
            }
            row("dbrow.motherlode_gold") {
                columnRSCM(0, "obj.gold_ore")
                column(1, 40)
                column(2, -40)
                column(3, 126)
                column(4, 150)
                column(5, 4)
                column(6, 4)
            }
            row("dbrow.motherlode_coal") {
                columnRSCM(0, "obj.coal")
                column(1, 30)
                column(2, 0)
                column(3, 0)
                column(4, 150)
                column(5, 0)
                column(6, 5)
            }
            row("dbrow.motherlode_nugget") {
                columnRSCM(0, "obj.motherlode_nugget")
                column(1, 30)
                column(2, 0)
                column(3, 0)
                column(4, 0)
                column(5, 0)
                column(6, 0)
            }
        },
    )
}
