package org.rsmod.content.events.leaguetasks.pack

import org.rsmod.content.events.leaguetasks.pack.LeagueTaskTriggerTable.COUNT
import org.rsmod.content.events.leaguetasks.pack.LeagueTaskTriggerTable.COUNTER
import org.rsmod.content.events.leaguetasks.pack.LeagueTaskTriggerTable.NPC_PARAM

private const val SUPERIOR_KILLS = "varbit.league_counter_kill_superiors"
private const val SLAYER_TASKS = "varbit.league_counter_complete_slayer_tasks"

internal fun TriggerRows.slayerTasks() {
    trigger("dbrow.league_task_complete_1_slayer_task", LeagueTaskTrigger.SlayerTask)
    trigger("dbrow.league_task_complete_200_slayer_tasks", LeagueTaskTrigger.SlayerTask) {
        column(COUNT, 200)
        columnRSCM(COUNTER, SLAYER_TASKS)
    }

    superiorKills("league_task_defeat_a_superior_slayer_creature", 1)
    superiorKills("league_task_defeat_10_superior_slayer_creatures", 10)
    superiorKills("league_task_defeat_20_superior_slayer_creatures", 20)
    superiorKills("league_task_25_superior_slayer_encounters", 25)
    superiorKills("league_task_defeat_50_superior_slayer_creatures", 50)
    superiorKills("league_task_75_superior_slayer_encounters", 75)
    superiorKills("league_task_100_superior_slayer_encounters", 100)

    product("league_task_assemble_a_slayer_helm", "stat.crafting", "obj.slayer_helm")
}

private fun TriggerRows.superiorKills(task: String, kills: Int) {
    trigger("dbrow.$task", LeagueTaskTrigger.Kill) {
        columnRSCM(NPC_PARAM, "param.slayer_superior_form")
        column(COUNT, kills)
        columnRSCM(COUNTER, SUPERIOR_KILLS)
    }
}
