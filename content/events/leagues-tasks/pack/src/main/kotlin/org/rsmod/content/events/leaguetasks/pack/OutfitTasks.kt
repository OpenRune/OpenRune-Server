package org.rsmod.content.events.leaguetasks.pack

/** Chompy hats share one name; `cbhat1` to `cbhat18` follow Rantz's kill tiers in order. */
internal fun TriggerRows.outfitTasks() {
    equip("league_task_equip_a_marksman_chompy_hat", "obj.cbhat6")
    equip("league_task_equip_an_ogre_forester_chompy_hat", "obj.cbhat9")
    equip("league_task_equip_an_ogre_expert_chompy_hat", "obj.cbhat13")
    equip("league_task_equip_a_dragon_archer_chompy_hat", "obj.cbhat16")

    equip(
        "league_task_equip_a_monkey_backpack",
        "obj.mm2_monkey",
        "obj.mm2_monkey_karamja",
        "obj.mm2_monkey_maniacal",
        "obj.mm2_monkey_awowogei",
        "obj.mm2_monkey_skeleton",
        "obj.mm2_monkey_zombie",
    )
    equip("league_task_equip_a_karamja_monkey_backpack", "obj.mm2_monkey_karamja")
    equip("league_task_equip_a_maniacal_monkey_backpack", "obj.mm2_monkey_maniacal")

    equip(
        "league_task_equip_a_twohanded_axe",
        "obj.bronze_axe_2h",
        "obj.iron_axe_2h",
        "obj.steel_axe_2h",
        "obj.black_axe_2h",
        "obj.mithril_axe_2h",
        "obj.adamant_axe_2h",
        "obj.rune_axe_2h",
        "obj.dragon_axe_2h",
        "obj.3a_axe_2h",
        "obj.crystal_axe_2h",
    )

    equip(
        "league_task_equip_a_full_set_of_the_smiths_outfit",
        "obj.smithing_uniform_torso",
        "obj.smithing_uniform_legs",
        "obj.smithing_uniform_boots",
        "obj.smithing_uniform_gloves",
        "obj.smithing_uniform_gloves_ice",
        worn = 4,
    )
    equip(
        "league_task_equip_the_pyromancers_garb",
        "obj.pyromancer_hood",
        "obj.pyromancer_top",
        "obj.pyromancer_bottom",
        "obj.pyromancer_boots",
        worn = 4,
    )
    equip(
        "league_task_equip_full_oathplate",
        "obj.oathplate_helm",
        "obj.oathplate_chest",
        "obj.oathplate_legs",
        worn = 3,
    )

    equip(
        "league_task_equip_a_corrupted_weapon",
        "obj.blade_of_saeldor_infinite",
        "obj.blade_of_saeldor_infinite_ithell",
        "obj.blade_of_saeldor_infinite_iorwerth",
        "obj.blade_of_saeldor_infinite_trahaearn",
        "obj.blade_of_saeldor_infinite_cadarn",
        "obj.blade_of_saeldor_infinite_crwys",
        "obj.blade_of_saeldor_infinite_meilyr",
        "obj.blade_of_saeldor_infinite_amlodd",
        "obj.bow_of_faerdhinen_infinite",
        "obj.bow_of_faerdhinen_infinite_ithell",
        "obj.bow_of_faerdhinen_infinite_iorwerth",
        "obj.bow_of_faerdhinen_infinite_trahaearn",
        "obj.bow_of_faerdhinen_infinite_cadarn",
        "obj.bow_of_faerdhinen_infinite_crwys",
        "obj.bow_of_faerdhinen_infinite_meilyr",
        "obj.bow_of_faerdhinen_infinite_amlodd",
    )
    equip(
        "league_task_equip_some_primordial_pegasian_or_eternal_boots",
        "obj.primordial_boots",
        "obj.pegasian_boots",
        "obj.eternal_boots",
        "obj.avernic_treads_melee",
        "obj.avernic_treads_ranged",
        "obj.avernic_treads_magic",
        "obj.avernic_treads_melee_ranged",
        "obj.avernic_treads_melee_magic",
        "obj.avernic_treads_ranged_magic",
        "obj.avernic_treads_max",
    )
    equip(
        "league_task_equip_a_gilded_or_trimmed_wizard_item",
        "obj.rune_full_helm_goldplate",
        "obj.rune_platebody_goldplate",
        "obj.rune_platelegs_goldplate",
        "obj.rune_plateskirt_goldplate",
        "obj.rune_kiteshield_goldplate",
        "obj.rune_med_helm_gold",
        "obj.rune_chainbody_gold",
        "obj.rune_sq_shield_gold",
        "obj.rune_boots_gold",
        "obj.trail_gilded_dhide_coif",
        "obj.trail_gilded_dhide_top",
        "obj.trail_gilded_dhide_chaps",
        "obj.trail_gilded_dhide_vambraces",
        "obj.bluewizhat_trim",
        "obj.wizards_robe_trim",
        "obj.blue_skirt_trim",
        "obj.blackwizhat_trim",
        "obj.black_wizards_robe_trim",
        "obj.black_skirt_trim",
    )
}
