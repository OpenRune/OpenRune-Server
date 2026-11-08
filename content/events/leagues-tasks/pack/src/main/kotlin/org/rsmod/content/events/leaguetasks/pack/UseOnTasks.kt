package org.rsmod.content.events.leaguetasks.pack

import org.rsmod.content.events.leaguetasks.pack.LeagueTaskTriggerTable.LOC
import org.rsmod.content.events.leaguetasks.pack.LeagueTaskTriggerTable.NPC
import org.rsmod.content.events.leaguetasks.pack.LeagueTaskTriggerTable.OBJ

private val EMPTY_WATER_CONTAINERS =
    listOf(
        "obj.bucket_empty",
        "obj.jug_empty",
        "obj.bowl_empty",
        "obj.vial_empty",
        "obj.cup_empty",
        "obj.fishbowl_empty",
        "obj.watering_can_0",
        "obj.watering_can_1",
        "obj.watering_can_2",
        "obj.watering_can_3",
        "obj.watering_can_4",
        "obj.watering_can_5",
        "obj.watering_can_6",
        "obj.watering_can_7",
        "obj.zeah_wateringcan",
        "obj.water_skin0",
        "obj.water_skin1",
        "obj.water_skin2",
        "obj.water_skin3",
    )

/**
 * Every bone a dog eats, from `dbtable.skill_prayer`. Shaikahan bones are left out: a dog refuses
 * them.
 */
private val DOG_BONES =
    listOf(
        "obj.bones",
        "obj.bones_burnt",
        "obj.wolf_bones",
        "obj.bat_bones",
        "obj.big_bones",
        "obj.tbwt_jogre_bones",
        "obj.babywyrm_bones",
        "obj.zogre_bones",
        "obj.babydragon_bones",
        "obj.wyrm_bones",
        "obj.wyvern_bones",
        "obj.dragon_bones",
        "obj.dragon_bones_superior",
        "obj.drake_bones",
        "obj.lava_dragon_bones",
        "obj.hydra_bones",
        "obj.dagannoth_king_bones",
        "obj.zogre_ancestral_bones_fayg",
        "obj.zogre_ancestral_bones_raurg",
        "obj.zogre_ancestral_bones_ourg",
        "obj.alan_bones",
        "obj.mm_normal_monkey_bones",
        "obj.mm_skeleton_bones",
        "obj.mm_small_zombie_monkey_bones",
        "obj.mm_large_zombie_monkey_bones",
        "obj.mm_small_ninja_monkey_bones",
        "obj.mm_medium_ninja_monkey_bones",
        "obj.mm_normal_gorilla_monkey_bones",
        "obj.mm_bearded_gorilla_monkey_bones",
        "obj.shade_bleached_bones",
    )

internal fun TriggerRows.useOnTasks() {
    useOnLoc(
        "league_task_fill_something_up_from_a_water_pump",
        listOf("loc.fortis_water_pump"),
        EMPTY_WATER_CONTAINERS,
    )
    useOnNpc(
        "league_task_feed_a_dog_some_bones",
        listOf(
            "npc.dog_stray",
            "npc.dog_stray2",
            "npc.varlamore_dog_chiribaya_1",
            "npc.varlamore_dog_chiribaya_2",
            "npc.varlamore_dog_chiribaya_1_indoors",
        ),
        DOG_BONES,
    )
    useOnNpc(
        "league_task_feed_a_ghast_some_sweets",
        listOf(
            "npc.ghast_vis",
            "npc.templetrek_ghast_vis_1",
            "npc.templetrek_ghast_vis_2",
            "npc.templetrek_ghast_vis_3",
        ),
        listOf("obj.trail_sweets"),
    )
}

private fun TriggerRows.useOnLoc(task: String, locs: List<String>, objs: List<String>) {
    trigger("dbrow.$task", LeagueTaskTrigger.UseOn) {
        columnRSCM(LOC, locs)
        columnRSCM(OBJ, objs)
    }
}

private fun TriggerRows.useOnNpc(
    task: String,
    npcs: List<String>,
    objs: List<String> = emptyList(),
) {
    trigger("dbrow.$task", LeagueTaskTrigger.UseOn) {
        columnRSCM(NPC, npcs)
        if (objs.isNotEmpty()) {
            columnRSCM(OBJ, objs)
        }
    }
}
