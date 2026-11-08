package org.rsmod.content.events.leaguetasks.pack

import org.rsmod.content.events.leaguetasks.pack.LeagueTaskTriggerTable.EMOTE
import org.rsmod.content.events.leaguetasks.pack.LeagueTaskTriggerTable.LOC
import org.rsmod.content.events.leaguetasks.pack.LeagueTaskTriggerTable.NPC

private val VARLAMORE_CHILDREN =
    listOf(
        "npc.cam_torum_child_m_1",
        "npc.cam_torum_child_m_2",
        "npc.cam_torum_child_f_1",
        "npc.cam_torum_child_f_2",
        "npc.varlamore_child_poor_m_1",
        "npc.varlamore_child_poor_m_2",
        "npc.varlamore_child_poor_f_1",
        "npc.varlamore_child_poor_f_2",
        "npc.varlamore_child_normal_m_1",
        "npc.varlamore_child_normal_m_2",
        "npc.varlamore_child_normal_f_1",
        "npc.varlamore_child_normal_f_2",
        "npc.aldarin_child_f_1",
        "npc.aldarin_child_m_1",
        "npc.mistrock_child_m_1",
        "npc.mistrock_child_m_2",
        "npc.mistrock_child_f_1",
        "npc.mistrock_child_f_2",
        "npc.auburn_child_f_1",
        "npc.auburn_child_m_1",
        "npc.kastori_child_m_1",
        "npc.kastori_child_f_1",
        "npc.tal_teklan_child_m_1",
        "npc.tal_teklan_child_f_1",
    )

internal fun TriggerRows.emoteTasks() {
    emoteNearNpc(
        "league_task_bow_near_a_quetzal",
        "Bow",
        "npc.hg_quetzal",
        "npc.vmq2_quetzal_noname",
    )
    emoteNearNpc("league_task_cry_near_a_child", "Cry", *VARLAMORE_CHILDREN.toTypedArray())
    emoteNearNpc(
        "league_task_dance_near_a_bard",
        "Dance",
        "npc.avium_savannah_bard_conch",
        "npc.avium_savannah_bard_singer",
        "npc.avium_savannah_bard_trumpet",
        "npc.rainforest_troupe_singer_1",
        "npc.rainforest_troupe_singer_2",
        "npc.rainforest_troupe_singer_3",
    )
    emoteNearNpc("league_task_headbang_with_ketsal_kuk", "Headbang", "npc.molch_altar_shaman")
    emoteNearLoc(
        "league_task_salute_next_to_a_statue_of_quoatlos",
        "Salute",
        "loc.civitas_museum_statue_quoatlos",
        "loc.statues_civitas01_serpent01",
        "loc.statues_civitas01_serpent02",
        "loc.varlamore_statue01_quoatlos01_1x1",
        "loc.varlamore_statue01_quoatlos01_2x2",
        "loc.varlamore_statue01_quoatlos01_3x3",
        "loc.varlamore_statue01_quoatlos01_4x4",
        "loc.varlamore_statue01_quoatlos02_3x3",
    )
    emoteNearLoc(
        "league_task_sit_near_a_stolen_cabbage",
        "Sit down",
        "loc.avium_cabbage_dog01",
        "loc.avium_cabbage_dog02",
    )
}

private fun TriggerRows.emoteNearNpc(task: String, emote: String, vararg npcs: String) {
    trigger("dbrow.$task", LeagueTaskTrigger.Emote) {
        column(EMOTE, emote)
        columnRSCM(NPC, npcs.asList())
    }
}

private fun TriggerRows.emoteNearLoc(task: String, emote: String, vararg locs: String) {
    trigger("dbrow.$task", LeagueTaskTrigger.Emote) {
        column(EMOTE, emote)
        columnRSCM(LOC, locs.asList())
    }
}
