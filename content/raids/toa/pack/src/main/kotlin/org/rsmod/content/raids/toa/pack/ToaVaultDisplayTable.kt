package org.rsmod.content.raids.toa.pack

import dev.openrune.definition.dbtables.DBTable
import dev.openrune.definition.dbtables.dbTable
import dev.openrune.definition.util.VarType

object ToaVaultDisplayTable {

    const val OBJ = 0
    const val SEQ = 1

    private val DISPLAYS =
        listOf(
            "lightbearer" to "fx_cycle_ring",
            "masori_mask" to "fx_cycle_masori_head",
            "masori_body" to "fx_cycle_masori_top",
            "masori_chaps" to "fx_cycle_masori_legs",
            "osmumtens_fang" to "fx_cycle_sword",
            "elidinis_ward" to "fx_cycle_ward",
            "tumekens_shadow_uncharged" to "fx_cycle_staff",
        )

    fun displays(): DBTable =
        dbTable("dbtable.toa_vault_displays", serverOnly = true) {
            column("obj", OBJ, VarType.OBJ)
            column("seq", SEQ, VarType.SEQ)

            for ((obj, seq) in DISPLAYS) {
                row("dbrow.toa_vault_display_$obj") {
                    columnRSCM(OBJ, "obj.$obj")
                    columnRSCM(SEQ, "seq.$seq")
                }
            }
        }
}
