package dev.openrune.tables.skills

import dev.openrune.definition.dbtables.dbTable
import dev.openrune.definition.util.VarType

object Slayer {

    const val COL_MASTER_ID = 0
    const val COL_NPC_IDS = 1
    const val COL_SLAYER_LEVEL = 2
    const val COL_COMBAT_LEVEL = 3
    const val COL_POINTS_PER_TASK = 4
    const val COL_BLOCK_VARBITS = 5
    const val COL_CAN_ASSIGN_BOSSES = 6
    const val COL_BLOCK_COST = 7
    const val COL_DIARY_VARBIT = 8
    const val COL_DIARY_POINTS_PER_TASK = 9
    const val COL_BOSSES = 10
    const val COL_EXCLUDED_BOSSES = 11

    const val COL_BONUS_INTERVAL = 0
    const val COL_BONUS_MULTIPLIER = 1

    fun masters() = dbTable("dbtable.slayer_masters", serverOnly = true) {
        column("master_id", COL_MASTER_ID, VarType.INT)
        column("npc_ids", COL_NPC_IDS, VarType.NPC)
        column("slayer_level", COL_SLAYER_LEVEL, VarType.INT)
        column("combat_level", COL_COMBAT_LEVEL, VarType.INT)
        column("points_per_task", COL_POINTS_PER_TASK, VarType.INT)
        column("block_varbits", COL_BLOCK_VARBITS, VarType.INT)
        column("assign_bosses", COL_CAN_ASSIGN_BOSSES, VarType.BOOLEAN)
        column("block_cost", COL_BLOCK_COST, VarType.INT)
        column("diary_varbit", COL_DIARY_VARBIT, VarType.INT)
        column("diary_points_per_task", COL_DIARY_POINTS_PER_TASK, VarType.INT)
        column("bosses", COL_BOSSES, VarType.DBROW)
        column("excluded_bosses", COL_EXCLUDED_BOSSES, VarType.DBROW)

        row("dbrow.turael_aya") {
            column(COL_MASTER_ID, 1)
            columnRSCM(COL_NPC_IDS, "npc.slayer_master_1_tureal", "npc.slayer_master_1_aya")
            column(COL_SLAYER_LEVEL, 1)
            column(COL_COMBAT_LEVEL, 1)
            column(COL_POINTS_PER_TASK, 0)
            columnRSCM(COL_BLOCK_VARBITS,
                "varbit.slayer_blocked_turael_1",
                "varbit.slayer_blocked_turael_2",
                "varbit.slayer_blocked_turael_3",
                "varbit.slayer_blocked_turael_4",
                "varbit.slayer_blocked_turael_5",
                "varbit.slayer_blocked_turael_6",
                "varbit.slayer_blocked_turael_diary"
            )
            column(COL_CAN_ASSIGN_BOSSES, false)
            column(COL_BLOCK_COST, 40)
        }

        row("dbrow.spira") {
            column(COL_MASTER_ID, 9)
            columnRSCM(COL_NPC_IDS, "npc.slayer_master_9_active", "npc.porcine_spria")
            column(COL_SLAYER_LEVEL, 1)
            column(COL_COMBAT_LEVEL, 1)
            column(COL_POINTS_PER_TASK, 0)
            columnRSCM(COL_BLOCK_VARBITS,
                "varbit.slayer_blocked_turael_1",
                "varbit.slayer_blocked_turael_2",
                "varbit.slayer_blocked_turael_3",
                "varbit.slayer_blocked_turael_4",
                "varbit.slayer_blocked_turael_5",
                "varbit.slayer_blocked_turael_6",
                "varbit.slayer_blocked_turael_diary"
            )
            column(COL_CAN_ASSIGN_BOSSES, false)
            column(COL_BLOCK_COST, 40)
        }

        row("dbrow.krystilia") {
            column(COL_MASTER_ID, 7)
            columnRSCM(COL_NPC_IDS, "npc.slayer_master_7")
            column(COL_SLAYER_LEVEL, 1)
            column(COL_COMBAT_LEVEL, 1)
            column(COL_POINTS_PER_TASK, 25)
            columnRSCM(COL_BLOCK_VARBITS,
                "varbit.slayer_blocked_krystilia_1",
                "varbit.slayer_blocked_krystilia_2",
                "varbit.slayer_blocked_krystilia_3",
                "varbit.slayer_blocked_krystilia_4",
                "varbit.slayer_blocked_krystilia_5",
                "varbit.slayer_blocked_krystilia_6",
                "varbit.slayer_blocked_krystilia_diary"
            )
            column(COL_CAN_ASSIGN_BOSSES, false)
            column(COL_BLOCK_COST, 100)
            columnRSCM(
                COL_BOSSES,
                "dbrow.slayer_boss_task_callisto",
                "dbrow.slayer_boss_task_venenatis",
                "dbrow.slayer_boss_task_vetion",
                "dbrow.slayer_boss_task_chaoselemental",
                "dbrow.slayer_boss_task_chaosfanatic",
                "dbrow.slayer_boss_task_crazyarchaeologist",
                "dbrow.slayer_boss_task_scorpia",
            )
        }

        row("dbrow.mazchna_achtryn") {
            column(COL_MASTER_ID, 2)
            columnRSCM(COL_NPC_IDS, "npc.slayer_master_2_mazchna", "npc.slayer_master_2_achtryn_vis")
            column(COL_SLAYER_LEVEL, 1)
            column(COL_COMBAT_LEVEL, 20)
            column(COL_POINTS_PER_TASK, 6)
            columnRSCM(COL_BLOCK_VARBITS,
                "varbit.slayer_blocked_mazchna_1",
                "varbit.slayer_blocked_mazchna_2",
                "varbit.slayer_blocked_mazchna_3",
                "varbit.slayer_blocked_mazchna_4",
                "varbit.slayer_blocked_mazchna_5",
                "varbit.slayer_blocked_mazchna_6",
                "varbit.slayer_blocked_mazchna_diary"
            )
            column(COL_CAN_ASSIGN_BOSSES, false)
            column(COL_BLOCK_COST, 50)
        }

        row("dbrow.vannaka") {
            column(COL_MASTER_ID, 3)
            columnRSCM(COL_NPC_IDS, "npc.slayer_master_3")
            column(COL_SLAYER_LEVEL, 1)
            column(COL_COMBAT_LEVEL, 40)
            column(COL_POINTS_PER_TASK, 8)
            columnRSCM(COL_BLOCK_VARBITS,
                "varbit.slayer_blocked_vannaka_1",
                "varbit.slayer_blocked_vannaka_2",
                "varbit.slayer_blocked_vannaka_3",
                "varbit.slayer_blocked_vannaka_4",
                "varbit.slayer_blocked_vannaka_5",
                "varbit.slayer_blocked_vannaka_6",
                "varbit.slayer_blocked_vannaka_diary"
            )
            column(COL_CAN_ASSIGN_BOSSES, false)
            column(COL_BLOCK_COST, 60)
        }

        row("dbrow.chaeldar") {
            column(COL_MASTER_ID, 4)
            columnRSCM(COL_NPC_IDS, "npc.slayer_master_4")
            column(COL_SLAYER_LEVEL, 1)
            column(COL_COMBAT_LEVEL, 70)
            column(COL_POINTS_PER_TASK, 10)
            columnRSCM(COL_BLOCK_VARBITS,
                "varbit.slayer_blocked_chaeldar_1",
                "varbit.slayer_blocked_chaeldar_2",
                "varbit.slayer_blocked_chaeldar_3",
                "varbit.slayer_blocked_chaeldar_4",
                "varbit.slayer_blocked_chaeldar_5",
                "varbit.slayer_blocked_chaeldar_6",
                "varbit.slayer_blocked_chaeldar_diary"
            )
            column(COL_CAN_ASSIGN_BOSSES, false)
            column(COL_BLOCK_COST, 70)
        }

        row("dbrow.konar") {
            column(COL_MASTER_ID, 8)
            columnRSCM(COL_NPC_IDS, "npc.slayer_master_8")
            column(COL_SLAYER_LEVEL, 1)
            column(COL_COMBAT_LEVEL, 75)
            column(COL_POINTS_PER_TASK, 18)
            columnRSCM(COL_BLOCK_VARBITS,
                "varbit.slayer_blocked_konar_1",
                "varbit.slayer_blocked_konar_2",
                "varbit.slayer_blocked_konar_3",
                "varbit.slayer_blocked_konar_4",
                "varbit.slayer_blocked_konar_5",
                "varbit.slayer_blocked_konar_6",
                "varbit.slayer_blocked_konar_diary"
            )
            column(COL_CAN_ASSIGN_BOSSES, true)
            column(COL_BLOCK_COST, 80)
            columnRSCM(COL_DIARY_VARBIT, "varbit.kourend_diary_elite_complete")
            column(COL_DIARY_POINTS_PER_TASK, 20)
            columnRSCM(COL_EXCLUDED_BOSSES, "dbrow.slayer_boss_task_maggot_king")
        }

        row("dbrow.nieve_steve") {
            column(COL_MASTER_ID, 6)
            columnRSCM(COL_NPC_IDS, "npc.slayer_master_nieve", "npc.slayer_master_steve")
            column(COL_SLAYER_LEVEL, 1)
            column(COL_COMBAT_LEVEL, 85)
            column(COL_POINTS_PER_TASK, 12)
            columnRSCM(COL_BLOCK_VARBITS,
                "varbit.slayer_blocked_nieve_1",
                "varbit.slayer_blocked_nieve_2",
                "varbit.slayer_blocked_nieve_3",
                "varbit.slayer_blocked_nieve_4",
                "varbit.slayer_blocked_nieve_5",
                "varbit.slayer_blocked_nieve_6",
                "varbit.slayer_blocked_nieve_diary"
            )
            column(COL_CAN_ASSIGN_BOSSES, true)
            column(COL_BLOCK_COST, 90)
            columnRSCM(COL_DIARY_VARBIT, "varbit.western_diary_elite_complete")
            column(COL_DIARY_POINTS_PER_TASK, 15)
            columnRSCM(
                COL_EXCLUDED_BOSSES,
                "dbrow.slayer_boss_task_hydraboss",
                "dbrow.slayer_boss_task_maggot_king",
            )
        }

        row("dbrow.duradel_kuradal") {
            column(COL_MASTER_ID, 5)
            columnRSCM(COL_NPC_IDS, "npc.slayer_master_5_kuradal", "npc.slayer_master_5_duradel")
            column(COL_SLAYER_LEVEL, 50)
            column(COL_COMBAT_LEVEL, 100)
            column(COL_POINTS_PER_TASK, 15)
            columnRSCM(COL_BLOCK_VARBITS,
                "varbit.slayer_blocked_duradel_1",
                "varbit.slayer_blocked_duradel_2",
                "varbit.slayer_blocked_duradel_3",
                "varbit.slayer_blocked_duradel_4",
                "varbit.slayer_blocked_duradel_5",
                "varbit.slayer_blocked_duradel_6",
                "varbit.slayer_blocked_duradel_diary"
            )
            column(COL_CAN_ASSIGN_BOSSES, true)
            column(COL_BLOCK_COST, 100)
            columnRSCM(
                COL_EXCLUDED_BOSSES,
                "dbrow.slayer_boss_task_hydraboss",
                "dbrow.slayer_boss_task_maggot_king",
            )
        }

        row("dbrow.mortimer") {
            column(COL_MASTER_ID, 10)
            columnRSCM(COL_NPC_IDS, "npc.slayer_master_mortimer_vis")
            column(COL_SLAYER_LEVEL, 70)
            column(COL_COMBAT_LEVEL, 100)
            column(COL_POINTS_PER_TASK, 0)
            columnRSCM(
                COL_BLOCK_VARBITS,
                "varbit.slayer_blocked_mortimer_1",
                "varbit.slayer_blocked_mortimer_2",
            )
            column(COL_CAN_ASSIGN_BOSSES, false)
            column(COL_BLOCK_COST, 120)
        }
    }

    fun streakBonuses() = dbTable("dbtable.slayer_streak_bonuses", serverOnly = true) {
        column("interval", COL_BONUS_INTERVAL, VarType.INT)
        column("multiplier", COL_BONUS_MULTIPLIER, VarType.INT)

        row("dbrow.slayer_streak_bonus_10") {
            column(COL_BONUS_INTERVAL, 10)
            column(COL_BONUS_MULTIPLIER, 5)
        }

        row("dbrow.slayer_streak_bonus_50") {
            column(COL_BONUS_INTERVAL, 50)
            column(COL_BONUS_MULTIPLIER, 15)
        }

        row("dbrow.slayer_streak_bonus_100") {
            column(COL_BONUS_INTERVAL, 100)
            column(COL_BONUS_MULTIPLIER, 25)
        }

        row("dbrow.slayer_streak_bonus_250") {
            column(COL_BONUS_INTERVAL, 250)
            column(COL_BONUS_MULTIPLIER, 35)
        }

        row("dbrow.slayer_streak_bonus_1000") {
            column(COL_BONUS_INTERVAL, 1000)
            column(COL_BONUS_MULTIPLIER, 50)
        }
    }
}
