package org.rsmod.content.events.leaguetasks.pack

/**
 * Obtain tasks count items entering the player's possession by any means; purchase tasks count
 * shop purchases. Where a task names the source, the item is only obtainable from it.
 */
internal fun TriggerRows.itemTasks() {
    obtained(
        "league_task_obtain_a_bird_nest",
        "obj.bird_nest_egg_red",
        "obj.bird_nest_egg_green",
        "obj.bird_nest_egg_blue",
        "obj.bird_nest_seeds",
        "obj.bird_nest_ring",
        "obj.bird_nest_empty",
        "obj.bird_nest_cheapseeds",
        "obj.bird_nest_decentseeds",
    )
    obtained(
        "league_task_receive_an_agility_arena_ticket",
        "obj.agilityarena_ticket",
        "obj.agilityarena_ticket_new",
    )
    objTask(
        "league_task_obtain_20_golden_nuggets",
        LeagueTaskTrigger.Obtain,
        "obj.motherlode_nugget",
        times = 20,
        counter = "varbit.league_task_motherlode_nugget",
    )
    obtained("league_task_obtain_an_ecumenical_key", "obj.ecumenical_key")
    obtained("league_task_obtain_the_big_harpoonfish", "obj.poh_trophydrop_harpoonfish")
    obtained("league_task_obtain_the_plank_sack", "obj.plank_sack")
    obtained("league_task_obtain_the_temple_key", "obj.hosdun_temple_key")
    obtained("league_task_obtain_the_huntsmans_kit", "obj.huntsmans_kit")
    obtained("league_task_obtain_the_fish_barrel", "obj.fish_barrel_closed")
    obtained("league_task_obtain_a_crystal_tool_seed", "obj.prif_tool_seed")
    obtained("league_task_obtain_a_zalcano_shard", "obj.zalcano_pickaxe_kit")
    product("action_obtain_an_old_boot_from_a_fishing_spot", "stat.fishing", "obj.old_boot")

    bought("league_task_buy_a_snapdragon_from_pirate_jackie_the_fruit", "obj.snapdragon_seed")
    bought("league_task_purchase_a_seed_box", "obj.seed_box")
    bought("league_task_purchase_the_reagents_pouch", "obj.mm_secondary_pouch")
    bought("league_task_purchase_a_celestial_ring", "obj.celestial_ring")
    bought("league_task_purchase_the_chugging_barrel", "obj.mm_prepot_device")
    bought("league_task_purchase_an_onyx_in_mor_ul_rek", "obj.onyx")
}

private fun TriggerRows.obtained(task: String, vararg objs: String) {
    objTask(task, LeagueTaskTrigger.Obtain, *objs)
}

private fun TriggerRows.bought(task: String, vararg objs: String) {
    objTask(task, LeagueTaskTrigger.ShopBuy, *objs)
}
