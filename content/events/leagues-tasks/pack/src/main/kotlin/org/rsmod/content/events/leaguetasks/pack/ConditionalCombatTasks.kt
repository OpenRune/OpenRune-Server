package org.rsmod.content.events.leaguetasks.pack

internal fun TriggerRows.conditionalCombatTasks() {
    kill(
        "league_task_kill_a_kalphite_with_the_keris_partisan",
        "npc.kalphite_worker",
        "npc.kalphite_worker_chamber",
        "npc.kalphite_worker_strongholdcave",
        "npc.kalphite_soldier",
        "npc.kalphite_soldier_strongholdcave",
        "npc.kalphite_lord",
        "npc.kalphite_lord_chamber",
        "npc.kalphite_lord_strongholdcave",
        "npc.kalphite_larva",
        "npc.kalphite_queen",
        "npc.kalphite_flyingqueen",
        "npc.league_6_kalphite_queen",
        "npc.league_6_kalphite_flyingqueen",
        tools =
            listOf(
                "obj.keris_partisan",
                "obj.keris_partisan_breach",
                "obj.keris_partisan_corruption",
                "obj.keris_partisan_sun",
                "obj.keris_partisan_amascut",
            ),
    )
    kill(
        "league_task_defeat_an_elf_in_tirannwn",
        "npc.mourning_guard_melee",
        "npc.mourning_guard_melee2",
        "npc.mourning_guard_range",
        "npc.mourning_guard_range2",
        counter = "varbit.league_counter_kill_elves_in_tirannwn",
        area = "area.league_region_tirannwn",
    )
    kill(
        "league_task_defeat_a_tortoise_with_riders_in_kandarin",
        "npc.battle_tortoise",
        area = "area.league_region_kandarin",
    )
    kill(
        "league_task_kill_a_moss_fire_hill_giant_in_kourend",
        "npc.kourend_mossgiant",
        counter = "varbit.league_killed_moss_giant_kourend",
    )
    kill(
        "league_task_kill_a_moss_fire_hill_giant_in_kourend",
        "npc.kourend_firegiant1",
        "npc.kourend_firegiant2",
        counter = "varbit.league_killed_fire_giant_kourend",
    )
    kill(
        "league_task_kill_a_moss_fire_hill_giant_in_kourend",
        "npc.kourend_hillgiant",
        counter = "varbit.league_killed_hill_giant_kourend",
    )
}
