package org.rsmod.content.quest.pack

import dev.openrune.definition.dbtables.dbTable
import dev.openrune.definition.util.VarType

object DaddysHomeFurnitureTable {
    const val LOC = 0
    const val VARBIT = 1
    const val REMAINING = 2
    const val PLANK = 3
    const val PLANK_NAME = 4
    const val PLANKS = 5
    const val NAILS = 6
    const val CLOTH = 7
    const val XP = 8
    const val OP = 9
    const val MENU_OBJ = 10
    const val LEVEL = 11

    private const val PLAIN_PLANK = "obj.woodplank"
    private const val WAXWOOD_PLANK = "obj.daddyshome_waxwood_plank"
    private const val MENU_LEVEL = 1
    private const val MENU_STOOL = "obj.beta_elder_wand"
    private const val MENU_CHAIR = "obj.cert_beta_elder_wand"
    private const val MENU_TABLE = "obj.beta_elder_maul"
    private const val MENU_BED = "obj.cert_beta_elder_maul"
    private const val MENU_CARPET = "obj.placeholder_beta_elder_maul"

    fun daddysHomeFurniture() =
        dbTable("dbtable.daddys_home_furniture", serverOnly = true) {
            column("loc", LOC, VarType.LOC)
            column("varbit", VARBIT, VarType.INT)
            column("remaining", REMAINING, VarType.STRING)
            column("plank", PLANK, VarType.OBJ)
            column("plank_name", PLANK_NAME, VarType.STRING)
            column("planks", PLANKS, VarType.INT)
            column("nails", NAILS, VarType.INT)
            column("cloth", CLOTH, VarType.INT)
            column("xp", XP, VarType.INT)
            column("op", OP, VarType.INT)
            column("menu_obj", MENU_OBJ, VarType.OBJ)
            column("level", LEVEL, VarType.INT)

            fun furniture(
                name: String,
                loc: String,
                varbit: String,
                remaining: String,
                plank: String,
                planks: Int,
                nails: Int,
                cloth: Int,
                xp: Int,
                menuObj: String,
                op: Int = 1,
            ) {
                row("dbrow.daddys_home_furniture_$name") {
                    columnRSCM(LOC, loc)
                    columnRSCM(VARBIT, varbit)
                    column(REMAINING, remaining)
                    columnRSCM(PLANK, plank)
                    column(PLANK_NAME, if (plank == WAXWOOD_PLANK) "waxwood plank" else "plank")
                    column(PLANKS, planks)
                    column(NAILS, nails)
                    column(CLOTH, cloth)
                    column(XP, xp)
                    column(OP, op)
                    columnRSCM(MENU_OBJ, menuObj)
                    column(LEVEL, MENU_LEVEL)
                }
            }

            furniture(
                "kitchen_stool",
                "loc.daddyshome_stool_1",
                "varbit.daddyshome_stool_1",
                "A broken stool in the kitchen",
                PLAIN_PLANK,
                planks = 1,
                nails = 2,
                cloth = 0,
                xp = 29,
                menuObj = MENU_STOOL,
            )
            furniture(
                "bedroom_stool",
                "loc.daddyshome_stool_2",
                "varbit.daddyshome_stool_2",
                "A broken stool in the bedroom",
                PLAIN_PLANK,
                planks = 1,
                nails = 2,
                cloth = 0,
                xp = 29,
                menuObj = MENU_STOOL,
            )
            furniture(
                "chair",
                "loc.daddyshome_chair",
                "varbit.daddyshome_chair",
                "A broken chair in the bedroom",
                PLAIN_PLANK,
                planks = 2,
                nails = 2,
                cloth = 0,
                xp = 58,
                menuObj = MENU_CHAIR,
            )
            furniture(
                "kitchen_table",
                "loc.daddyshome_table_1",
                "varbit.daddyshome_table_1",
                "A broken table in the kitchen",
                PLAIN_PLANK,
                planks = 3,
                nails = 4,
                cloth = 0,
                xp = 87,
                menuObj = MENU_TABLE,
            )
            furniture(
                "bedroom_table",
                "loc.daddyshome_table_2",
                "varbit.daddyshome_table_2",
                "A broken table in the bedroom",
                PLAIN_PLANK,
                planks = 3,
                nails = 4,
                cloth = 0,
                xp = 87,
                menuObj = MENU_TABLE,
            )
            furniture(
                "bed",
                "loc.daddyshome_bed",
                "varbit.daddyshome_bed",
                "The old campbed",
                WAXWOOD_PLANK,
                planks = 3,
                nails = 0,
                cloth = 2,
                xp = 207,
                menuObj = MENU_BED,
            )
            furniture(
                "carpet",
                "loc.daddyshome_carpet_middle",
                "varbit.daddyshome_carpet",
                "The rotten carpet",
                PLAIN_PLANK,
                planks = 0,
                nails = 0,
                cloth = 3,
                xp = 45,
                menuObj = MENU_CARPET,
                op = 5,
            )
        }
}
