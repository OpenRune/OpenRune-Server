package org.rsmod.content.raids.toa.pack

import dev.openrune.definition.dbtables.DBTable
import dev.openrune.definition.dbtables.dbTable
import dev.openrune.definition.util.VarType

object ToaInvocationsTable {

    const val NAME = 0
    const val INDEX = 1
    const val CATEGORY = 2
    const val LEVEL_MODIFIER = 3
    const val PREREQUISITE = 4

    fun invocations(): DBTable =
        dbTable("dbtable.toa_invocations", serverOnly = true) {
            column("name", NAME, VarType.STRING)
            column("index", INDEX, VarType.INT)
            column("category", CATEGORY, VarType.INT)
            column("level_modifier", LEVEL_MODIFIER, VarType.INT)
            column("prerequisite", PREREQUISITE, VarType.DBROW)

            fun invocation(
                key: String,
                name: String,
                index: Int,
                category: Int,
                levelModifier: Int,
                prerequisite: String? = null,
            ) {
                row("dbrow.toa_invocation_$key") {
                    column(NAME, name)
                    column(INDEX, index)
                    column(CATEGORY, category)
                    column(LEVEL_MODIFIER, levelModifier)
                    if (prerequisite != null) {
                        columnRSCM(PREREQUISITE, "dbrow.toa_invocation_$prerequisite")
                    }
                }
            }

            invocation("try_again", "Try Again", 1, 3, 5)
            invocation("persistence", "Persistence", 2, 3, 10)
            invocation("softcore_run", "Softcore Run", 3, 3, 15)
            invocation("hardcore_run", "Hardcore Run", 4, 3, 25)
            invocation("walk_for_it", "Walk for It", 5, 4, 10)
            invocation("jog_for_it", "Jog for It", 6, 4, 15)
            invocation("run_for_it", "Run for It", 7, 4, 20)
            invocation("sprint_for_it", "Sprint for It", 8, 4, 25)
            invocation("need_some_help", "Need Some Help?", 9, 5, 15)
            invocation("need_less_help", "Need Less Help?", 10, 5, 25)
            invocation("no_help_needed", "No Help Needed", 11, 5, 40)
            invocation("walk_the_path", "Walk the Path", 12, 9, 50)
            invocation("pathseeker", "Pathseeker", 13, 6, 15)
            invocation("pathfinder", "Pathfinder", 14, 6, 40)
            invocation("pathmaster", "Pathmaster", 15, 6, 50)
            invocation("quiet_prayers", "Quiet Prayers", 16, 7, 20)
            invocation("deadly_prayers", "Deadly Prayers", 17, 7, 20)
            invocation("on_a_diet", "On a Diet", 18, 8, 15)
            invocation("dehydration", "Dehydration", 19, 8, 30)
            invocation("overly_draining", "Overly Draining", 20, 8, 15)
            invocation("lively_larvae", "Lively Larvae", 21, 11, 5)
            invocation("more_overlords", "More Overlords", 22, 11, 15)
            invocation("blowing_mud", "Blowing Mud", 23, 11, 10)
            invocation("medic", "Medic!", 24, 11, 15)
            invocation("aerial_assault", "Aerial Assault", 25, 11, 10)
            invocation("not_just_a_head", "Not Just a Head", 26, 12, 15)
            invocation("arterial_spray", "Arterial Spray", 27, 12, 10, "not_just_a_head")
            invocation("blood_thinners", "Blood Thinners", 28, 12, 5, "not_just_a_head")
            invocation("upset_stomach", "Upset Stomach", 29, 12, 15)
            invocation("double_trouble", "Double Trouble", 30, 10, 20)
            invocation("keep_back", "Keep Back", 31, 10, 10)
            invocation("stay_vigilant", "Stay Vigilant", 32, 10, 15)
            invocation("feeling_special", "Feeling Special?", 33, 10, 20)
            invocation("mind_the_gap", "Mind the Gap!", 34, 13, 10)
            invocation("gotta_have_faith", "Gotta Have Faith", 35, 13, 10)
            invocation("jungle_japes", "Jungle Japes", 36, 13, 5)
            invocation("shaking_things_up", "Shaking Things Up", 37, 13, 10)
            invocation("boulderdash", "Boulderdash", 38, 13, 10)
            invocation("ancient_haste", "Ancient Haste", 39, 14, 10)
            invocation("acceleration", "Acceleration", 40, 14, 10)
            invocation("penetration", "Penetration", 41, 14, 10)
            invocation("overclocked", "Overclocked", 42, 14, 10)
            invocation("overclocked_2", "Overclocked 2", 43, 14, 10, "overclocked")
            invocation("insanity", "Insanity", 44, 14, 50, "overclocked_2")
            invocation("blazing_tombs_1", "Blazing Tombs I", 45, 15, 200)
            invocation("blazing_tombs_2", "Blazing Tombs II", 46, 15, 200)
        }
}
