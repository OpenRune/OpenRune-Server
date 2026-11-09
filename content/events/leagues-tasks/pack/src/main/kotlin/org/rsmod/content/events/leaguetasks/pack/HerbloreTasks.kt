package org.rsmod.content.events.leaguetasks.pack

private const val HERBLORE = "stat.herblore"

private fun doses(patternWithDoseHash: String): Array<String> =
    (1..4).map { "obj." + patternWithDoseHash.replace("#", it.toString()) }.toTypedArray()

internal fun TriggerRows.herbloreTasks() {
    product("league_task_clean_a_grimy_guam", HERBLORE, "obj.guam_leaf")
    product("league_task_clean_a_grimy_avantoe", HERBLORE, "obj.avantoe")
    cleaned("league_task_clean_15_grimy_tarromin", "obj.tarromin", 15, "tarromin")
    cleaned("league_task_clean_25_grimy_guam_leafs", "obj.guam_leaf", 25, "guam_leafs")
    cleaned("league_task_clean_50_grimy_cadantine", "obj.cadantine", 50, "cadantine")
    cleaned("league_task_clean_50_grimy_ranarr_weed", "obj.ranarr_weed", 50, "ranarr_weed")
    cleaned("league_task_clean_100_grimy_avantoe", "obj.avantoe", 100, "avantoe")
    cleaned("league_task_clean_50_grimy_lantadyme", "obj.lantadyme", 50, "lantadyme")

    product("league_task_create_an_antipoison", HERBLORE, *doses("#doseantipoison"))
    product("league_task_make_a_combat_potion", HERBLORE, *doses("#dosecombat"))
    product("league_task_make_a_saradomin_brew", HERBLORE, *doses("#dosepotionofsaradomin"))
    product("league_task_make_a_super_combat_potion", HERBLORE, *doses("#dose2combat"))
    product("league_task_make_an_extended_antifire_potion", HERBLORE, *doses("#dose2antidragon"))

    made(
        "league_task_make_20_stamina_potions",
        doses("#dosestamina"),
        20,
        "league_counter_make_stamina_potions",
    )
    made(
        "league_task_make_30_prayer_potions",
        doses("#doseprayerrestore"),
        30,
        "league_counter_make_prayer_potions",
    )
    made(
        "league_task_make_50_ancient_brews",
        doses("#doseancientbrew"),
        50,
        "league_counter_make_ancient_brews",
    )
    made(
        "league_task_make_20_ranging_potions",
        doses("#doserangerspotion"),
        20,
        "league_counter_make_ranging_potions",
    )
    made(
        "league_task_make_20_magic_potions",
        doses("#dose1magic"),
        20,
        "league_counter_make_magic_potions",
    )
    made(
        "league_task_make_50_menaphite_remedies",
        doses("#dosestatrenewal"),
        50,
        "league_counter_make_menaphite_remedies",
    )
    made(
        "league_task_make_50_forgotten_brews",
        doses("#doseforgottenbrew"),
        50,
        "league_counter_make_forgotten_brews",
    )
    made(
        "league_task_make_100_prayer_regeneration_potions",
        doses("#dose1prayer_regeneration"),
        100,
        "league_task_make_prayer_regeneration_potions",
    )
    made(
        "league_task_make_50_goading_potions",
        doses("#dosegoading"),
        50,
        "league_task_make_goading_potions",
    )
    made(
        "league_task_make_100_antivenom_potions",
        doses("antivenom#"),
        100,
        "league_task_create_antivenom_potions",
    )
    made(
        "league_task_make_50_antivenom+_potions",
        doses("antivenom+#"),
        50,
        "league_task_create_antivenom_plus_potions",
    )
    made(
        "league_task_create_25_divine_super_attack_potions",
        doses("#dosedivineattack"),
        25,
        "league_task_create_divine_super_attack_potions",
    )
    made(
        "league_task_create_25_divine_super_strength_potions",
        doses("#dosedivinestrength"),
        25,
        "league_task_create_divine_super_strength_potions",
    )
    made(
        "league_task_create_100_divine_ranging_potions",
        doses("#dosedivinerange"),
        100,
        "league_counter_make_divine_ranging_potions",
    )
    made(
        "league_task_create_100_divine_super_combat_potions",
        doses("#dosedivinecombat"),
        100,
        "league_counter_make_divine_super_combat_potions",
    )

    equip(
        "league_task_equip_a_piece_of_alchemists_outfit",
        "obj.mm_alchemist_body",
        "obj.mm_alchemist_body_alt",
        "obj.mm_alchemist_legs",
        "obj.mm_alchemist_legs_alt",
        "obj.mm_alchemist_gloves",
    )
    equip(
        "league_task_equip_full_alchemists_outfit",
        "obj.mm_alchemist_body",
        "obj.mm_alchemist_body_alt",
        "obj.mm_alchemist_legs",
        "obj.mm_alchemist_legs_alt",
        "obj.mm_alchemist_gloves",
        worn = 3,
    )
}

private fun TriggerRows.cleaned(task: String, herb: String, times: Int, grimy: String) {
    product(
        task,
        HERBLORE,
        herb,
        times = times,
        counter = "varbit.league_task_clean_grimy_${grimy}",
    )
}

private fun TriggerRows.made(task: String, potions: Array<String>, times: Int, counter: String) {
    product(task, HERBLORE, *potions, times = times, counter = "varbit.$counter")
}
