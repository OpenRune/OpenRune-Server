package org.rsmod.content.events.leaguetasks.pack

private const val SMITHING = "stat.smithing"

/** Anvil-smithed rune items; dart tips, arrowtips, nails, bolts and javelin tips don't count. */
private val RUNE_ITEMS =
    arrayOf(
        "obj.rune_dagger",
        "obj.rune_axe",
        "obj.rune_mace",
        "obj.rune_med_helm",
        "obj.rune_sword",
        "obj.rune_scimitar",
        "obj.rune_longsword",
        "obj.rune_full_helm",
        "obj.rune_sq_shield",
        "obj.rune_warhammer",
        "obj.rune_battleaxe",
        "obj.rune_chainbody",
        "obj.rune_kiteshield",
        "obj.rune_claws",
        "obj.rune_2h_sword",
        "obj.rune_platelegs",
        "obj.rune_plateskirt",
        "obj.rune_platebody",
        "obj.rune_knife",
        "obj.xbows_crossbow_limbs_runite",
    )

internal fun TriggerRows.smithingTasks() {
    product("league_task_smelt_a_bronze_bar", SMITHING, "obj.bronze_bar")
    product("league_task_smelt_an_iron_bar", SMITHING, "obj.iron_bar")
    product("league_task_smelt_a_steel_bar", SMITHING, "obj.steel_bar")
    product("league_task_smelt_a_runite_bar", SMITHING, "obj.runite_bar")
    product(
        "league_task_smelt_a_steel_bar_in_port_phasmatys",
        SMITHING,
        "obj.steel_bar",
        areas = listOf("area.league_port_phasmatys"),
    )

    product(
        "league_task_smith_10_steel_bolts_unf",
        SMITHING,
        "obj.xbows_crossbow_bolts_steel_unfeathered",
        times = 10,
        counter = "varbit.league_counter_smith_steel_bolts",
    )
    product(
        "league_task_smith_150_iron_arrowtips",
        SMITHING,
        "obj.iron_arrowheads",
        times = 150,
        counter = "varbit.league_counter_smith_iron_arrowtips",
    )
    product(
        "league_task_smith_250_mithril_bolts_unf",
        SMITHING,
        "obj.xbows_crossbow_bolts_mithril_unfeathered",
        times = 250,
        counter = "varbit.league_task_smith_mithril_bolts",
    )
    product("action_smith_a_steel_2h_sword", SMITHING, "obj.steel_2h_sword")
    product(
        "league_task_smith_shayzien_1",
        SMITHING,
        "obj.shayzien_gloves_1",
        "obj.shayzien_boots_1",
        "obj.shayzien_helm_1",
        "obj.shayzien_legs_1",
        "obj.shayzien_body_1",
    )
    product("league_task_smith_a_rune_item", SMITHING, *RUNE_ITEMS)
    product(
        "league_task_smith_a_dragonfire_shield",
        SMITHING,
        "obj.dragonfire_shield",
        "obj.dragonfire_shield_uncharged",
    )
}
