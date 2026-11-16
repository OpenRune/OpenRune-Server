package org.rsmod.content.quest.area.varlamore.perilousmoons

import dev.openrune.types.MesAnimType
import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.player.vars.boolVarBit
import org.rsmod.api.player.vars.intVarBit
import org.rsmod.game.entity.Player
import org.rsmod.map.CoordGrid

internal object PmoonStage {
    const val NOT_STARTED = 0
    const val KILL_NAGUA = 2
    const val NAGUA_KILLED = 4
    const val CITY_OPEN = 5
    const val IN_CITY = 6
    const val RUINS_OPEN = 7
    const val MURALS = 8
    const val BUILD_CAMPS = 10
    const val CAMPS_BUILT = 12
    const val INSCRIPTION_READ = 13
    const val TALISMANS_SHOWN = 14
    const val TALISMANS = 15
    const val ENCHANTED = 16
    const val INFUSED = 17
    const val EYATLALLI_FOUND = 18
    const val EYATLALLI_GONE = 19
    const val ANTECHAMBER_SHAKEN = 20
    const val ZUMA_BLAMED = 21
    const val ZUMA_BLAMED_AGAIN = 22
    const val GATHER = 23
    const val SCALES_PLACED = 24
    const val TAIL_PLACED = 25
    const val PASTE_PLACED = 26
    const val RITUAL_STARTED = 27
    const val FIGHT_MOONS = 28
    const val ECLIPSE_SEEN = 29
    const val JESSAMINE_DONE = 30
    const val ZUMA_DONE = 31
    const val COMPLETE = 36

    /** Neypotzli is a private copy from the first entry until the talismans are enchanted. */
    val INSTANCED = MURALS until ENCHANTED
}

internal object PmoonNpc {
    const val ATTALA_GATES = "npc.pmoon_attala_gates"
    const val ZUMA_GATES = "npc.pmoon_zuma_gates"
    const val BUILDERS_GATES = "npc.pmoon_digsite_workers_plank"
    const val GUARD = "npc.pmoon_quest_guard"
    const val NAGUA = "npc.pmoon_quest_nagua"
    const val ATTALA_CITY = "npc.pmoon_attala_camtorum"
    const val ZUMA_CITY = "npc.pmoon_zuma_camtorum"
    const val JESSAMINE_CITY = "npc.pmoon_jessica_camtorum"
    const val ATTALA_RUINS = "npc.pmoon_attala_dungeon"
    const val ZUMA_RUINS = "npc.pmoon_zuma_dungeon"
    const val JESSAMINE_RUINS = "npc.pmoon_jessica_dungeon"
    const val BUILDERS_RUINS = "npc.pmoon_digsite_workers_noplank"
    const val EYATLALLI = "npc.pmoon_eyatlalli"
    const val EYATLALLI_DISCOVERED = "npc.pmoon_eyatlalli_discovered"
    const val NAHTA = "npc.cam_torum_shop_magic"
    const val BLACKSMITH = "npc.cam_torum_shop_blacksmith"
    const val BLACKSMITH_CUTSCENE = "npc.cam_torum_shop_blacksmith_cutscene"
    const val ZUMA_CUTSCENE = "npc.pmoon_zuma_cutscene"
    const val ECLIPSE_STAND_IN = "npc.pmoon_boss_eclipse_clone"

    const val ATTALA_HEAD = "npc.pmoon_attala_vis"
    const val ZUMA_HEAD = "npc.pmoon_zuma_vis"
    const val JESSAMINE_HEAD = "npc.pmoon_jessica_vis"
    const val EYATLALLI_HEAD = "npc.pmoon_eyatlalli_vis"
    const val GUARD_HEAD = "npc.pmoon_quest_guard_vis"
    const val BUILDER_1_HEAD = "npc.pmoon_builder1_chathead_spoof"
    const val BUILDER_2_HEAD = "npc.pmoon_builder2_chathead_spoof"
}

internal object PmoonObj {
    const val EARTH_TALISMAN = "obj.earth_talisman"
    const val WATER_TALISMAN = "obj.water_talisman"
    const val ENCHANTED_EARTH = "obj.enchanted_earth_talisman"
    const val ENCHANTED_WATER = "obj.enchanted_water_talisman"
    const val INFUSED_EARTH = "obj.infused_earth_talisman"
    const val INFUSED_WATER = "obj.infused_water_talisman"
    const val BUILDING_SUPPLIES = "obj.pmoon_camp_supplies"
    const val HAMMER = "obj.hammer"
    const val SAW = "obj.poh_saw"
    const val SCALES = "obj.pmoon_fish_scales"
    const val LIZARD_TAIL = "obj.pmoon_lizard_tail"
    const val GRUB_PASTE = "obj.moonlight_grub_paste"
    const val ICOSAHEDRON = "obj.pmoon_icosahedron"
}

internal object PmoonPlace {
    val RUINS_ENTRANCE = CoordGrid(1439, 9599, 1)
    val ATTALA_MURAL = CoordGrid(1433, 9632, 1)
    val ZUMA_MURAL = CoordGrid(1447, 9632, 1)
    val ATTALA_REST = CoordGrid(1439, 9638, 1)
    val ZUMA_REST = CoordGrid(1441, 9638, 1)
    val JESSAMINE = CoordGrid(1440, 9639, 1)
    val BUILDERS = CoordGrid(1432, 9636, 1)
}

internal var Player.pmoonStage by intVarBit("varbit.pmoon_quest")
internal var Player.pmoonMurals by intVarBit("varbit.pmoon_murals_inspected")
internal var Player.pmoonCamp1 by boolVarBit("varbit.pmoon_camp_1")
internal var Player.pmoonCamp2 by boolVarBit("varbit.pmoon_camp_2")
internal var Player.pmoonCamp3 by boolVarBit("varbit.pmoon_camp_3")
internal var Player.pmoonBriefed by boolVarBit("varbit.pmoon_ruin_dialogue")
internal var Player.pmoonTalismanReturn by boolVarBit("varbit.pmoon_talisman_return_dialogue")
internal var Player.pmoonEyatlalliVis by boolVarBit("varbit.pmoon_eyatlalli_vis")
internal var Player.pmoonJeering by intVarBit("varbit.pmoon_quest_dwarf_jeering")
internal var Player.pmoonJeeringCooldown by intVarBit("varbit.pmoon_quest_dwarf_jeering_cooldown")

internal val Player.pmoonCampsBuilt: Int
    get() = listOf(pmoonCamp1, pmoonCamp2, pmoonCamp3).count { it }

internal val Player.pmoonMoonsSubdued: Boolean
    get() =
        vars["varbit.pmoon_boss_blood_dead"] != 0 &&
            vars["varbit.pmoon_boss_blue_dead"] != 0 &&
            vars["varbit.pmoon_boss_eclipse_dead"] != 0

internal suspend fun Dialogue.attala(anim: MesAnimType, text: String) =
    chatNpcSpecific("Attala", PmoonNpc.ATTALA_HEAD, anim, text)

internal suspend fun Dialogue.zuma(anim: MesAnimType, text: String) =
    chatNpcSpecific("Zuma", PmoonNpc.ZUMA_HEAD, anim, text)

internal suspend fun Dialogue.jessamine(anim: MesAnimType, text: String) =
    chatNpcSpecific("Jessamine", PmoonNpc.JESSAMINE_HEAD, anim, text)

internal suspend fun Dialogue.eyatlalli(anim: MesAnimType, text: String) =
    chatNpcSpecific("Eyatlalli", PmoonNpc.EYATLALLI_HEAD, anim, text)

internal suspend fun Dialogue.guard(anim: MesAnimType, text: String) =
    chatNpcSpecific("Guard", PmoonNpc.GUARD_HEAD, anim, text)

internal suspend fun Dialogue.builder1(anim: MesAnimType, text: String) =
    chatNpcSpecific("Builder 1", PmoonNpc.BUILDER_1_HEAD, anim, text)

internal suspend fun Dialogue.builder2(anim: MesAnimType, text: String) =
    chatNpcSpecific("Builder 2", PmoonNpc.BUILDER_2_HEAD, anim, text)

internal suspend fun Dialogue.nahta(anim: MesAnimType, text: String) =
    chatNpcSpecific("Nahta", PmoonNpc.NAHTA, anim, text)

internal suspend fun Dialogue.blacksmith(anim: MesAnimType, text: String) =
    chatNpcSpecific("Blacksmith", PmoonNpc.BLACKSMITH, anim, text)
