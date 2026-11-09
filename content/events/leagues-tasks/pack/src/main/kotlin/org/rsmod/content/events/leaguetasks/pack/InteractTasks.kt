package org.rsmod.content.events.leaguetasks.pack

import org.rsmod.content.events.leaguetasks.pack.LeagueTaskTriggerTable.AREA
import org.rsmod.content.events.leaguetasks.pack.LeagueTaskTriggerTable.LOC
import org.rsmod.content.events.leaguetasks.pack.LeagueTaskTriggerTable.NPC
import org.rsmod.content.events.leaguetasks.pack.LeagueTaskTriggerTable.OPTION

/**
 * Talk-to, pet, open and other option tasks: met when the option's handler finishes on any npc or
 * loc sharing the task's display name. The option slot is the one those types give the option.
 */
internal fun TriggerRows.interactTasks() {
    interact(
        "league_task_talk_to_any_port_master",
        1,
        "npc.port_task_master_port_sarim",
        "npc.port_task_master_musa_point",
        "npc.port_task_master_pandemonium",
        "npc.port_task_master_catherby",
        "npc.port_task_master_entrana",
        "npc.port_task_master_ardougne",
        "npc.port_task_master_brimhaven",
        "npc.port_task_master_port_khazard",
        "npc.port_task_master_corsair_cove",
        "npc.port_task_master_ruins_of_unkah",
        "npc.port_task_master_port_piscarillius",
        "npc.port_task_master_civitas_illa_fortis",
        "npc.port_task_master_rellekka",
        "npc.port_task_master_lands_end",
        "npc.port_task_master_hosidius",
        "npc.port_task_master_cairn_isle",
        "npc.port_task_master_sunset_coast",
        "npc.port_task_master_the_summer_shore",
        "npc.port_task_master_aldarin",
        "npc.port_task_master_void_knights_outpost",
        "npc.port_task_master_port_roberts",
        "npc.port_task_master_red_rock",
        "npc.port_task_master_mos_leharmless",
        "npc.port_task_master_etceteria",
        "npc.port_task_master_port_tyras",
        "npc.port_task_master_deepfin_point",
        "npc.port_task_master_jatizso",
        "npc.port_task_master_neitiznot",
        "npc.port_task_master_prifddinas",
        "npc.port_task_master_piscatoris",
        "npc.port_task_master_lunar_isle",
    )
    interact(
        "league_task_pet_renu",
        4,
        "npc.quetzal_child_green",
        "npc.quetzal_child_orange",
        "npc.quetzal_child_blue",
        "npc.quetzal_child_cyan",
        "npc.quetzal_child_green_orange",
    )
    interact(
        "league_task_pet_xolo_in_civitas",
        1,
        "npc.varlamore_dog_xolo",
        "npc.varlamore_dog_xolo_indoors",
        area = "area.league_region_varlamore",
    )
    interact(
        "league_task_pet_a_caique",
        1,
        "npc.varlamore_parrot_caique_rock_north",
        "npc.varlamore_parrot_caique_rock_south",
        "npc.varlamore_parrot_caique_rock_east",
        "npc.varlamore_parrot_caique_rock_west",
    )
    interact(
        "league_task_talk_to_a_gladiator",
        1,
        "npc.colosseeum_gladiator_1",
        "npc.colosseeum_gladiator_2",
        "npc.colosseeum_gladiator_3",
        "npc.civitas_gladiator_pushup_1",
        "npc.civitas_gladiator_pushup_2",
        "npc.civitas_gladiator_pushup_3",
        "npc.civitas_gladiator_attacking_1",
        "npc.civitas_gladiator_attacking_2",
        "npc.civitas_gladiator_attacking_3",
    )
    interact(
        "league_task_talk_to_ilfeen_in_tirannwn",
        1,
        "npc.roving_ilfeen_1op",
        "npc.roving_ilfeen_2ops",
        "npc.sote_ilfeen_vis",
        area = "area.league_region_tirannwn",
    )
    interact(
        "league_task_talk_to_the_mysterious_old_man_in_falador",
        1,
        "npc.macro_cubenpc",
        "npc.macro_mysterious_old_man",
        "npc.macro_mysterious_old_man_underwater",
        "npc.macro_maze_invitation",
        "npc.macro_mime_invitation",
        area = "area.league_region_asgarnia",
    )
    interact(
        "league_task_talk_to_the_voice_of_yama",
        1,
        "npc.voice_of_yama_1op",
        "npc.voice_of_yama_2op",
        "npc.voice_of_yama_3op",
    )
    interact(
        "league_task_pet_frank",
        1,
        "npc.darkm_bloodveld_pet",
    )
    interact(
        "league_task_open_1_grubby_chest",
        1,
        "loc.hosdun_grubby_chest_closed",
    )
    interact(
        "league_task_open_10_grubby_chests",
        1,
        "loc.hosdun_grubby_chest_closed",
        times = 10,
        counter = "varbit.league_counter_open_grubby_chests",
    )
    interact(
        "league_task_open_25_grubby_chests",
        1,
        "loc.hosdun_grubby_chest_closed",
        times = 25,
        counter = "varbit.league_counter_open_grubby_chests",
    )
    interact(
        "league_task_open_a_bronze_chest",
        1,
        "loc.shadechest_bronze_bloodred",
        "loc.shadechest_bronze_brown",
        "loc.shadechest_bronze_crimson",
        "loc.shadechest_bronze_black",
        "loc.shadechest_bronze_purple",
    )
    interact(
        "league_task_open_a_black_chest",
        1,
        "loc.shadechest_black_bloodred",
        "loc.shadechest_black_brown",
        "loc.shadechest_black_crimson",
        "loc.shadechest_black_black",
        "loc.shadechest_black_purple",
    )
    interact(
        "league_task_open_a_gold_chest",
        1,
        "loc.shadechest_gold_bloodred",
        "loc.shadechest_gold_brown",
        "loc.shadechest_gold_crimson",
        "loc.shadechest_gold_black",
        "loc.shadechest_gold_purple",
    )
    interact(
        "league_task_open_the_enhanced_crystal_chest",
        1,
        "loc.prif_crystal_chest_closed",
    )
    interact(
        "league_task_open_one_of_larrans_chests",
        1,
        "loc.slayer_larran_chest_small_closed",
        "loc.slayer_larran_chest_big_closed",
    )
    interact(
        "league_task_open_15_larrans_chests",
        1,
        "loc.slayer_larran_chest_small_closed",
        "loc.slayer_larran_chest_big_closed",
        times = 15,
        counter = "varbit.league_counter_open_larrans_chests",
    )
    interact(
        "league_task_open_50_larrans_chests",
        1,
        "loc.slayer_larran_chest_small_closed",
        "loc.slayer_larran_chest_big_closed",
        times = 50,
        counter = "varbit.league_counter_open_larrans_chests",
    )
    interact(
        "league_task_open_1_zombie_pirate_locker",
        1,
        "loc.wildy_pirate_boat_locker",
    )
    interact(
        "league_task_open_15_zombie_pirate_lockers",
        1,
        "loc.wildy_pirate_boat_locker",
        times = 15,
        counter = "varbit.league_counter_open_zombie_pirate_lockers",
    )
    interact(
        "league_task_open_50_zombie_pirate_lockers",
        1,
        "loc.wildy_pirate_boat_locker",
        times = 50,
        counter = "varbit.league_counter_open_zombie_pirate_lockers",
    )
    interact(
        "league_task_open_1_muddy_chest",
        1,
        "loc.muddy_chestclosed",
    )
    interact(
        "league_task_open_10_muddy_chests",
        1,
        "loc.muddy_chestclosed",
        times = 10,
        counter = "varbit.league_counter_open_muddy_chests",
    )
    interact(
        "league_task_open_an_ornate_lockbox",
        1,
        "loc.camdozaal_vault_chest_large_available",
    )
    interact(
        "league_task_pray_at_the_elidinis_statuette",
        1,
        "loc.elid_statuette_placed",
    )
    interact(
        "league_task_use_the_pottery_oven_in_civitas",
        1,
        "loc.viking_potteryoven",
        "loc.fai_barbarian_pottery_oven",
        "loc.potteryoven",
        "loc.darkm_poor_pottery_oven",
        "loc.amenity_potteryoven_built",
        area = "area.league_region_varlamore",
    )
}

private fun TriggerRows.interact(
    task: String,
    option: Int,
    vararg targets: String,
    times: Int = 1,
    counter: String? = null,
    area: String? = null,
) {
    trigger("dbrow.$task", LeagueTaskTrigger.Interact) {
        val (npcs, locs) = targets.partition { it.startsWith("npc.") }
        if (npcs.isNotEmpty()) {
            columnRSCM(NPC, npcs)
        }
        if (locs.isNotEmpty()) {
            columnRSCM(LOC, locs)
        }
        if (option != 0) {
            column(OPTION, option)
        }
        if (area != null) {
            columnRSCM(AREA, area)
        }
        times(times, counter)
    }
}
