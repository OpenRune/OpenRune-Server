package org.rsmod.content.other.treasuretrails.pack

import dev.openrune.definition.dbtables.dbTable
import dev.openrune.definition.util.VarType

/** The item Charlie the Tramp asks for in each beginner skill challenge step. */
object TrailCharlieTaskTable {
    const val STEP = 0
    const val OBJ = 1

    fun tasks() =
        dbTable("dbtable.trail_charlie_task", serverOnly = true) {
            column("step", STEP, VarType.DBROW)
            column("obj", OBJ, VarType.OBJ)

            fun task(row: String, step: String, obj: String) {
                row(row) {
                    columnRSCM(STEP, step)
                    columnRSCM(OBJ, obj)
                }
            }

            task(
                "dbrow.trail_charlie_task_trout",
                "dbrow.cluehelper_skillchallenge_beginner_0",
                "obj.trout",
            )
            task(
                "dbrow.trail_charlie_task_pike",
                "dbrow.cluehelper_skillchallenge_beginner_1",
                "obj.pike",
            )
            task(
                "dbrow.trail_charlie_task_raw_herring",
                "dbrow.cluehelper_skillchallenge_beginner_2",
                "obj.raw_herring",
            )
            task(
                "dbrow.trail_charlie_task_raw_trout",
                "dbrow.cluehelper_skillchallenge_beginner_3",
                "obj.raw_trout",
            )
            task(
                "dbrow.trail_charlie_task_iron_ore",
                "dbrow.cluehelper_skillchallenge_beginner_4",
                "obj.iron_ore",
            )
            task(
                "dbrow.trail_charlie_task_iron_dagger",
                "dbrow.cluehelper_skillchallenge_beginner_5",
                "obj.iron_dagger",
            )
            task(
                "dbrow.trail_charlie_task_leather_body",
                "dbrow.cluehelper_skillchallenge_beginner_6",
                "obj.leather_armour",
            )
            task(
                "dbrow.trail_charlie_task_leather_chaps",
                "dbrow.cluehelper_skillchallenge_beginner_7",
                "obj.leather_chaps",
            )
        }
}
