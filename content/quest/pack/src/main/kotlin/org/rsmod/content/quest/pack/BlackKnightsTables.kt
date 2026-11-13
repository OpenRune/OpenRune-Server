package org.rsmod.content.quest.pack

import dev.openrune.definition.dbtables.dbTable
import dev.openrune.definition.util.VarType

object BlackKnightsTables {
    fun routes() =
        dbTable("dbtable.black_knights_routes", serverOnly = true) {
            column("loc", 0, VarType.STRING)
            column("x", 1, VarType.INT)
            column("z", 2, VarType.INT)
            column("level", 3, VarType.INT)
            column("dest_x", 4, VarType.INT)
            column("dest_z", 5, VarType.INT)
            column("dest_level", 6, VarType.INT)
            row("dbrow.bkn_ladder_0") {
                column(0, "loc.dk_ladder")
                column(1, 3015)
                column(2, 3519)
                column(3, 0)
                column(4, 3015)
                column(5, 3518)
                column(6, 1)
            }
            row("dbrow.bkn_ladder_1") {
                column(0, "loc.dk_laddertop")
                column(1, 3015)
                column(2, 3519)
                column(3, 1)
                column(4, 3015)
                column(5, 3518)
                column(6, 0)
            }
            row("dbrow.bkn_ladder_2") {
                column(0, "loc.dk_ladder")
                column(1, 3016)
                column(2, 3519)
                column(3, 1)
                column(4, 3016)
                column(5, 3518)
                column(6, 2)
            }
            row("dbrow.bkn_ladder_3") {
                column(0, "loc.dk_laddertop")
                column(1, 3016)
                column(2, 3519)
                column(3, 2)
                column(4, 3016)
                column(5, 3518)
                column(6, 1)
            }
            row("dbrow.bkn_ladder_4") {
                column(0, "loc.dk_ladder")
                column(1, 3017)
                column(2, 3516)
                column(3, 1)
                column(4, 3017)
                column(5, 3515)
                column(6, 2)
            }
            row("dbrow.bkn_ladder_5") {
                column(0, "loc.dk_laddertop")
                column(1, 3017)
                column(2, 3516)
                column(3, 2)
                column(4, 3017)
                column(5, 3515)
                column(6, 1)
            }
            row("dbrow.bkn_ladder_6") {
                column(0, "loc.dk_ladder")
                column(1, 3023)
                column(2, 3513)
                column(3, 1)
                column(4, 3023)
                column(5, 3514)
                column(6, 2)
            }
            row("dbrow.bkn_ladder_7") {
                column(0, "loc.dk_laddertop")
                column(1, 3023)
                column(2, 3513)
                column(3, 2)
                column(4, 3023)
                column(5, 3514)
                column(6, 1)
            }
            row("dbrow.bkn_ladder_8") {
                column(0, "loc.dk_ladder")
                column(1, 3025)
                column(2, 3513)
                column(3, 1)
                column(4, 3025)
                column(5, 3514)
                column(6, 2)
            }
            row("dbrow.bkn_ladder_9") {
                column(0, "loc.dk_laddertop")
                column(1, 3025)
                column(2, 3513)
                column(3, 2)
                column(4, 3025)
                column(5, 3514)
                column(6, 1)
            }
            row("dbrow.bkn_ladder_10") {
                column(0, "loc.dk_ladder")
                column(1, 3021)
                column(2, 3510)
                column(3, 0)
                column(4, 3022)
                column(5, 3510)
                column(6, 1)
            }
            row("dbrow.bkn_ladder_11") {
                column(0, "loc.dk_laddertop")
                column(1, 3021)
                column(2, 3510)
                column(3, 1)
                column(4, 3022)
                column(5, 3510)
                column(6, 0)
            }
            row("dbrow.bkn_ladder_12") {
                column(0, "loc.dk_meeting_ladder")
                column(1, 3022)
                column(2, 3518)
                column(3, 0)
                column(4, 3022)
                column(5, 3517)
                column(6, 1)
            }
            row("dbrow.bkn_ladder_13") {
                column(0, "loc.dk_meeting_laddertop")
                column(1, 3022)
                column(2, 3518)
                column(3, 1)
                column(4, 3022)
                column(5, 3517)
                column(6, 0)
            }
        }
}
