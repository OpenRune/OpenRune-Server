package org.rsmod.content.events.leaguetasks.pack

private const val MINING = "stat.mining"

private val GRANITE =
    arrayOf("obj.enakh_granite_tiny", "obj.enakh_granite_small", "obj.enakh_granite_medium")

private val DRAGON_PICKAXES =
    listOf(
        "obj.dragon_pickaxe",
        "obj.dragon_pickaxe_pretty",
        "obj.zalcano_pickaxe",
        "obj.trailblazer_pickaxe_no_infernal",
        "obj.trailblazer_reloaded_pickaxe_no_infernal",
    )

internal fun TriggerRows.miningTasks() {
    product(
        "league_task_mine_5_tin_ore",
        MINING,
        "obj.tin_ore",
        times = 5,
        counter = "varbit.league_counter_mine_tin_ore",
    )
    product("action_mine_some_clay", MINING, "obj.clay")
    product(
        "league_task_mine_some_ore_with_a_steel_pickaxe",
        MINING,
        tools = listOf("obj.steel_pickaxe"),
    )
    product(
        "league_task_mine_some_ore_with_a_rune_pickaxe",
        MINING,
        tools = listOf("obj.rune_pickaxe"),
    )
    product(
        "action_mine_some_coal_from_stone_cutter_outpost",
        MINING,
        "obj.coal",
        areas = listOf("area.league_stonecutter_outpost"),
    )
    product(
        "league_task_mine_10_silver",
        MINING,
        "obj.silver_ore",
        times = 10,
        counter = "varbit.league_counter_mine_silver_ore",
    )
    product(
        "league_task_mine_50_iron_ore",
        MINING,
        "obj.iron_ore",
        times = 50,
        counter = "varbit.league_counter_mine_iron_ore",
    )
    product(
        "league_task_mine_50_mithril_ore",
        MINING,
        "obj.mithril_ore",
        times = 50,
        counter = "varbit.league_task_mine_mithril_ore",
    )
    product(
        "league_task_mine_20_mithril_ore_in_the_stonecutter_outpost",
        MINING,
        "obj.mithril_ore",
        times = 20,
        counter = "varbit.league_task_mine_mithril_ore_in_the_stonecutter_outpost",
        areas = listOf("area.league_stonecutter_outpost"),
    )
    product(
        "league_task_mine_250_blessed_bone_shards",
        MINING,
        "obj.blessed_bone_shard",
        times = 250,
        counter = "varbit.league_task_mine_blessed_bone_shards",
    )
    product(
        "league_task_mine_15_granite_in_the_necropolis",
        MINING,
        *GRANITE,
        times = 15,
        counter = "varbit.league_counter_mine_granite_in_the_necropolis",
        areas = listOf("area.league_necropolis"),
    )
    product(
        "league_task_mine_25_gold_rocks_in_tirannwn",
        MINING,
        "obj.gold_ore",
        times = 25,
        counter = "varbit.league_counter_mine_gold_in_tirannwn",
        areas = listOf("area.league_region_tirannwn"),
    )
    product(
        "league_task_mine_30_chunks_of_granite",
        MINING,
        *GRANITE,
        times = 30,
        counter = "varbit.league_counter_mine_granite",
        areas = listOf("area.league_region_desert"),
    )
    product(
        "league_task_mine_50_volcanic_sulphur",
        MINING,
        "obj.lovakengj_sulphur",
        times = 50,
        counter = "varbit.league_task_mine_sulphur",
    )
    product(
        "league_task_mine_50_adamantite_ore",
        MINING,
        "obj.adamantite_ore",
        times = 50,
        counter = "varbit.league_counter_mine_adamantite_ore",
    )
    product(
        "league_task_mine_200_soft_clay_in_tirannwn",
        MINING,
        "obj.softclay",
        times = 200,
        counter = "varbit.league_counter_mine_soft_clay_in_tirannwn",
        areas = listOf("area.league_region_tirannwn"),
    )
    product(
        "league_task_mine_100_runite_ore_in_tirannwn",
        MINING,
        "obj.runite_ore",
        times = 100,
        counter = "varbit.league_counter_mine_runite_in_tirannwn",
        areas = listOf("area.league_region_tirannwn"),
    )
    product(
        "league_task_mine_1000_ancient_essence",
        MINING,
        "obj.ancient_essence",
        times = 1000,
        counter = "varbit.league_task_mine_1000_ancient_essence",
    )
    product(
        "league_task_mine_some_runite_ore_in_the_wilderness",
        MINING,
        "obj.runite_ore",
        areas = listOf("area.wilderness"),
    )
    product(
        "league_task_mine_runite_dragon_pickaxe_in_wilderness",
        MINING,
        "obj.runite_ore",
        tools = DRAGON_PICKAXES,
        areas = listOf("area.wilderness"),
    )

    equip(
        "league_task_equip_a_full_prospector_outfit",
        "obj.motherlode_reward_hat",
        "obj.motherlode_reward_top",
        "obj.motherlode_reward_legs",
        "obj.motherlode_reward_boots",
        "obj.motherlode_reward_hat_gold",
        "obj.motherlode_reward_top_gold",
        "obj.motherlode_reward_legs_gold",
        "obj.motherlode_reward_boots_gold",
        "obj.fossil_motherlode_reward_hat",
        "obj.fossil_motherlode_reward_top",
        "obj.fossil_motherlode_reward_legs",
        "obj.fossil_motherlode_reward_boots",
        worn = 4,
    )
    equip(
        "league_task_equip_a_full_set_of_golden_prospector",
        "obj.motherlode_reward_hat_gold",
        "obj.motherlode_reward_top_gold",
        "obj.motherlode_reward_legs_gold",
        "obj.motherlode_reward_boots_gold",
        worn = 4,
    )
}
