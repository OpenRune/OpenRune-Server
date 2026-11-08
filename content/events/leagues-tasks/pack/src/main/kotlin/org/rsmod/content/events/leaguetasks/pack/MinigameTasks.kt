package org.rsmod.content.events.leaguetasks.pack

internal fun TriggerRows.minigameTasks() {
    variable("league_task_complete_the_gauntlet", "varp.total_completed_gauntlet", 1)
    variable("league_task_complete_the_corrupted_gauntlet", "varp.total_completed_gauntlet_hm", 1)
    variable(
        "league_task_complete_the_corrupted_gauntlet_50_times",
        "varp.total_completed_gauntlet_hm",
        50,
    )
    variable(
        "league_task_complete_the_corrupted_gauntlet_100_times",
        "varp.total_completed_gauntlet_hm",
        100,
    )

    for (handins in listOf(10, 25, 50)) {
        variable(
            "league_task_giants_foundry_${handins}_handins",
            "varp.giants_foundry_swords_made",
            handins,
        )
    }
    for (quality in listOf(50, 125, 150)) {
        variable(
            "league_task_giants_foundry_${quality}_quality_sword",
            "varp.giants_foundry_best_quality",
            quality,
        )
    }
    objTask("league_task_drink_kovacs_grog", LeagueTaskTrigger.Consume, "obj.kovacs_grog")
}
