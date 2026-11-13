package org.rsmod.content.other.treasuretrails.pack

import dev.openrune.definition.dbtables.dbTable
import dev.openrune.definition.util.VarType

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

            task(
                "dbrow.trail_sherlock_task_dragon_scimitar",
                "dbrow.cluehelper_skillchallenge_elite_0",
                "obj.dragon_scimitar",
            )
            task(
                "dbrow.trail_sherlock_task_nature_rune",
                "dbrow.cluehelper_skillchallenge_elite_2",
                "obj.naturerune",
            )
            task(
                "dbrow.trail_sherlock_task_super_defence",
                "dbrow.cluehelper_skillchallenge_elite_6",
                "obj.3dose2defense",
            )
            task(
                "dbrow.trail_sherlock_task_green_dhide_body",
                "dbrow.cluehelper_skillchallenge_elite_8",
                "obj.dragonhide_body",
            )
            task(
                "dbrow.trail_sherlock_task_yew_longbow",
                "dbrow.cluehelper_skillchallenge_elite_9",
                "obj.yew_longbow",
            )
            task(
                "dbrow.trail_sherlock_task_black_warlock",
                "dbrow.cluehelper_skillchallenge_elite_11",
                "obj.butterfly_jar_warlock",
            )
            task(
                "dbrow.trail_sherlock_task_red_chinchompa",
                "dbrow.cluehelper_skillchallenge_elite_12",
                "obj.chinchompa_big_captured",
            )
            task(
                "dbrow.trail_sherlock_task_mithril_ore",
                "dbrow.cluehelper_skillchallenge_elite_13",
                "obj.mithril_ore",
            )
            task(
                "dbrow.trail_sherlock_task_mithril_2h",
                "dbrow.cluehelper_skillchallenge_elite_14",
                "obj.mithril_2h_sword",
            )
            task(
                "dbrow.trail_sherlock_task_raw_shark",
                "dbrow.cluehelper_skillchallenge_elite_15",
                "obj.raw_shark",
            )
            task(
                "dbrow.trail_sherlock_task_yew_logs",
                "dbrow.cluehelper_skillchallenge_elite_16",
                "obj.yew_logs",
            )
            task(
                "dbrow.trail_sherlock_task_swordfish",
                "dbrow.cluehelper_skillchallenge_elite_19",
                "obj.swordfish",
            )
        }
}
