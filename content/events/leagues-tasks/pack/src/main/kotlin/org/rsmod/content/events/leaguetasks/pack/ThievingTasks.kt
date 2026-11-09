package org.rsmod.content.events.leaguetasks.pack

import org.rsmod.content.events.leaguetasks.pack.LeagueTaskTriggerTable.AREA
import org.rsmod.content.events.leaguetasks.pack.LeagueTaskTriggerTable.LOC
import org.rsmod.content.events.leaguetasks.pack.LeagueTaskTriggerTable.NPC
import org.rsmod.content.events.leaguetasks.pack.LeagueTaskTriggerTable.OBJ

/** Pickpocket tasks list every pickpocketable npc sharing the name; stall tasks every stall. */
internal fun TriggerRows.thievingTasks() {
    pickpocket(
        "league_task_pickpocket_a_citizen",
        "npc.varrock_man1",
        "npc.varrock_woman1",
        "npc.man",
        "npc.man2",
        "npc.man3",
        "npc.man4",
        "npc.man4_for_musa_point",
        "npc.woman",
        "npc.woman2",
        "npc.woman3",
        "npc.al_kharid_man",
        "npc.falador_man2",
        "npc.falador_man3",
        "npc.falador_woman2",
        "npc.ardougnian_male1",
        "npc.ardougnian_female1",
        "npc.karamja_man",
        "npc.death_man_indoors1",
        "npc.man_indoor",
        "npc.zeah_man",
        "npc.zeah_man2",
        "npc.zeah_man3",
        "npc.zeah_woman",
        "npc.zeah_woman2",
        "npc.zeah_woman3",
        "npc.zeah_woman_outside",
        "npc.shayzien_woman_1",
        "npc.shayzien_woman_2",
        "npc.shayzien_man_1",
        "npc.shayzien_man_2",
        "npc.man5",
        "npc.woman4",
    )
    pickpocket(
        "league_task_pickpocket_a_master_farmer",
        "npc.master_farmer_1",
        "npc.master_farmer_2",
        "npc.master_farmer_1_f",
        "npc.master_farmer_2_f",
        "npc.varlamore_master_farmer_m_1",
        "npc.varlamore_master_farmer_m_2",
        "npc.varlamore_master_farmer_m_3",
        "npc.varlamore_master_farmer_m_4",
        "npc.varlamore_master_farmer_f_1",
        "npc.varlamore_master_farmer_f_2",
        "npc.varlamore_master_farmer_f_3",
        "npc.varlamore_master_farmer_f_4",
        "npc.kastori_master_farmer_m_1",
        "npc.kastori_master_farmer_m_2",
        "npc.kastori_master_farmer_f_1",
        "npc.kastori_master_farmer_f_2",
    )
    pickpocket(
        "league_task_pickpocket_a_knight_of_varlamore_20_times",
        "npc.varlamore_knight_m_1",
        "npc.varlamore_knight_m_2",
        "npc.varlamore_knight_m_3",
        "npc.varlamore_knight_f_1",
        "npc.varlamore_knight_f_2",
        "npc.varlamore_knight_f_3",
        times = 20,
        counter = "varbit.league_counter_pickpocket_knights_of_varlamore",
    )
    pickpocket(
        "league_task_pickpocket_a_bandit_in_the_bandit_camp",
        "npc.fourdiamonds_sword_bandit_1",
        "npc.fourdiamonds_sword_bandit_free",
        "npc.feud_arabian_guard1_1",
        "npc.feud_arabian_guard1_2",
        "npc.feud_arabian_guard2_1",
        "npc.feud_arabian_guard2_2",
        area = "area.league_region_desert",
    )
    pickpocket(
        "league_task_pickpocket_a_knight_of_ardougne_50_times",
        "npc.knight_of_ardougne",
        "npc.knight_of_ardougne2",
        "npc.knight_of_ardougne_west_vis",
        "npc.knight_of_ardougne_f_west_vis",
        "npc.knight_of_ardougne_f",
        times = 50,
        counter = "varbit.league_counter_pickpocket_knights_of_ardougne",
    )
    pickpocket(
        "league_task_pickpocket_a_menaphite_thug_50_times",
        "npc.feud_egyptian_doorman_2",
        times = 50,
        counter = "varbit.league_counter_pickpocket_menaphite_thugs",
    )
    steal(
        "action_steal_some_bread",
        "loc.bakerymarket",
        "loc.dwarf_market_bakery",
        "loc.icthalarins_bakerymarket",
        "loc.hos_stall_bread",
        "loc.cakethiefstall",
        "loc.contact_bakerymarket_vis",
        "loc.cam_torum_market_bakery",
        "loc.fortis_market_stall_bakers",
        "loc.barracuda_market_stall_bakery",
        obj = "obj.bread",
    )
    steal(
        "league_task_steal_from_the_fortis_spice_stall",
        "loc.fortis_market_stall_spice",
    )
    steal(
        "league_task_steal_a_fish",
        "loc.viking_fish_market",
        "loc.misc_fish_market",
        "loc.etc_fish_market",
        "loc.fish_stall_warrens",
        "loc.vikingexile_fish_market",
        "loc.barracuda_market_stall_fish",
        "loc.tortugan_stall_fish",
        "loc.pandemonium_fishing_stall",
        "loc.port_roberts_market_stall_fish",
    )
    steal(
        "league_task_steal_a_wooden_stock",
        "loc.xbows_dwarf_market",
        obj = "obj.xbows_crossbow_stock_wood",
    )
    steal(
        "league_task_steal_from_a_fur_stall",
        "loc.furmarket",
        "loc.viking_fur_market",
        "loc.icthalarins_furmarket",
        "loc.furthiefstall",
        "loc.contact_furmarket",
        "loc.vikingexile_fur_market",
        "loc.fortis_market_stall_fur",
        "loc.barracuda_market_stall_fur",
        "loc.port_roberts_market_stall_fur",
        area = "area.league_region_kandarin",
    )
    steal(
        "league_task_steal_from_a_silver_stall",
        "loc.silvermarket",
        "loc.dwarf_market_silver",
        "loc.silverthiefstall",
        "loc.prif_marketstall_silver",
        "loc.silverthiefstall_noop",
        "loc.tortugan_stall_silver",
        "loc.port_roberts_market_stall_silver",
        area = "area.league_region_kandarin",
    )
    steal(
        "league_task_steal_from_a_spice_stall",
        "loc.spicemarket",
        "loc.icthalarins_spicemarket",
        "loc.spicethiefstall",
        "loc.contact_spicemarket",
        "loc.prif_marketstall_spice",
        "loc.fortis_market_stall_spice",
        "loc.port_roberts_market_stall_spice",
        area = "area.league_region_kandarin",
    )
    steal(
        "league_task_thieve_a_tiara_from_a_silver_stall_in_tirannwn",
        "loc.silvermarket",
        "loc.dwarf_market_silver",
        "loc.silverthiefstall",
        "loc.prif_marketstall_silver",
        "loc.silverthiefstall_noop",
        "loc.tortugan_stall_silver",
        "loc.port_roberts_market_stall_silver",
        obj = "obj.tiara",
        area = "area.league_region_tirannwn",
    )
    steal(
        "league_task_steal_from_a_gem_stall",
        "loc.gemmarket",
        "loc.dwarf_market_gems",
        "loc.icthalarins_gemmarket",
        "loc.gemthiefstall",
        "loc.contact_gemmarket",
        "loc.prif_marketstall_gem",
        "loc.gemthiefstall_noop",
        "loc.fortis_market_stall_gems",
        "loc.aldarin_market_stall_gems",
        "loc.barracuda_market_stall_gem",
        "loc.port_roberts_market_stall_gems",
    )
    steal(
        "league_task_thieve_a_diamond_from_a_gem_stall_in_tirannwn",
        "loc.gemmarket",
        "loc.dwarf_market_gems",
        "loc.icthalarins_gemmarket",
        "loc.gemthiefstall",
        "loc.contact_gemmarket",
        "loc.prif_marketstall_gem",
        "loc.gemthiefstall_noop",
        "loc.fortis_market_stall_gems",
        "loc.aldarin_market_stall_gems",
        "loc.barracuda_market_stall_gem",
        "loc.port_roberts_market_stall_gems",
        obj = "obj.uncut_diamond",
        area = "area.league_region_tirannwn",
    )
    steal(
        "league_task_steal_a_chisel",
        "loc.mm_stall_crafting",
        "loc.dwarf_market_crafting",
        "loc.tortugan_stall_crafting",
        obj = "obj.chisel",
    )
    steal(
        "league_task_steal_a_golovanova_fruit_top",
        "loc.hos_fruit_stall",
        "loc.hos_fruit_stall_01",
        "loc.hos_fruit_stall_02",
        "loc.tortugan_stall_fruit",
        obj = "obj.golovanova_top",
        area = "area.league_region_kourend",
    )
}

private fun TriggerRows.pickpocket(
    task: String,
    vararg npcs: String,
    times: Int = 1,
    counter: String? = null,
    area: String? = null,
) {
    trigger("dbrow.$task", LeagueTaskTrigger.Pickpocket) {
        columnRSCM(NPC, npcs.asList())
        if (area != null) {
            columnRSCM(AREA, area)
        }
        times(times, counter)
    }
}

private fun TriggerRows.steal(
    task: String,
    vararg stalls: String,
    obj: String? = null,
    area: String? = null,
) {
    trigger("dbrow.$task", LeagueTaskTrigger.Steal) {
        columnRSCM(LOC, stalls.asList())
        if (obj != null) {
            columnRSCM(OBJ, obj)
        }
        if (area != null) {
            columnRSCM(AREA, area)
        }
    }
}
