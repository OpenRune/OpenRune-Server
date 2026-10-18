package org.rsmod.content.other.treasuretrails.pack

import dev.openrune.definition.dbtables.dbTable
import dev.openrune.definition.util.VarType

object TrailTierTable {
    const val DIFFICULTY = 0
    const val CLUE_OBJ = 1
    const val CASKET_OBJ = 2
    const val BOX_OBJ = 3
    const val STEPS_MIN = 4
    const val STEPS_MAX = 5
    const val REWARD_ROLLS_MIN = 6
    const val REWARD_ROLLS_MAX = 7
    const val MASTER_CLUE_CHANCE = 8
    const val COMPLETED_VARP = 9
    const val MINOR_CASE_OBJ = 10
    const val MINOR_CASE_AT = 11
    const val MINOR_CASE_VARBIT = 12
    const val MAJOR_CASE_OBJ = 13
    const val MAJOR_CASE_AT = 14
    const val MAJOR_CASE_VARBIT = 15
    const val MILESTONE_OBJ = 16
    const val MILESTONE_AT = 17
    const val WATSON_VARBIT = 18
    const val LENGTH_VARBIT = 19
    const val PROGRESS_VARBIT = 20
    const val STEP_VARP = 21
    const val NAME = 22
    const val URI_NPC = 23

    fun tiers() =
        dbTable("dbtable.trail_tier", serverOnly = true) {
            column("difficulty", DIFFICULTY, VarType.INT)
            column("clue_obj", CLUE_OBJ, VarType.OBJ)
            column("casket_obj", CASKET_OBJ, VarType.OBJ)
            column("box_obj", BOX_OBJ, VarType.OBJ)
            column("steps_min", STEPS_MIN, VarType.INT)
            column("steps_max", STEPS_MAX, VarType.INT)
            column("reward_rolls_min", REWARD_ROLLS_MIN, VarType.INT)
            column("reward_rolls_max", REWARD_ROLLS_MAX, VarType.INT)
            column("master_clue_chance", MASTER_CLUE_CHANCE, VarType.INT)
            column("completed_varp", COMPLETED_VARP, VarType.VARP)
            column("minor_case_obj", MINOR_CASE_OBJ, VarType.OBJ)
            column("minor_case_at", MINOR_CASE_AT, VarType.INT)
            column("minor_case_varbit", MINOR_CASE_VARBIT, VarType.INT)
            column("major_case_obj", MAJOR_CASE_OBJ, VarType.OBJ)
            column("major_case_at", MAJOR_CASE_AT, VarType.INT)
            column("major_case_varbit", MAJOR_CASE_VARBIT, VarType.INT)
            column("milestone_obj", MILESTONE_OBJ, VarType.OBJ)
            column("milestone_at", MILESTONE_AT, VarType.INT)
            column("watson_varbit", WATSON_VARBIT, VarType.INT)
            column("length_varbit", LENGTH_VARBIT, VarType.INT)
            column("progress_varbit", PROGRESS_VARBIT, VarType.INT)
            column("step_varp", STEP_VARP, VarType.VARP)
            column("name", NAME, VarType.STRING)
            column("uri_npc", URI_NPC, VarType.NPC)

            row("dbrow.trail_tier_beginner") {
                column(DIFFICULTY, 0)
                column(NAME, "beginner")
                columnRSCM(URI_NPC, "npc.trail_beginner_uri")
                columnRSCM(CLUE_OBJ, "obj.trail_clue_beginner")
                columnRSCM(CASKET_OBJ, "obj.trail_reward_casket_beginner")
                columnRSCM(BOX_OBJ, "obj.league_clue_box_beginner")
                column(STEPS_MIN, 1)
                column(STEPS_MAX, 3)
                column(REWARD_ROLLS_MIN, 1)
                column(REWARD_ROLLS_MAX, 3)
                columnRSCM(COMPLETED_VARP, "varp.trail_completed_beginner")
                columnRSCM(MINOR_CASE_OBJ, "obj.scroll_case_beginner_minor")
                column(MINOR_CASE_AT, 50)
                columnRSCM(MINOR_CASE_VARBIT, "varbit.scroll_case_beginner_minor")
                columnRSCM(MAJOR_CASE_OBJ, "obj.scroll_case_beginner_major")
                column(MAJOR_CASE_AT, 100)
                columnRSCM(MAJOR_CASE_VARBIT, "varbit.scroll_case_beginner_major")
                columnRSCM(LENGTH_VARBIT, "varbit.trail_beginner_length")
                columnRSCM(PROGRESS_VARBIT, "varbit.trail_beginner_progress")
                columnRSCM(STEP_VARP, "varp.trail_beginner_step")
            }

            row("dbrow.trail_tier_easy") {
                column(DIFFICULTY, 1)
                column(NAME, "easy")
                columnRSCM(URI_NPC, "npc.trail_easy_uri")
                columnRSCM(CASKET_OBJ, "obj.trail_reward_casket_easy")
                columnRSCM(BOX_OBJ, "obj.league_clue_box_easy")
                column(STEPS_MIN, 2)
                column(STEPS_MAX, 4)
                column(REWARD_ROLLS_MIN, 2)
                column(REWARD_ROLLS_MAX, 4)
                column(MASTER_CLUE_CHANCE, 50)
                columnRSCM(COMPLETED_VARP, "varp.trail_completed_easy")
                columnRSCM(MINOR_CASE_OBJ, "obj.scroll_case_easy_minor")
                column(MINOR_CASE_AT, 100)
                columnRSCM(MINOR_CASE_VARBIT, "varbit.scroll_case_easy_minor")
                columnRSCM(MAJOR_CASE_OBJ, "obj.scroll_case_easy_major")
                column(MAJOR_CASE_AT, 200)
                columnRSCM(MAJOR_CASE_VARBIT, "varbit.scroll_case_easy_major")
                columnRSCM(MILESTONE_OBJ, "obj.large_spade")
                column(MILESTONE_AT, 500)
                columnRSCM(WATSON_VARBIT, "varbit.trail_watson_easy")
                columnRSCM(LENGTH_VARBIT, "varbit.trail_easy_length")
                columnRSCM(PROGRESS_VARBIT, "varbit.trail_easy_progress")
            }

            row("dbrow.trail_tier_medium") {
                column(DIFFICULTY, 2)
                column(NAME, "medium")
                columnRSCM(URI_NPC, "npc.trail_medium_uri")
                columnRSCM(CASKET_OBJ, "obj.trail_reward_casket_medium")
                columnRSCM(BOX_OBJ, "obj.league_clue_box_medium")
                column(STEPS_MIN, 3)
                column(STEPS_MAX, 5)
                column(REWARD_ROLLS_MIN, 3)
                column(REWARD_ROLLS_MAX, 5)
                column(MASTER_CLUE_CHANCE, 30)
                columnRSCM(COMPLETED_VARP, "varp.trail_completed_medium")
                columnRSCM(MINOR_CASE_OBJ, "obj.scroll_case_medium_minor")
                column(MINOR_CASE_AT, 100)
                columnRSCM(MINOR_CASE_VARBIT, "varbit.scroll_case_medium_minor")
                columnRSCM(MAJOR_CASE_OBJ, "obj.scroll_case_medium_major")
                column(MAJOR_CASE_AT, 250)
                columnRSCM(MAJOR_CASE_VARBIT, "varbit.scroll_case_medium_major")
                columnRSCM(MILESTONE_OBJ, "obj.clueless_scroll")
                column(MILESTONE_AT, 400)
                columnRSCM(WATSON_VARBIT, "varbit.trail_watson_medium")
                columnRSCM(LENGTH_VARBIT, "varbit.trail_medium_length")
                columnRSCM(PROGRESS_VARBIT, "varbit.trail_medium_progress")
            }

            row("dbrow.trail_tier_hard") {
                column(DIFFICULTY, 3)
                column(NAME, "hard")
                columnRSCM(URI_NPC, "npc.trail_hard_uri")
                columnRSCM(CASKET_OBJ, "obj.trail_reward_casket_hard")
                columnRSCM(BOX_OBJ, "obj.league_clue_box_hard")
                column(STEPS_MIN, 4)
                column(STEPS_MAX, 6)
                column(REWARD_ROLLS_MIN, 4)
                column(REWARD_ROLLS_MAX, 6)
                column(MASTER_CLUE_CHANCE, 15)
                columnRSCM(COMPLETED_VARP, "varp.trail_completed_hard")
                columnRSCM(MINOR_CASE_OBJ, "obj.scroll_case_hard_minor")
                column(MINOR_CASE_AT, 50)
                columnRSCM(MINOR_CASE_VARBIT, "varbit.scroll_case_hard_minor")
                columnRSCM(MAJOR_CASE_OBJ, "obj.scroll_case_hard_major")
                column(MAJOR_CASE_AT, 150)
                columnRSCM(MAJOR_CASE_VARBIT, "varbit.scroll_case_hard_major")
                columnRSCM(WATSON_VARBIT, "varbit.trail_watson_hard")
                columnRSCM(LENGTH_VARBIT, "varbit.trail_hard_length")
                columnRSCM(PROGRESS_VARBIT, "varbit.trail_hard_progress")
            }

            row("dbrow.trail_tier_elite") {
                column(DIFFICULTY, 4)
                column(NAME, "elite")
                columnRSCM(URI_NPC, "npc.trail_elite_uri")
                columnRSCM(CASKET_OBJ, "obj.trail_reward_casket_elite")
                columnRSCM(BOX_OBJ, "obj.league_clue_box_elite")
                column(STEPS_MIN, 5)
                column(STEPS_MAX, 7)
                column(REWARD_ROLLS_MIN, 4)
                column(REWARD_ROLLS_MAX, 6)
                column(MASTER_CLUE_CHANCE, 5)
                columnRSCM(COMPLETED_VARP, "varp.trail_completed_elite")
                columnRSCM(MINOR_CASE_OBJ, "obj.scroll_case_elite_minor")
                column(MINOR_CASE_AT, 50)
                columnRSCM(MINOR_CASE_VARBIT, "varbit.scroll_case_elite_minor")
                columnRSCM(MAJOR_CASE_OBJ, "obj.scroll_case_elite_major")
                column(MAJOR_CASE_AT, 150)
                columnRSCM(MAJOR_CASE_VARBIT, "varbit.scroll_case_elite_major")
                columnRSCM(MILESTONE_OBJ, "obj.heavy_casket")
                column(MILESTONE_AT, 200)
                columnRSCM(WATSON_VARBIT, "varbit.trail_watson_elite")
                columnRSCM(LENGTH_VARBIT, "varbit.trail_elite_length")
                columnRSCM(PROGRESS_VARBIT, "varbit.trail_elite_progress")
            }

            row("dbrow.trail_tier_master") {
                column(DIFFICULTY, 5)
                column(NAME, "master")
                columnRSCM(URI_NPC, "npc.trail_master_uri")
                columnRSCM(CLUE_OBJ, "obj.trail_clue_master")
                columnRSCM(CASKET_OBJ, "obj.trail_reward_casket_master")
                columnRSCM(BOX_OBJ, "obj.league_clue_box_master")
                column(STEPS_MIN, 6)
                column(STEPS_MAX, 8)
                column(REWARD_ROLLS_MIN, 5)
                column(REWARD_ROLLS_MAX, 7)
                columnRSCM(COMPLETED_VARP, "varp.trail_completed_master")
                columnRSCM(MINOR_CASE_OBJ, "obj.scroll_case_master_minor")
                column(MINOR_CASE_AT, 25)
                columnRSCM(MINOR_CASE_VARBIT, "varbit.scroll_case_master_minor")
                columnRSCM(MAJOR_CASE_OBJ, "obj.scroll_case_master_major")
                column(MAJOR_CASE_AT, 75)
                columnRSCM(MAJOR_CASE_VARBIT, "varbit.scroll_case_master_major")
                columnRSCM(MILESTONE_OBJ, "obj.scroll_sack")
                column(MILESTONE_AT, 100)
                columnRSCM(LENGTH_VARBIT, "varbit.trail_master_length")
                columnRSCM(PROGRESS_VARBIT, "varbit.trail_master_progress")
            }
        }
}
