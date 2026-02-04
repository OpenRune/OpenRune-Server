package org.alter.impl.skills

import dev.openrune.definition.dbtables.dbTable
import dev.openrune.definition.util.VarType

object Slayer {

    const val COL_MASTER_ID = 0
    const val COL_NPC_IDS = 1
    const val COL_SLAYER_LEVEL = 2
    const val COL_COMBAT_LEVEL = 3
    const val COL_POINTS_PER_TASK = 4
    const val COL_BLOCK_VARBITS = 5
    
    fun masters() = dbTable("tables.slayer_masters",serverOnly = true) {
        column("master_id", COL_MASTER_ID, VarType.INT)
        column("npc_ids", COL_NPC_IDS, VarType.NPC)
        column("slayer_level", COL_SLAYER_LEVEL, VarType.INT)
        column("combat_level", COL_COMBAT_LEVEL, VarType.INT)
        column("points_per_task", COL_POINTS_PER_TASK, VarType.INT)
        column("block_varbits", COL_BLOCK_VARBITS, VarType.INT)
        
        row("dbrows.turael_aya") {
            column(COL_MASTER_ID, 1)
            columnRSCM(COL_NPC_IDS,"npcs.slayer_master_1_tureal","npcs.slayer_master_1_aya")
            column(COL_SLAYER_LEVEL, 1)
            column(COL_COMBAT_LEVEL, 1)
            column(COL_POINTS_PER_TASK, 0)
            columnRSCM(COL_BLOCK_VARBITS,
                "varbits.slayer_blocked_turael_1",
                "varbits.slayer_blocked_turael_2",
                "varbits.slayer_blocked_turael_3",
                "varbits.slayer_blocked_turael_4",
                "varbits.slayer_blocked_turael_5",
                "varbits.slayer_blocked_turael_6"
            )
        }

        row("dbrows.spira") {
            column(COL_MASTER_ID, 9)
            columnRSCM(COL_NPC_IDS,"npcs.slayer_master_9_active","npcs.porcine_spria")
            column(COL_SLAYER_LEVEL, 1)
            column(COL_COMBAT_LEVEL, 1)
            column(COL_POINTS_PER_TASK, 0)
            columnRSCM(COL_BLOCK_VARBITS,
                "varbits.slayer_blocked_turael_1",
                "varbits.slayer_blocked_turael_2",
                "varbits.slayer_blocked_turael_3",
                "varbits.slayer_blocked_turael_4",
                "varbits.slayer_blocked_turael_5",
                "varbits.slayer_blocked_turael_6"
            )
        }

        row("dbrows.krystilia") {
            column(COL_MASTER_ID, 9)
            columnRSCM(COL_NPC_IDS,"npcs.slayer_master_7")
            column(COL_SLAYER_LEVEL, 1)
            column(COL_COMBAT_LEVEL, 1)
            column(COL_POINTS_PER_TASK, 25)
            columnRSCM(COL_BLOCK_VARBITS,
                "varbits.slayer_blocked_krystilia_1",
                "varbits.slayer_blocked_krystilia_2",
                "varbits.slayer_blocked_krystilia_3",
                "varbits.slayer_blocked_krystilia_4",
                "varbits.slayer_blocked_krystilia_5",
                "varbits.slayer_blocked_krystilia_6"
            )
        }

        row("dbrows.mazchna_achtryn") {
            column(COL_MASTER_ID, 2)
            columnRSCM(COL_NPC_IDS,"npcs.slayer_master_2_mazchna","npcs.slayer_master_2_achtryn_vis")
            column(COL_SLAYER_LEVEL, 1)
            column(COL_COMBAT_LEVEL, 20)
            column(COL_POINTS_PER_TASK, 6)
            columnRSCM(COL_BLOCK_VARBITS,
                "varbits.slayer_blocked_mazchna_1",
                "varbits.slayer_blocked_mazchna_2",
                "varbits.slayer_blocked_mazchna_3",
                "varbits.slayer_blocked_mazchna_4",
                "varbits.slayer_blocked_mazchna_5",
                "varbits.slayer_blocked_mazchna_6"
            )
        }

        row("dbrows.vannaka") {
            column(COL_MASTER_ID, 3)
            columnRSCM(COL_NPC_IDS,"npcs.slayer_master_3")
            column(COL_SLAYER_LEVEL, 1)
            column(COL_COMBAT_LEVEL, 40)
            column(COL_POINTS_PER_TASK, 8)
            columnRSCM(COL_BLOCK_VARBITS,
                "varbits.slayer_blocked_vannaka_1",
                "varbits.slayer_blocked_vannaka_2",
                "varbits.slayer_blocked_vannaka_3",
                "varbits.slayer_blocked_vannaka_4",
                "varbits.slayer_blocked_vannaka_5",
                "varbits.slayer_blocked_vannaka_6"
            )
        }

        row("dbrows.chaeldar") {
            column(COL_MASTER_ID, 3)
            columnRSCM(COL_NPC_IDS,"npcs.slayer_master_4")
            column(COL_SLAYER_LEVEL, 1)
            column(COL_COMBAT_LEVEL, 70)
            column(COL_POINTS_PER_TASK, 10)
            columnRSCM(COL_BLOCK_VARBITS,
                "varbits.slayer_blocked_chaeldar_1",
                "varbits.slayer_blocked_chaeldar_2",
                "varbits.slayer_blocked_chaeldar_3",
                "varbits.slayer_blocked_chaeldar_4",
                "varbits.slayer_blocked_chaeldar_5",
                "varbits.slayer_blocked_chaeldar_6"
            )
        }

        row("dbrows.konar") {
            column(COL_MASTER_ID, 8)
            columnRSCM(COL_NPC_IDS,"npcs.slayer_master_8")
            column(COL_SLAYER_LEVEL, 1)
            column(COL_COMBAT_LEVEL, 75)
            column(COL_POINTS_PER_TASK, 18)
            columnRSCM(COL_BLOCK_VARBITS,
                "varbits.slayer_blocked_konar_1",
                "varbits.slayer_blocked_konar_2",
                "varbits.slayer_blocked_konar_3",
                "varbits.slayer_blocked_konar_4",
                "varbits.slayer_blocked_konar_5",
                "varbits.slayer_blocked_konar_6"
            )
        }

        row("dbrows.nieve_steve") {
            column(COL_MASTER_ID, 6)
            columnRSCM(COL_NPC_IDS,"npcs.slayer_master_nieve","npcs.slayer_master_steve")
            column(COL_SLAYER_LEVEL, 1)
            column(COL_COMBAT_LEVEL, 85)
            column(COL_POINTS_PER_TASK, 12)
            columnRSCM(COL_BLOCK_VARBITS,
                "varbits.slayer_blocked_nieve_1",
                "varbits.slayer_blocked_nieve_2",
                "varbits.slayer_blocked_nieve_3",
                "varbits.slayer_blocked_nieve_4",
                "varbits.slayer_blocked_nieve_5",
                "varbits.slayer_blocked_nieve_6"
            )
        }

        row("dbrows.duradel_kuradal") {
            column(COL_MASTER_ID, 5)
            columnRSCM(COL_NPC_IDS,"npcs.slayer_master_5_kuradal","npcs.slayer_master_5_duradel")
            column(COL_SLAYER_LEVEL, 50)
            column(COL_COMBAT_LEVEL, 100)
            column(COL_POINTS_PER_TASK, 15)
            columnRSCM(COL_BLOCK_VARBITS,
                "varbits.slayer_blocked_duradel_1",
                "varbits.slayer_blocked_duradel_2",
                "varbits.slayer_blocked_duradel_3",
                "varbits.slayer_blocked_duradel_4",
                "varbits.slayer_blocked_duradel_5",
                "varbits.slayer_blocked_duradel_6"
            )
        }

    }

}