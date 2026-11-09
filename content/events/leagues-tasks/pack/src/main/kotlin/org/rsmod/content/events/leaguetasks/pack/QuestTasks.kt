package org.rsmod.content.events.leaguetasks.pack

import org.rsmod.content.events.leaguetasks.pack.LeagueTaskTriggerTable.VALUE
import org.rsmod.content.events.leaguetasks.pack.LeagueTaskTriggerTable.VARBIT
import org.rsmod.content.events.leaguetasks.pack.LeagueTaskTriggerTable.VARP

/**
 * Quest tasks, met once the quest's progress var reaches its `endstate`. The var is the one
 * `[proc,quest_progress_get]` reads for the task's `quest` dbrow.
 */
internal fun TriggerRows.questTasks() {
    variable("action_complete_the_vale_totems_miniquest", "varbit.ent_totems_intro", 100)
    variable("action_complete_at_first_light_quest", "varbit.afl", 12)
    variable("league_task_complete_death_on_the_isle", "varbit.doti", 50)
    variable("league_task_complete_meat_and_greet", "varbit.mag", 26)
    variable("action_complete_shadows_of_custodia", "varbit.soc", 24)
    variable("league_task_complete_a_ribbiting_tale", "varbit.frog_quest", 32)
    variable("action_complete_the_heart_of_darkness", "varbit.vmq3", 76)
    variable("league_task_complete_a_porcine_of_interest", "varbit.porcine", 40)
    variable("league_task_complete_fishing_contest", "varp.fishingcompo", 5)
    variable("league_task_complete_haunted_mine", "varp.hauntedmine", 11)
    variable("league_task_his_faithful_servants", "varbit.hfs", 10)
    variable("league_task_complete_monks_friend", "varp.drunkmonkquest", 80)
    variable("league_task_complete_royal_trouble", "varbit.royal_quest", 30)
    variable("league_task_complete_sea_slug", "varp.seaslugquest", 12)
    variable("league_task_complete_shadow_of_the_storm", "varbit.agrith_quest", 125)
    variable("league_task_complete_sleeping_giants", "varbit.sleeping_giants", 30)
    variable("league_task_complete_spirits_of_the_elid", "varbit.elidquest", 60)
    variable("league_task_complete_the_golem", "varbit.golem_a", 10)
    variable("league_task_complete_throne_of_miscellania", "varp.misc_quest", 100)
    variable("league_task_complete_tower_of_life", "varbit.tol_prog", 18)
    variable("league_task_complete_witchs_potion", "varp.hetty", 3)
    variable("league_task_complete_the_garden_of_death", "varbit.tgod", 56)
    variable("league_task_complete_the_observatory_quest", "varp.itgronigen", 7)
    variable("action_complete_the_final_dawn", "varbit.vmq4", 68)
    variable("league_task_complete_beneath_cursed_sands", "varbit.bcs", 108)
    variable("league_task_complete_elemental_workshop_ii", "varbit.elemental_quest_2_main", 11)
    variable("league_task_complete_path_of_glouphrie", "varbit.pog", 50)
    variable("league_task_complete_sins_of_the_father", "varbit.myq5", 138)
}

internal fun TriggerRows.variable(task: String, symbol: String, atLeast: Int) {
    trigger("dbrow.$task", LeagueTaskTrigger.Var) {
        columnRSCM(if (symbol.startsWith("varp.")) VARP else VARBIT, symbol)
        column(VALUE, atLeast)
    }
}
