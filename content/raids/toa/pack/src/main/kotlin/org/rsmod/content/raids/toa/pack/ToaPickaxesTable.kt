package org.rsmod.content.raids.toa.pack

import dev.openrune.definition.dbtables.DBTable
import dev.openrune.definition.dbtables.dbTable
import dev.openrune.definition.util.VarType

object ToaPickaxesTable {

    const val VALUE = 0
    const val OBJ = 1

    private val PICKAXES =
        listOf(
            "iron_pickaxe",
            "steel_pickaxe",
            "black_pickaxe",
            "mithril_pickaxe",
            "adamant_pickaxe",
            "rune_pickaxe",
            "trail_gilded_pickaxe",
            "dragon_pickaxe",
            "dragon_pickaxe_pretty",
            "zalcano_pickaxe",
            "infernal_pickaxe",
            "infernal_pickaxe_empty",
            "3a_pickaxe",
            "crystal_pickaxe",
            "crystal_pickaxe_inactive",
        )

    fun pickaxes(): DBTable =
        dbTable("dbtable.toa_pickaxes", serverOnly = true) {
            column("value", VALUE, VarType.INT)
            column("obj", OBJ, VarType.OBJ)

            PICKAXES.forEachIndexed { index, obj ->
                row("dbrow.toa_pickaxe_${obj.removePrefix("obj.")}") {
                    column(VALUE, index + 1)
                    columnRSCM(OBJ, "obj.$obj")
                }
            }
        }
}
