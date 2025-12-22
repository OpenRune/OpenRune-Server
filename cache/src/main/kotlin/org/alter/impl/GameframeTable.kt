package org.alter.impl

import dev.openrune.definition.dbtables.dbTable
import dev.openrune.definition.util.VarType

object GameframeTable {

    const val COL_TOP_LEVEL = 0
    const val COL_MAPPINGS = 1
    const val COL_CLIENT_MODE = 2
    const val COL_RESIZABLE = 3
    const val COL_IS_DEFAULT = 4
    const val COL_STONE_ARRANGEMENT = 5

    fun gameframe() = dbTable("tables.gameframe") {


        column("toplevel", COL_TOP_LEVEL, VarType.INT)
        column("mappings", COL_MAPPINGS, VarType.ENUM)
        column("client_mode", COL_CLIENT_MODE, VarType.INT)
        column("resizable", COL_RESIZABLE, VarType.BOOLEAN)
        column("default", COL_IS_DEFAULT, VarType.BOOLEAN)
        column("stone_arrangement", COL_STONE_ARRANGEMENT, VarType.BOOLEAN)

        row("dbrows.gameframe_toplevel") {
            columnRSCM(COL_TOP_LEVEL,"interfaces.toplevel")
            columnRSCM(COL_MAPPINGS,"enums.fixed_pane_redirect")
            column(COL_CLIENT_MODE,0)

            column(COL_RESIZABLE,false)
            column(COL_IS_DEFAULT,true)
            column(COL_STONE_ARRANGEMENT,false)
        }

        row("dbrows.gameframe_osrs_stretch") {
            columnRSCM(COL_TOP_LEVEL,"interfaces.toplevel_osrs_stretch")
            columnRSCM(COL_MAPPINGS,"enums.resizable_basic_pane_redirect")
            column(COL_CLIENT_MODE,1)

            column(COL_RESIZABLE,true)
            column(COL_IS_DEFAULT,false)
            column(COL_STONE_ARRANGEMENT,false)
        }

        row("dbrows.gameframe_pre_eoc") {
            columnRSCM(COL_TOP_LEVEL,"interfaces.toplevel_pre_eoc")
            columnRSCM(COL_MAPPINGS,"enums.side_panels_resizable_pane_redirect")
            column(COL_CLIENT_MODE,2)

            column(COL_RESIZABLE,true)
            column(COL_IS_DEFAULT,false)
            column(COL_STONE_ARRANGEMENT,true)
        }

        row("dbrows.gameframe_fullscreen") {
            columnRSCM(COL_TOP_LEVEL,"interfaces.toplevel_display")
            columnRSCM(COL_MAPPINGS,"enums.fullscreen_pane")
            column(COL_CLIENT_MODE,2)

            column(COL_RESIZABLE,true)
            column(COL_IS_DEFAULT,false)
            column(COL_STONE_ARRANGEMENT,true)
        }



    }

}