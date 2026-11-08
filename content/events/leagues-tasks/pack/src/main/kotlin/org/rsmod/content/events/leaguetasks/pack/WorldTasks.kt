package org.rsmod.content.events.leaguetasks.pack

import org.rsmod.content.events.leaguetasks.pack.LeagueTaskTriggerTable.INTERFACE

internal fun TriggerRows.worldTasks() {
    opened("league_task_open_the_leagues_menu", "interface.league_side_panel")
}

internal fun TriggerRows.opened(task: String, interf: String) {
    trigger("dbrow.$task", LeagueTaskTrigger.Interface) { columnRSCM(INTERFACE, interf) }
}
