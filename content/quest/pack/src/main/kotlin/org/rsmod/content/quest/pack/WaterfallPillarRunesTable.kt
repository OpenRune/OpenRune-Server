package org.rsmod.content.quest.pack

import dev.openrune.definition.dbtables.dbTable
import dev.openrune.definition.util.VarType

object WaterfallPillarRunesTable {
    const val RUNE = 0
    const val BIT = 1

    fun runes() =
        dbTable("dbtable.waterfall_pillar_runes", serverOnly = true) {
            column("rune", RUNE, VarType.OBJ)
            column("bit", BIT, VarType.INT)

            row("dbrow.waterfall_rune_air") {
                columnRSCM(RUNE, "obj.airrune")
                column(BIT, 0)
            }
            row("dbrow.waterfall_rune_water") {
                columnRSCM(RUNE, "obj.waterrune")
                column(BIT, 1)
            }
            row("dbrow.waterfall_rune_earth") {
                columnRSCM(RUNE, "obj.earthrune")
                column(BIT, 2)
            }
        }
}
