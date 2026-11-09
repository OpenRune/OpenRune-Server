package org.rsmod.content.events.leaguetasks.pack

private val LAW_TELEPORTS =
    arrayOf(
        "obj.25_varrock_teleport",
        "obj.31_lumbridge_teleport",
        "obj.37_falador_teleport",
        "obj.67_house_teleport",
        "obj.45_camelot_teleport",
        "obj.cert_deadman_level99_lamp",
        "obj.51_ardougne_teleport",
        "obj.placeholder_blighted_sack_snare",
        "obj.58_watchtower_teleport",
        "obj.61_trollheim_teleport",
        "obj.64_ape_atoll_teleport",
        "obj.54_paddewwa_teleport",
        "obj.60_senntisten_teleport",
        "obj.66_kharyllyl_teleport",
        "obj.72_lassar_teleport",
        "obj.78_dareeyak_teleport",
        "obj.84_carrallagar_teleport",
        "obj.90_annakarl_teleport",
        "obj.96_ghorrock_teleport",
        "obj.br_mithril_platebody",
        "obj.br_mithril_platelegs",
        "obj.23_teleport_battlefront",
        "obj.br_greendhide_body",
        "obj.poh_guide_guildtrophy",
        "obj.br_greendhide_chaps",
        "obj.br_moonclan_body",
        "obj.br_moonclan_legs",
        "obj.br_xeric_body",
        "obj.br_xeric_legs",
        "obj.br_air_staff",
        "obj.br_dragon_helm",
    )

/**
 * Several spells reuse odd obj symbols (`br_`, `placeholder_`, `cert_`) that the cache gives them;
 * these are the objs `SpellTeleportScript` casts.
 */
internal fun TriggerRows.magicTasks() {
    spell("league_task_teleport_using_law_runes", *LAW_TELEPORTS)
    spell("league_task_cast_paddewwa_teleport", "obj.54_paddewwa_teleport")
    spell("league_task_teleport_to_civitas_illa_fortis", "obj.placeholder_blighted_sack_snare")

    for (rifts in listOf(1, 10, 25)) {
        val suffix = if (rifts == 1) "rift_closed" else "rifts_closed"
        val task = "league_task_guardians_of_the_rift_${rifts}_$suffix"
        variable(task, "varp.total_gotr_kills", rifts)
    }
}
