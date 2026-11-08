package org.rsmod.content.events.leaguetasks.pack

private const val WOODCUTTING = "stat.woodcutting"

internal fun TriggerRows.woodcuttingTasks() {
    product("league_task_chop_some_logs", WOODCUTTING)
    product("league_task_chop_some_logs_with_a_steel_axe", WOODCUTTING, tools = listOf("obj.steel_axe"))
    product("league_task_chop_some_logs_with_a_rune_axe", WOODCUTTING, tools = listOf("obj.rune_axe"))
    product("league_task_chop_down_an_arctic_pine_tree", WOODCUTTING, "obj.arctic_pine_log")
    product(
        "league_task_chop_100_willow_logs",
        WOODCUTTING,
        "obj.willow_logs",
        times = 100,
        counter = "varbit.league_counter_chop_willow_logs",
    )
    product(
        "league_task_cut_50_maple_logs",
        WOODCUTTING,
        "obj.maple_logs",
        times = 50,
        counter = "varbit.league_task_cut_maple_logs",
    )
    product(
        "league_task_chop_100_blisterwood_logs",
        WOODCUTTING,
        "obj.blisterwood_logs",
        times = 100,
        counter = "varbit.league_counter_chop_blisterwood_logs",
    )
    product(
        "league_task_chop_25_juniper_logs",
        WOODCUTTING,
        "obj.juniper_logs",
        times = 25,
        counter = "varbit.league_task_chop_juniper_logs",
    )
    product(
        "league_task_chop_100_yew_logs",
        WOODCUTTING,
        "obj.yew_logs",
        times = 100,
        counter = "varbit.league_task_chop_yew_logs",
    )
    product(
        "league_task_chop_75_magic_logs",
        WOODCUTTING,
        "obj.magic_logs",
        times = 75,
        counter = "varbit.league_counter_chop_magic_logs",
    )

    product(
        "league_task_chop_a_tree_in_the_tlati_rainforest",
        WOODCUTTING,
        areas = listOf("area.league_tlati_rainforest"),
    )
    product(
        "league_task_chop_100_maples_in_tirannwn",
        WOODCUTTING,
        "obj.maple_logs",
        times = 100,
        counter = "varbit.league_counter_chop_maples_in_tirannwn",
        areas = listOf("area.league_region_tirannwn"),
    )
    product(
        "league_task_chop_15_yew_logs_in_shayzien",
        WOODCUTTING,
        "obj.yew_logs",
        times = 15,
        counter = "varbit.league_task_chop_yew_logs_in_shayzien",
        areas = listOf("area.league_shayzien"),
    )
    product(
        "league_task_chop_20_magic_logs_in_varlamore",
        WOODCUTTING,
        "obj.magic_logs",
        times = 20,
        counter = "varbit.league_task_chop_magic_logs_in_varlamore",
        areas = listOf("area.league_region_varlamore"),
    )
    product(
        "league_task_chop_100_teak_logs_in_prifddinas",
        WOODCUTTING,
        "obj.teak_logs",
        times = 100,
        counter = "varbit.league_counter_chop_teak_logs_in_prifddinas",
        areas = listOf("area.league_prifddinas"),
    )
    product(
        "league_task_chop_50_magic_logs_in_tirannwn",
        WOODCUTTING,
        "obj.magic_logs",
        times = 50,
        counter = "varbit.league_task_chop_magic_logs_in_tirannwn",
        areas = listOf("area.league_region_tirannwn"),
    )
    product(
        "league_task_chop_a_magic_log_at_the_forsaken_tower",
        WOODCUTTING,
        "obj.magic_logs",
        areas = listOf("area.league_forsaken_tower"),
    )
}
