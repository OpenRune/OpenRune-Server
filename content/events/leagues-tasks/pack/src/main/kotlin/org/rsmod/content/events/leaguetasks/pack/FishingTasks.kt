package org.rsmod.content.events.leaguetasks.pack

private const val FISHING = "stat.fishing"

internal fun TriggerRows.fishingTasks() {
    product("league_task_catch_a_herring", FISHING, "obj.raw_herring")
    product("league_task_catch_a_shrimp", FISHING, "obj.raw_shrimp")
    product("league_task_catch_an_anchovy", FISHING, "obj.raw_anchovies")
    product(
        "league_task_catch_a_karambwanji",
        FISHING,
        "obj.tbwt_raw_karambwanji",
        areas = listOf("area.league_region_karamja"),
    )
    product(
        "league_task_catch_a_salmon",
        FISHING,
        "obj.raw_salmon",
        areas = listOf("area.league_region_desert"),
    )
    product(
        "league_task_catch_a_salmon_on_karamja",
        FISHING,
        "obj.raw_salmon",
        areas = listOf("area.league_region_karamja"),
    )
    product(
        "league_task_catch_a_monkfish",
        FISHING,
        "obj.raw_monkfish",
        areas = listOf("area.league_piscatoris"),
    )

    caught("league_task_catch_10_cod", "obj.raw_cod", 10, "catch_cod")
    caught("league_task_catch_100_swordfish", "obj.raw_swordfish", 100, "catch_swordfish")
    caught("league_task_catch_50_tuna", "obj.raw_tuna", 50, "catch_tuna")
    caught("league_task_catch_100_tuna", "obj.raw_tuna", 100, "catch_tuna")
    caught("league_task_catch_20_mackerel", "obj.raw_mackerel", 20, "catch_mackerel")
    caught("league_task_catch_75_lobsters", "obj.raw_lobster", 75, "catch_lobsters")
    caught("league_task_catch_75_trout", "obj.raw_trout", 75, "catch_trout")
    caught("league_task_catch_100_shark", "obj.raw_shark", 100, "catch_shark")
    product(
        "league_task_catch_50_karambwan",
        FISHING,
        "obj.tbwt_raw_karambwan",
        times = 50,
        counter = "varbit.league_counter_catch_karambwan_on_karamja",
        areas = listOf("area.league_region_karamja"),
    )
    product(
        "league_task_catch_100_dark_crabs",
        FISHING,
        "obj.raw_dark_crab",
        times = 100,
        counter = "varbit.league_counter_catch_dark_crabs",
        areas = listOf("area.wilderness"),
    )
    product(
        "league_task_catch_150_anglerfish",
        FISHING,
        "obj.raw_anglerfish",
        times = 150,
        counter = "varbit.league_task_catch_anglerfish",
    )

    equip(
        "league_task_equip_a_full_anglers_outfit",
        "obj.trawler_reward_hat",
        "obj.trawler_reward_top",
        "obj.trawler_reward_legs",
        "obj.trawler_reward_boots",
        "obj.spirit_angler_hat",
        "obj.krill_angler_hat",
        "obj.spirit_angler_top",
        "obj.spirit_angler_legs",
        "obj.spirit_angler_boots",
        worn = 4,
    )
}

private fun TriggerRows.caught(task: String, fish: String, times: Int, counter: String) {
    product(task, FISHING, fish, times = times, counter = "varbit.league_counter_$counter")
}
