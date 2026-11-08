package org.rsmod.content.events.leaguetasks.pack

private const val COOKING = "stat.cooking"

internal fun TriggerRows.cookingTasks() {
    product("league_task_cook_shrimp", COOKING, "obj.shrimp")
    product(
        "league_task_cook_a_rabbit_in_tirannwn",
        COOKING,
        "obj.cooked_rabbit",
        areas = listOf("area.league_region_tirannwn"),
    )
    product("action_make_a_meat_pizza", COOKING, "obj.meat_pizza")
    product("league_task_cook_a_dark_crab", COOKING, "obj.dark_crab")
    product("league_task_butter_a_potato", COOKING, "obj.potato_butter")

    cooked("league_task_cook_100_swordfish", "obj.swordfish", 100, "cook_swordfish")
    cooked("league_task_cook_50_tuna", "obj.tuna", 50, "cook_tuna")
    cooked("league_task_cook_100_sharks", "obj.shark", 100, "cook_sharks")
    cooked("league_task_cook_25_meat_pies", "obj.meat_pie", 25, "cook_meat_pies")
    cooked("league_task_cook_100_karambwans", "obj.tbwt_cooked_karambwan", 100, "cook_karambwans")
    cooked("league_task_cook_100_anglerfish", "obj.anglerfish", 100, "cook_anglerfish")
    product(
        "league_task_cook_20_sharks_in_darkmeyer",
        COOKING,
        "obj.shark",
        times = 20,
        counter = "varbit.league_counter_cook_sharks_in_darkmeyer",
        areas = listOf("area.darkmeyer"),
    )
    product(
        "league_task_cook_100_moonlight_antelopes",
        COOKING,
        "obj.antelopemoon_cooked",
        times = 100,
        counter = "varbit.league_task_cook_moonlight_antelopes",
    )
}

private fun TriggerRows.cooked(task: String, food: String, times: Int, counter: String) {
    product(task, COOKING, food, times = times, counter = "varbit.league_counter_$counter")
}
