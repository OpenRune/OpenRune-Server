package org.rsmod.content.events.leaguetasks.pack

private const val CRAFTING = "stat.crafting"
private const val RUNECRAFTING = "stat.runecrafting"

/** Crystal singing publishes its products under Smithing. */
private const val CRYSTAL_SINGING = "stat.smithing"

private val SNELMS =
    arrayOf(
        "obj.snelm_round_swamp",
        "obj.snelm_round_red+black",
        "obj.snelm_round_yellow",
        "obj.snelm_round_blue",
        "obj.snelm_round_orange",
        "obj.snelm_point_swamp",
        "obj.snelm_point_red+black",
        "obj.snelm_point_yellow",
        "obj.snelm_point_blue",
        "obj.snelm_vampyre",
    )

private val COMBINATION_RUNES =
    arrayOf(
        "obj.mistrune",
        "obj.dustrune",
        "obj.mudrune",
        "obj.smokerune",
        "obj.steamrune",
        "obj.lavarune",
        "obj.sunfirerune",
    )

private val CRYSTAL_ARMOUR =
    arrayOf(
        "obj.crystal_helmet",
        "obj.crystal_chestplate",
        "obj.crystal_platelegs",
        "obj.crystal_helmet_inactive",
        "obj.crystal_chestplate_inactive",
        "obj.crystal_platelegs_inactive",
    )

internal fun TriggerRows.craftingTasks() {
    product("action_craft_leather_chaps", CRAFTING, "obj.leather_chaps")
    product("action_cut_a_ruby", CRAFTING, "obj.ruby")
    product("league_task_spin_a_ball_of_wool", CRAFTING, "obj.ball_of_wool")
    product("league_task_successfully_cut_a_red_topaz", CRAFTING, "obj.red_topaz")
    product("league_task_craft_a_snelm", CRAFTING, *SNELMS)
    product("action_craft_a_sapphire_amulet", CRAFTING, "obj.unstrung_sapphire_amulet")
    product("league_task_craft_an_emerald_ring", CRAFTING, "obj.emerald_ring")
    product("league_task_assemble_a_slayer_helm", CRAFTING, "obj.slayer_helm")
    product("league_task_craft_a_dragonstone_amulet", CRAFTING, "obj.unstrung_dragonstone_amulet")
    product("league_task_craft_an_onyx_amulet", CRAFTING, "obj.unstrung_onyx_amulet")
    product(
        "league_task_craft_some_pottery_in_sophanem",
        CRAFTING,
        areas = listOf("area.league_sophanem"),
    )
    product(
        "league_task_blow_100_light_orbs",
        CRAFTING,
        "obj.dorgesh_lightbulb_nofilament",
        times = 100,
        counter = "varbit.league_counter_blow_light_orbs",
    )
    product(
        "league_task_craft_100_unpowered_orbs",
        CRAFTING,
        "obj.stafforb",
        times = 100,
        counter = "varbit.league_counter_craft_unpowered_orbs",
    )
    product(
        "league_task_craft_30_blue_dragonhide_bodies",
        CRAFTING,
        "obj.blue_dragonhide_body",
        times = 30,
        counter = "varbit.league_counter_craft_blue_dragonhide_bodies",
    )
    product("league_task_craft_a_piece_of_crystal_armour", CRYSTAL_SINGING, *CRYSTAL_ARMOUR)
    equip(
        "league_task_equip_a_full_set_of_yakhide_armour",
        "obj.yak_hide_armour_body",
        "obj.yak_hide_armour_greaves",
        worn = 2,
    )

    product("league_task_craft_a_fire_rune", RUNECRAFTING, "obj.firerune")
    product("league_task_craft_an_air_rune", RUNECRAFTING, "obj.airrune")
    product("league_task_craft_a_body_rune", RUNECRAFTING, "obj.bodyrune")
    product("league_task_craft_a_lava_rune_at_the_fire_altar", RUNECRAFTING, "obj.lavarune")
    product("league_task_craft_a_wrath_rune", RUNECRAFTING, "obj.wrathrune")
    product("league_task_craft_any_combination_rune", RUNECRAFTING, *COMBINATION_RUNES)
    runes(
        "league_task_craft_50_astral_runes",
        "obj.astralrune",
        50,
        "league_counter_craft_astral_runes",
    )
    runes(
        "league_task_craft_50_nature_runes",
        "obj.naturerune",
        50,
        "league_counter_craft_nature_runes",
    )
    runes(
        "league_task_craft_50_death_runes",
        "obj.deathrune",
        50,
        "league_counter_craft_death_runes",
    )
    runes("league_task_craft_50_law_runes", "obj.lawrune", 50, "league_counter_craft_law_runes")
    runes(
        "league_task_craft_1000_sunfire_runes",
        "obj.sunfirerune",
        1000,
        "league_task_craft_sunfire_runes",
    )
    runes(
        "league_task_craft_100_aether_runes",
        "obj.aetherrune",
        100,
        "league_task_craft_aether_runes",
    )
    product(
        "league_task_craft_100_blood_runes",
        RUNECRAFTING,
        "obj.bloodrune",
        times = 100,
        counter = "varbit.league_task_craft_blood_runes",
        areas = listOf("area.league_region_kourend"),
    )

    product("league_task_craft_a_ghorrock_teleport_tablet", "stat.magic", "obj.tablet_ghorrock")

    product(
        "league_task_fill_a_bucket_with_sand_at_the_sunset_coast",
        CRAFTING,
        "obj.bucket_sand",
        areas = listOf("area.league_region_varlamore"),
    )
    product(
        "league_task_fill_up_20_buckets_of_sand_in_rellekka",
        CRAFTING,
        "obj.bucket_sand",
        times = 20,
        counter = "varbit.league_counter_fill_sand_in_rellekka",
        areas = listOf("area.league_region_fremennik"),
    )
}

private fun TriggerRows.runes(task: String, rune: String, times: Int, counter: String) {
    product(task, RUNECRAFTING, rune, times = times, counter = "varbit.$counter")
}
