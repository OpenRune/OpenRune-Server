@file:Suppress("SpellCheckingInspection", "unused")

package org.rsmod.api.config.refs

import dev.openrune.varp

typealias varps = BaseVarps

object BaseVarps {
    /*
     * These "generic" temporary-state varps are used across multiple interfaces to track temporary
     * state. Unlike varps tied to a specific piece of content with child varbits, these are more
     * general-purpose.
     */
    val if1 = varp("if1")
    val if2 = varp("if2")
    val if3 = varp("if3")

    val canoeing_menu = varp("canoeing_menu")

    val com_mode = varp("com_mode")
    // Note: This varp seems to only be transmitted while wielding melee weapons and correlates to
    // Controlled (0), Accurate (1), Aggressive (2), and Defensive (3).
    // Though it may be the case that it actually represents the current "XP" type being granted.
    val com_stance = varp("com_stance")
    val option_nodef = varp("option_nodef")
    val sa_energy = varp("sa_energy")
    val sa_attack = varp("sa_attack")
    val soulreaper_souls = varp("soulreaper_stacks")

    val option_run = varp("option_run")
    val option_attackpriority = varp("option_attackpriority")
    val option_attackpriority_npc = varp("option_attackpriority_npc")

    val option_master_volume = varp("option_master_volume")
    val option_music = varp("option_music")
    val option_sounds = varp("option_sounds")
    val option_areasounds = varp("option_areasounds")

    val musicplay = varp("musicplay")
    val settings_tracking = varp("settings_tracking")
    val chat_filter_assist = varp("chat_filter_assist")

    val cookquest = varp("cookquest")
    val doricquest = varp("doricquest")
    val haunted = varp("haunted")
    val runemysteries = varp("runemysteries")
    val hetty = varp("hetty")
    val hunt = varp("hunt")
    val rjquest = varp("rjquest")
    val imp = varp("imp")
    val dragonquest = varp("dragonquest")
    val vampire = varp("vampire")
    val sheep = varp("sheep")

    val colosseum_glory = varp("colosseum_glory")

    val collection_count_other_max = varp("collection_count_other_max")
    val collection_count_other = varp("collection_count_other")
    val collection_count_minigames_max = varp("collection_count_minigames_max")
    val collection_count_minigames = varp("collection_count_minigames")
    val collection_count_clues_max = varp("collection_count_clues_max")
    val collection_count_clues = varp("collection_count_clues")
    val collection_count_raids_max = varp("collection_count_raids_max")
    val collection_count_raids = varp("collection_count_raids")
    val collection_count_bosses_max = varp("collection_count_bosses_max")
    val collection_count_bosses = varp("collection_count_bosses")
    val collection_count_max = varp("collection_count_max")
    val collection_count = varp("collection_count")

    val map_clock = varp("map_clock")
    val date_vars = varp("date_vars")

    /* Server-side-only types */
    val music_playlist = varp("music_playlist")
    val music_temp_state_3 = varp("music_temp_state_3")
    val music_temp_state_2 = varp("music_temp_state_2")
    val music_temp_state_1 = varp("music_temp_state_1")
    val gameframe = varp("gameframe")
    val pk_prey1 = varp("pk_prey1")
    val pk_prey2 = varp("pk_prey2")
    val pk_predator1 = varp("pk_predator1")
    val pk_predator2 = varp("pk_predator2")
    val pk_predator3 = varp("pk_predator3")
    val prayer_drain = varp("prayer_drain")
    val prayer0 = varp("prayer0")
    val playtime = varp("playtime")
    val generic_temp_state_65516 = varp("generic_temp_state_65516")
    val dinhs_passive_delay = varp("dinhs_passive_delay")
    val com_maxhit = varp("com_maxhit")
    val forinthry_surge_expiration = varp("forinthry_surge_expiration")
    val saved_autocast_state_staff = varp("saved_autocast_state_staff")
    val saved_autocast_state_bladed_staff = varp("saved_autocast_state_bladed_staff")
    val lastcombat = varp("lastcombat")
    val lastcombat_pvp = varp("lastcombat_pvp")
    val aggressive_npc = varp("aggressive_npc")
    val generic_temp_coords_65529 = varp("generic_temp_coords_65529")
    val inv_capacity_65530 = varp("inv_capacity_65530")
    val generic_storage_65531 = varp("generic_storage_65531")

    /*
     * "Restore" varps serve as temporary storage for varps that are modified temporarily and need
     * to be restored later.
     */
    val temp_restore_65527 = varp("temp_restore_65527")

    val venom_strikes = varp("venom_strikes")
    val disease = varp("disease")
    val disease_drain = varp("disease_drain")
    val poison_severity = varp("poison_severity")
    val poison = varp("poison")
}
