package org.rsmod.content.events.leaguetasks.pack

import org.rsmod.content.events.leaguetasks.pack.LeagueTaskTriggerTable.COUNT
import org.rsmod.content.events.leaguetasks.pack.LeagueTaskTriggerTable.COUNTER
import org.rsmod.content.events.leaguetasks.pack.LeagueTaskTriggerTable.NPC

/**
 * Kill tasks whose `action` row has no `npc_kill`, `npc_kill_category` or
 * `npc_kill_slayer_category` of its own. Tasks with one are read from Jagex's row directly.
 */
internal fun TriggerRows.killTasks() {
    kill("league_task_defeat_the_king_black_dragon", "npc.king_dragon")
    kill(
        "league_task_kill_10_frost_crabs",
        "npc.frostcrab",
        "npc.frostcrab_small",
        times = 10,
        counter = "varbit.league_task_kill_frost_crabs",
    )
}

internal fun TriggerRows.kill(
    task: String,
    vararg npcs: String,
    times: Int = 1,
    counter: String? = null,
) {
    trigger("dbrow.$task", LeagueTaskTrigger.Kill) {
        columnRSCM(NPC, npcs.asList())
        if (times != 1) {
            column(COUNT, times)
        }
        if (counter != null) {
            columnRSCM(COUNTER, counter)
        }
    }
}
