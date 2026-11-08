package org.rsmod.content.events.leaguetasks.pack

internal fun TriggerRows.firemakingTasks() {
    burn("league_task_burn_some_normal_logs", "obj.logs")
    burn("league_task_burn_some_oak_logs", "obj.oak_logs")
    burn("league_task_burn_some_magic_logs", "obj.magic_logs")
    burn(
        "league_task_burn_25_maple_logs",
        "obj.maple_logs",
        times = 25,
        counter = "varbit.league_task_burn_maple_logs",
    )
    burn(
        "league_task_burn_100_willow_logs",
        "obj.willow_logs",
        times = 100,
        counter = "varbit.league_counter_burn_willow_logs",
    )
    burn(
        "league_task_burn_100_yew_logs",
        "obj.yew_logs",
        times = 100,
        counter = "varbit.league_counter_burn_yew_logs",
    )
    burn(
        "league_task_burn_20_blisterwood_logs",
        "obj.blisterwood_logs",
        times = 20,
        counter = "varbit.league_counter_burn_blisterwood_logs",
    )
    burn(
        "league_task_burn_200_redwood_logs",
        "obj.redwood_logs",
        times = 200,
        counter = "varbit.league_counter_burn_redwood_logs",
    )
}

private fun TriggerRows.burn(task: String, log: String, times: Int = 1, counter: String? = null) {
    objTask(task, LeagueTaskTrigger.Firemaking, log, times = times, counter = counter)
}
