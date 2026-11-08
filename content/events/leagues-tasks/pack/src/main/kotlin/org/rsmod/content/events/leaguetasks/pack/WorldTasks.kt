package org.rsmod.content.events.leaguetasks.pack

import org.rsmod.content.events.leaguetasks.pack.LeagueTaskTriggerTable.AREA
import org.rsmod.content.events.leaguetasks.pack.LeagueTaskTriggerTable.INTERFACE

internal fun TriggerRows.worldTasks() {
    opened("league_task_open_the_leagues_menu", "interface.league_side_panel")
    opened("league_task_use_the_bank_in_nardah", "interface.bankmain", "area.league_nardah_bank")
    opened(
        "league_task_use_the_bank_at_the_mage_arena",
        "interface.bankmain",
        "area.league_mage_arena_bank",
    )
    opened("league_task_use_the_bank_in_lletya", "interface.bankmain", "area.league_lletya_bank")
    opened("league_task_use_the_bank_on_jatizso", "interface.bankmain", "area.league_jatizso_bank")
    opened(
        "league_task_use_the_bank_on_neitiznot",
        "interface.bankmain",
        "area.league_neitiznot_bank",
    )

    entered("league_task_enter_the_taverley_dungeon", "area.taverley_dungeon")
    entered("league_task_enter_the_brimhaven_dungeon", "area.brimhaven_dungeon")
    entered("league_task_enter_the_fishing_guild", "area.fishing_guild")
    entered("league_task_visit_ferox_enclave", "area.ferox_enclave")
    entered("league_task_visit_port_phasmatys", "area.league_port_phasmatys")
    entered("league_task_enter_the_woodcutting_guild", "area.league_woodcutting_guild")
    entered("league_task_enter_the_crafting_guild", "area.league_crafting_guild")
    entered("league_task_enter_the_ranging_guild", "area.league_ranging_guild")
    entered("league_task_enter_the_wizards_guild", "area.league_wizards_guild")
    entered("league_task_enter_the_farming_guild", "area.league_farming_guild")
    entered("league_task_visit_the_tree_gnome_stronghold", "area.league_gnome_stronghold")
    entered("league_task_visit_the_tree_gnome_village", "area.league_gnome_village")
    entered("league_task_visit_harmony_island", "area.league_harmony_island")
    entered("league_task_visit_mos_leharmless", "area.league_mos_leharmless")
    entered(
        "league_task_enter_the_wilderness_resource_area",
        "area.league_wilderness_resource_area",
    )
    variable("league_task_open_the_crystal_chest", "varp.kc_crystal_chest", 1)
}

internal fun TriggerRows.opened(task: String, interf: String, area: String? = null) {
    trigger("dbrow.$task", LeagueTaskTrigger.Interface) {
        columnRSCM(INTERFACE, interf)
        if (area != null) {
            columnRSCM(AREA, area)
        }
    }
}

internal fun TriggerRows.entered(task: String, area: String) {
    trigger("dbrow.$task", LeagueTaskTrigger.Enter) { columnRSCM(AREA, area) }
}
