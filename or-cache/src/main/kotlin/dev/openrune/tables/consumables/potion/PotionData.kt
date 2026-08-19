package dev.openrune.tables.consumables.potion

internal enum class PotionData(
    val key: String,
    val displayName: String,
    val items: List<String>,
    val empty: String,
    val effect: String,
    val category: String = "potion",
    val wildernessOnly: Boolean = false,
    val minigameOnly: String = "",
    val raidOnly: String = "",
    val mix: Boolean = false,
    val heal: Int = 0,
    val drinkDelay: Int = 3,
    val combatDelay: Int = 0,
) {
    // Standard potions.
    ATTACK_POTION(
        key = "attack_potion",
        displayName = "Attack potion",
        items =
            listOf("obj.4dose1attack", "obj.3dose1attack", "obj.2dose1attack", "obj.1dose1attack"),
        empty = "obj.vial_empty",
        effect = "dbrow.effect_attack_boost",
    ),
    ANTIPOISON(
        key = "antipoison",
        displayName = "Antipoison",
        items =
            listOf(
                "obj.4doseantipoison",
                "obj.3doseantipoison",
                "obj.2doseantipoison",
                "obj.1doseantipoison",
            ),
        empty = "obj.vial_empty",
        effect = "dbrow.effect_antipoison",
    ),
    STRENGTH_POTION(
        key = "strength_potion",
        displayName = "Strength potion",
        items =
            listOf(
                "obj.strength4",
                "obj.3dose1strength",
                "obj.2dose1strength",
                "obj.1dose1strength",
            ),
        empty = "obj.vial_empty",
        effect = "dbrow.effect_strength_boost",
    ),
    RESTORE_POTION(
        key = "restore_potion",
        displayName = "Restore potion",
        items =
            listOf(
                "obj.4dosestatrestore",
                "obj.3dosestatrestore",
                "obj.2dosestatrestore",
                "obj.1dosestatrestore",
            ),
        empty = "obj.vial_empty",
        effect = "dbrow.effect_restore_potion",
    ),
    ENERGY_POTION(
        key = "energy_potion",
        displayName = "Energy potion",
        items =
            listOf("obj.4dose1energy", "obj.3dose1energy", "obj.2dose1energy", "obj.1dose1energy"),
        empty = "obj.vial_empty",
        effect = "dbrow.effect_energy_restore",
    ),
    DEFENCE_POTION(
        key = "defence_potion",
        displayName = "Defence potion",
        items =
            listOf(
                "obj.4dose1defense",
                "obj.3dose1defense",
                "obj.2dose1defense",
                "obj.1dose1defense",
            ),
        empty = "obj.vial_empty",
        effect = "dbrow.effect_defence_boost",
    ),
    AGILITY_POTION(
        key = "agility_potion",
        displayName = "Agility potion",
        items =
            listOf(
                "obj.4dose1agility",
                "obj.3dose1agility",
                "obj.2dose1agility",
                "obj.1dose1agility",
            ),
        empty = "obj.vial_empty",
        effect = "dbrow.effect_agility_boost",
    ),
    COMBAT_POTION(
        key = "combat_potion",
        displayName = "Combat potion",
        items = listOf("obj.4dosecombat", "obj.3dosecombat", "obj.2dosecombat", "obj.1dosecombat"),
        empty = "obj.vial_empty",
        effect = "dbrow.effect_combat_boost",
    ),
    PRAYER_POTION(
        key = "prayer_potion",
        displayName = "Prayer potion",
        items =
            listOf(
                "obj.4doseprayerrestore",
                "obj.3doseprayerrestore",
                "obj.2doseprayerrestore",
                "obj.1doseprayerrestore",
            ),
        empty = "obj.vial_empty",
        effect = "dbrow.effect_prayer_restore",
    ),
    PRAYER_REGENERATION(
        key = "prayer_regeneration",
        displayName = "Prayer regeneration potion",
        items =
            listOf(
                "obj.4dose1prayer_regeneration",
                "obj.3dose1prayer_regeneration",
                "obj.2dose1prayer_regeneration",
                "obj.1dose1prayer_regeneration",
            ),
        empty = "obj.vial_empty",
        effect = "dbrow.effect_prayer_regeneration",
    ),
    SUPER_ATTACK(
        key = "super_attack",
        displayName = "Super attack",
        items =
            listOf("obj.4dose2attack", "obj.3dose2attack", "obj.2dose2attack", "obj.1dose2attack"),
        empty = "obj.vial_empty",
        effect = "dbrow.effect_super_attack_boost",
    ),
    SUPERANTIPOISON(
        key = "superantipoison",
        displayName = "Superantipoison",
        items =
            listOf(
                "obj.4dose2antipoison",
                "obj.3dose2antipoison",
                "obj.2dose2antipoison",
                "obj.1dose2antipoison",
            ),
        empty = "obj.vial_empty",
        effect = "dbrow.effect_superantipoison",
    ),
    FISHING_POTION(
        key = "fishing_potion",
        displayName = "Fishing potion",
        items =
            listOf(
                "obj.4dosefisherspotion",
                "obj.3dosefisherspotion",
                "obj.2dosefisherspotion",
                "obj.1dosefisherspotion",
            ),
        empty = "obj.vial_empty",
        effect = "dbrow.effect_fishing_boost",
    ),
    SUPER_ENERGY(
        key = "super_energy",
        displayName = "Super energy",
        items =
            listOf("obj.4dose2energy", "obj.3dose2energy", "obj.2dose2energy", "obj.1dose2energy"),
        empty = "obj.vial_empty",
        effect = "dbrow.effect_super_energy_restore",
    ),
    HUNTER_POTION(
        key = "hunter_potion",
        displayName = "Hunter potion",
        items =
            listOf("obj.4dosehunting", "obj.3dosehunting", "obj.2dosehunting", "obj.1dosehunting"),
        empty = "obj.vial_empty",
        effect = "dbrow.effect_hunter_boost",
    ),
    SUPER_STRENGTH(
        key = "super_strength",
        displayName = "Super strength",
        items =
            listOf(
                "obj.4dose2strength",
                "obj.3dose2strength",
                "obj.2dose2strength",
                "obj.1dose2strength",
            ),
        empty = "obj.vial_empty",
        effect = "dbrow.effect_super_strength_boost",
    ),
    SUPER_RESTORE(
        key = "super_restore",
        displayName = "Super restore",
        items =
            listOf(
                "obj.4dose2restore",
                "obj.3dose2restore",
                "obj.2dose2restore",
                "obj.1dose2restore",
            ),
        empty = "obj.vial_empty",
        effect = "dbrow.effect_super_restore",
    ),
    SUPER_DEFENCE(
        key = "super_defence",
        displayName = "Super defence",
        items =
            listOf(
                "obj.4dose2defense",
                "obj.3dose2defense",
                "obj.2dose2defense",
                "obj.1dose2defense",
            ),
        empty = "obj.vial_empty",
        effect = "dbrow.effect_super_defence_boost",
    ),
    ANTIFIRE(
        key = "antifire",
        displayName = "Antifire",
        items =
            listOf(
                "obj.4dose1antidragon",
                "obj.3dose1antidragon",
                "obj.2dose1antidragon",
                "obj.1dose1antidragon",
            ),
        empty = "obj.vial_empty",
        effect = "dbrow.effect_antifire",
    ),
    SUPER_ANTIFIRE(
        key = "super_antifire",
        displayName = "Super antifire",
        items =
            listOf(
                "obj.4dose2antidragon",
                "obj.3dose2antidragon",
                "obj.2dose2antidragon",
                "obj.1dose2antidragon",
            ),
        empty = "obj.vial_empty",
        effect = "dbrow.effect_super_antifire",
    ),
    RANGING_POTION(
        key = "ranging_potion",
        displayName = "Ranging potion",
        items =
            listOf(
                "obj.4doserangerspotion",
                "obj.3doserangerspotion",
                "obj.2doserangerspotion",
                "obj.1doserangerspotion",
            ),
        empty = "obj.vial_empty",
        effect = "dbrow.effect_ranging_boost",
    ),
    MAGIC_POTION(
        key = "magic_potion",
        displayName = "Magic potion",
        items = listOf("obj.4dose1magic", "obj.3dose1magic", "obj.2dose1magic", "obj.1dose1magic"),
        empty = "obj.vial_empty",
        effect = "dbrow.effect_magic_boost",
    ),
    ZAMORAK_BREW(
        key = "zamorak_brew",
        displayName = "Zamorak brew",
        items =
            listOf(
                "obj.4dosepotionofzamorak",
                "obj.3dosepotionofzamorak",
                "obj.2dosepotionofzamorak",
                "obj.1dosepotionofzamorak",
            ),
        empty = "obj.vial_empty",
        effect = "dbrow.effect_zamorak_brew",
    ),
    SARADOMIN_BREW(
        key = "saradomin_brew",
        displayName = "Saradomin brew",
        items =
            listOf(
                "obj.4dosepotionofsaradomin",
                "obj.3dosepotionofsaradomin",
                "obj.2dosepotionofsaradomin",
                "obj.1dosepotionofsaradomin",
            ),
        empty = "obj.vial_empty",
        effect = "dbrow.effect_saradomin_brew",
    ),
    BASTION_POTION(
        key = "bastion_potion",
        displayName = "Bastion potion",
        items =
            listOf("obj.4dosebastion", "obj.3dosebastion", "obj.2dosebastion", "obj.1dosebastion"),
        empty = "obj.vial_empty",
        effect = "dbrow.effect_bastion_boost",
    ),
    BATTLEMAGE_POTION(
        key = "battlemage_potion",
        displayName = "Battlemage potion",
        items =
            listOf(
                "obj.4dosebattlemage",
                "obj.3dosebattlemage",
                "obj.2dosebattlemage",
                "obj.1dosebattlemage",
            ),
        empty = "obj.vial_empty",
        effect = "dbrow.effect_battlemage_boost",
    ),
    SUPER_COMBAT(
        key = "super_combat",
        displayName = "Super combat potion",
        items =
            listOf("obj.4dose2combat", "obj.3dose2combat", "obj.2dose2combat", "obj.1dose2combat"),
        empty = "obj.vial_empty",
        effect = "dbrow.effect_super_combat_boost",
    ),
    STAMINA_POTION(
        key = "stamina_potion",
        displayName = "Stamina potion",
        items =
            listOf("obj.4dosestamina", "obj.3dosestamina", "obj.2dosestamina", "obj.1dosestamina"),
        empty = "obj.vial_empty",
        effect = "dbrow.effect_stamina",
    ),
    ANCIENT_BREW(
        key = "ancient_brew",
        displayName = "Ancient brew",
        items =
            listOf(
                "obj.4doseancientbrew",
                "obj.3doseancientbrew",
                "obj.2doseancientbrew",
                "obj.1doseancientbrew",
            ),
        empty = "obj.vial_empty",
        effect = "dbrow.effect_ancient_brew",
    ),
    ANTI_VENOM_PLUS(
        key = "anti_venom_plus",
        displayName = "Anti-venom+",
        items = listOf("obj.antivenom+4", "obj.antivenom+3", "obj.antivenom+2", "obj.antivenom+1"),
        empty = "obj.vial_empty",
        effect = "dbrow.effect_anti_venom_plus",
    ),
    EXTENDED_ANTI_VENOM_PLUS(
        key = "extended_anti_venom_plus",
        displayName = "Extended anti-venom+",
        items =
            listOf(
                "obj.extended_antivenom+4",
                "obj.extended_antivenom+3",
                "obj.extended_antivenom+2",
                "obj.extended_antivenom+1",
            ),
        empty = "obj.vial_empty",
        effect = "dbrow.effect_extended_anti_venom_plus",
    ),

    // Blighted potions.
    BLIGHTED_SUPER_RESTORE(
        key = "blighted_super_restore",
        displayName = "Blighted super restore",
        items =
            listOf(
                "obj.blighted_4dose2restore",
                "obj.blighted_3dose2restore",
                "obj.blighted_2dose2restore",
                "obj.blighted_1dose2restore",
            ),
        empty = "obj.vial_empty",
        effect = "dbrow.effect_super_restore",
        category = "blighted_potion",
        wildernessOnly = true,
    ),

    // Standard potions.
    SANFEW_SERUM(
        key = "sanfew_serum",
        displayName = "Sanfew serum",
        items =
            listOf(
                "obj.sanfew_salve_4_dose",
                "obj.sanfew_salve_3_dose",
                "obj.sanfew_salve_2_dose",
                "obj.sanfew_salve_1_dose",
            ),
        empty = "obj.vial_empty",
        effect = "dbrow.effect_sanfew_serum",
    ),
    SUPER_FISHING_POTION(
        key = "super_fishing_potion",
        displayName = "Super fishing potion",
        items =
            listOf(
                "obj.4dose2fisherspotion",
                "obj.3dose2fisherspotion",
                "obj.2dose2fisherspotion",
                "obj.1dose2fisherspotion",
            ),
        empty = "obj.vial_empty",
        effect = "dbrow.effect_super_fishing_boost",
    ),
    EXTREME_ENERGY_POTION(
        key = "extreme_energy_potion",
        displayName = "Extreme energy potion",
        items =
            listOf("obj.4dose3energy", "obj.3dose3energy", "obj.2dose3energy", "obj.1dose3energy"),
        empty = "obj.vial_empty",
        effect = "dbrow.effect_extreme_energy_restore",
    ),
    SUPER_HUNTER_POTION(
        key = "super_hunter_potion",
        displayName = "Super hunter potion",
        items =
            listOf(
                "obj.4dose2hunting",
                "obj.3dose2hunting",
                "obj.2dose2hunting",
                "obj.1dose2hunting",
            ),
        empty = "obj.vial_empty",
        effect = "dbrow.effect_super_hunter_boost",
    ),
    EXTENDED_STAMINA_POTION(
        key = "extended_stamina_potion",
        displayName = "Extended stamina potion",
        items =
            listOf(
                "obj.4dose2stamina",
                "obj.3dose2stamina",
                "obj.2dose2stamina",
                "obj.1dose2stamina",
            ),
        empty = "obj.vial_empty",
        effect = "dbrow.effect_extended_stamina",
    ),
    ARMADYL_BREW(
        key = "armadyl_brew",
        displayName = "Armadyl brew",
        items =
            listOf(
                "obj.4dosearmadylbrew",
                "obj.3dosearmadylbrew",
                "obj.2dosearmadylbrew",
                "obj.1dosearmadylbrew",
            ),
        empty = "obj.vial_empty",
        effect = "dbrow.effect_armadyl_brew",
    ),

    // Divine potions.
    DIVINE_SUPER_ATTACK(
        key = "divine_super_attack",
        displayName = "Divine super attack potion",
        items =
            listOf(
                "obj.4dosedivineattack",
                "obj.3dosedivineattack",
                "obj.2dosedivineattack",
                "obj.1dosedivineattack",
            ),
        empty = "obj.vial_empty",
        effect = "dbrow.effect_divine_super_attack_boost",
        category = "divine_potion",
    ),
    DIVINE_SUPER_STRENGTH(
        key = "divine_super_strength",
        displayName = "Divine super strength potion",
        items =
            listOf(
                "obj.4dosedivinestrength",
                "obj.3dosedivinestrength",
                "obj.2dosedivinestrength",
                "obj.1dosedivinestrength",
            ),
        empty = "obj.vial_empty",
        effect = "dbrow.effect_divine_super_strength_boost",
        category = "divine_potion",
    ),
    DIVINE_SUPER_DEFENCE(
        key = "divine_super_defence",
        displayName = "Divine super defence potion",
        items =
            listOf(
                "obj.4dosedivinedefence",
                "obj.3dosedivinedefence",
                "obj.2dosedivinedefence",
                "obj.1dosedivinedefence",
            ),
        empty = "obj.vial_empty",
        effect = "dbrow.effect_divine_super_defence_boost",
        category = "divine_potion",
    ),
    DIVINE_RANGING(
        key = "divine_ranging",
        displayName = "Divine ranging potion",
        items =
            listOf(
                "obj.4dosedivinerange",
                "obj.3dosedivinerange",
                "obj.2dosedivinerange",
                "obj.1dosedivinerange",
            ),
        empty = "obj.vial_empty",
        effect = "dbrow.effect_divine_ranging_boost",
        category = "divine_potion",
    ),
    DIVINE_MAGIC(
        key = "divine_magic",
        displayName = "Divine magic potion",
        items =
            listOf(
                "obj.4dosedivinemagic",
                "obj.3dosedivinemagic",
                "obj.2dosedivinemagic",
                "obj.1dosedivinemagic",
            ),
        empty = "obj.vial_empty",
        effect = "dbrow.effect_divine_magic_boost",
        category = "divine_potion",
    ),
    DIVINE_BASTION(
        key = "divine_bastion",
        displayName = "Divine bastion potion",
        items =
            listOf(
                "obj.4dosedivinebastion",
                "obj.3dosedivinebastion",
                "obj.2dosedivinebastion",
                "obj.1dosedivinebastion",
            ),
        empty = "obj.vial_empty",
        effect = "dbrow.effect_divine_bastion_boost",
        category = "divine_potion",
    ),
    DIVINE_BATTLEMAGE(
        key = "divine_battlemage",
        displayName = "Divine battlemage potion",
        items =
            listOf(
                "obj.4dosedivinebattlemage",
                "obj.3dosedivinebattlemage",
                "obj.2dosedivinebattlemage",
                "obj.1dosedivinebattlemage",
            ),
        empty = "obj.vial_empty",
        effect = "dbrow.effect_divine_battlemage_boost",
        category = "divine_potion",
    ),
    DIVINE_SUPER_COMBAT(
        key = "divine_super_combat",
        displayName = "Divine super combat potion",
        items =
            listOf(
                "obj.4dosedivinecombat",
                "obj.3dosedivinecombat",
                "obj.2dosedivinecombat",
                "obj.1dosedivinecombat",
            ),
        empty = "obj.vial_empty",
        effect = "dbrow.effect_divine_super_combat_boost",
        category = "divine_potion",
    ),

    // Barbarian mixes.
    ATTACK_MIX(
        key = "attack_mix",
        displayName = "Attack mix",
        items = listOf("obj.brutal_2dose1attack", "obj.brutal_1dose1attack"),
        empty = "obj.vial_empty",
        effect = "dbrow.effect_attack_boost",
        category = "barbarian_mix",
        mix = true,
        heal = 3,
    ),
    STRENGTH_MIX(
        key = "strength_mix",
        displayName = "Strength mix",
        items = listOf("obj.brutal_2dose1strength", "obj.brutal_1dose1strength"),
        empty = "obj.vial_empty",
        effect = "dbrow.effect_strength_boost",
        category = "barbarian_mix",
        mix = true,
        heal = 3,
    ),
    RESTORE_MIX(
        key = "restore_mix",
        displayName = "Restore mix",
        items = listOf("obj.brutal_2dosestatrestore", "obj.brutal_1dosestatrestore"),
        empty = "obj.vial_empty",
        effect = "dbrow.effect_restore_potion",
        category = "barbarian_mix",
        mix = true,
        heal = 3,
    ),
    ENERGY_MIX(
        key = "energy_mix",
        displayName = "Energy mix",
        items = listOf("obj.brutal_2dose1energy", "obj.brutal_1dose1energy"),
        empty = "obj.vial_empty",
        effect = "dbrow.effect_energy_restore",
        category = "barbarian_mix",
        mix = true,
        heal = 3,
    ),
    DEFENCE_MIX(
        key = "defence_mix",
        displayName = "Defence mix",
        items = listOf("obj.brutal_2dose1defense", "obj.brutal_1dose1defense"),
        empty = "obj.vial_empty",
        effect = "dbrow.effect_defence_boost",
        category = "barbarian_mix",
        mix = true,
        heal = 6,
    ),
    AGILITY_MIX(
        key = "agility_mix",
        displayName = "Agility mix",
        items = listOf("obj.brutal_2dose1agility", "obj.brutal_1dose1agility"),
        empty = "obj.vial_empty",
        effect = "dbrow.effect_agility_boost",
        category = "barbarian_mix",
        mix = true,
        heal = 6,
    ),
    PRAYER_MIX(
        key = "prayer_mix",
        displayName = "Prayer mix",
        items = listOf("obj.brutal_2doseprayerrestore", "obj.brutal_1doseprayerrestore"),
        empty = "obj.vial_empty",
        effect = "dbrow.effect_prayer_restore",
        category = "barbarian_mix",
        mix = true,
        heal = 6,
    ),
    SUPER_ATTACK_MIX(
        key = "super_attack_mix",
        displayName = "Super attack mix",
        items = listOf("obj.brutal_2dose2attack", "obj.brutal_1dose2attack"),
        empty = "obj.vial_empty",
        effect = "dbrow.effect_super_attack_boost",
        category = "barbarian_mix",
        mix = true,
        heal = 6,
    ),
    ANTIPOISON_MIX(
        key = "antipoison_mix",
        displayName = "Antipoison mix",
        items = listOf("obj.brutal_2doseantipoison", "obj.brutal_1doseantipoison"),
        empty = "obj.vial_empty",
        effect = "dbrow.effect_antipoison",
        category = "barbarian_mix",
        mix = true,
        heal = 3,
    ),
    COMBAT_MIX(
        key = "combat_mix",
        displayName = "Combat mix",
        items = listOf("obj.brutal_2dosecombat", "obj.brutal_1dosecombat"),
        empty = "obj.vial_empty",
        effect = "dbrow.effect_combat_boost",
        category = "barbarian_mix",
        mix = true,
        heal = 3,
    ),
    HUNTING_MIX(
        key = "hunting_mix",
        displayName = "Hunting mix",
        items = listOf("obj.brutal_2dose1hunting", "obj.brutal_1dose1hunting"),
        empty = "obj.vial_empty",
        effect = "dbrow.effect_hunter_boost",
        category = "barbarian_mix",
        mix = true,
        heal = 6,
    ),
    ANCIENT_MIX(
        key = "ancient_mix",
        displayName = "Ancient mix",
        items = listOf("obj.brutal_2doseancientbrew", "obj.brutal_1doseancientbrew"),
        empty = "obj.vial_empty",
        effect = "dbrow.effect_ancient_brew",
        category = "barbarian_mix",
        mix = true,
        heal = 6,
    ),
    SUPERANTIPOISON_MIX(
        key = "superantipoison_mix",
        displayName = "Superantipoison mix",
        items = listOf("obj.brutal_2dose2antipoison", "obj.brutal_1dose2antipoison"),
        empty = "obj.vial_empty",
        effect = "dbrow.effect_superantipoison",
        category = "barbarian_mix",
        mix = true,
        heal = 6,
    ),
    FISHING_MIX(
        key = "fishing_mix",
        displayName = "Fishing mix",
        items = listOf("obj.brutal_2dosefisherspotion", "obj.brutal_1dosefisherspotion"),
        empty = "obj.vial_empty",
        effect = "dbrow.effect_fishing_boost",
        category = "barbarian_mix",
        mix = true,
        heal = 6,
    ),
    SUPER_ENERGY_MIX(
        key = "super_energy_mix",
        displayName = "Super energy mix",
        items = listOf("obj.brutal_2dose2energy", "obj.brutal_1dose2energy"),
        empty = "obj.vial_empty",
        effect = "dbrow.effect_super_energy_restore",
        category = "barbarian_mix",
        mix = true,
        heal = 6,
    ),
    SUPER_STRENGTH_MIX(
        key = "super_strength_mix",
        displayName = "Super strength mix",
        items = listOf("obj.brutal_2dose2strength", "obj.brutal_1dose2strength"),
        empty = "obj.vial_empty",
        effect = "dbrow.effect_super_strength_boost",
        category = "barbarian_mix",
        mix = true,
        heal = 6,
    ),
    SUPER_RESTORE_MIX(
        key = "super_restore_mix",
        displayName = "Super restore mix",
        items = listOf("obj.brutal_2dose2restore", "obj.brutal_1dose2restore"),
        empty = "obj.vial_empty",
        effect = "dbrow.effect_super_restore",
        category = "barbarian_mix",
        mix = true,
        heal = 6,
    ),
    SUPER_DEFENCE_MIX(
        key = "super_defence_mix",
        displayName = "Super defence mix",
        items = listOf("obj.brutal_2dose2defense", "obj.brutal_1dose2defense"),
        empty = "obj.vial_empty",
        effect = "dbrow.effect_super_defence_boost",
        category = "barbarian_mix",
        mix = true,
        heal = 6,
    ),
    ANTIFIRE_MIX(
        key = "antifire_mix",
        displayName = "Antifire mix",
        items = listOf("obj.brutal_2dose1antidragon", "obj.brutal_1dose1antidragon"),
        empty = "obj.vial_empty",
        effect = "dbrow.effect_antifire",
        category = "barbarian_mix",
        mix = true,
        heal = 6,
    ),
    EXTENDED_ANTIFIRE_MIX(
        key = "extended_antifire_mix",
        displayName = "Extended antifire mix",
        items = listOf("obj.brutal_2dose2antidragon", "obj.brutal_1dose2antidragon"),
        empty = "obj.vial_empty",
        effect = "dbrow.effect_extended_antifire",
        category = "barbarian_mix",
        mix = true,
        heal = 6,
    ),
    SUPER_ANTIFIRE_MIX(
        key = "super_antifire_mix",
        displayName = "Super antifire mix",
        items = listOf("obj.brutal_2dose3antidragon", "obj.brutal_1dose3antidragon"),
        empty = "obj.vial_empty",
        effect = "dbrow.effect_super_antifire",
        category = "barbarian_mix",
        mix = true,
        heal = 6,
    ),
    EXTENDED_SUPER_ANTIFIRE_MIX(
        key = "extended_super_antifire_mix",
        displayName = "Extended super antifire mix",
        items = listOf("obj.brutal_2dose4antidragon", "obj.brutal_1dose4antidragon"),
        empty = "obj.vial_empty",
        effect = "dbrow.effect_extended_super_antifire",
        category = "barbarian_mix",
        mix = true,
        heal = 6,
    ),
    RANGING_MIX(
        key = "ranging_mix",
        displayName = "Ranging mix",
        items = listOf("obj.brutal_2doserangerspotion", "obj.brutal_1doserangerspotion"),
        empty = "obj.vial_empty",
        effect = "dbrow.effect_ranging_boost",
        category = "barbarian_mix",
        mix = true,
        heal = 6,
    ),
    MAGIC_MIX(
        key = "magic_mix",
        displayName = "Magic mix",
        items = listOf("obj.brutal_2dose1magic", "obj.brutal_1dose1magic"),
        empty = "obj.vial_empty",
        effect = "dbrow.effect_magic_boost",
        category = "barbarian_mix",
        mix = true,
        heal = 6,
    ),
    ZAMORAK_MIX(
        key = "zamorak_mix",
        displayName = "Zamorak mix",
        items = listOf("obj.brutal_2dosepotionofzamorak", "obj.brutal_1dosepotionofzamorak"),
        empty = "obj.vial_empty",
        effect = "dbrow.effect_zamorak_brew",
        category = "barbarian_mix",
        mix = true,
        heal = 6,
    ),

    // Standard potions.
    ANTI_VENOM(
        key = "anti_venom",
        displayName = "Anti-venom",
        items = listOf("obj.antivenom4", "obj.antivenom3", "obj.antivenom2", "obj.antivenom1"),
        empty = "obj.vial_empty",
        effect = "dbrow.effect_anti_venom",
    ),
    EXTENDED_ANTIFIRE(
        key = "extended_antifire",
        displayName = "Extended antifire",
        items =
            listOf(
                "obj.4dose4antidragon",
                "obj.3dose4antidragon",
                "obj.2dose4antidragon",
                "obj.1dose4antidragon",
            ),
        empty = "obj.vial_empty",
        effect = "dbrow.effect_extended_antifire",
    ),
    EXTENDED_SUPER_ANTIFIRE(
        key = "extended_super_antifire",
        displayName = "Extended super antifire",
        items =
            listOf(
                "obj.4dose3antidragon",
                "obj.3dose3antidragon",
                "obj.2dose3antidragon",
                "obj.1dose3antidragon",
            ),
        empty = "obj.vial_empty",
        effect = "dbrow.effect_extended_super_antifire",
    ),
    FORGOTTEN_BREW(
        key = "forgotten_brew",
        displayName = "Forgotten brew",
        items =
            listOf(
                "obj.4doseforgottenbrew",
                "obj.3doseforgottenbrew",
                "obj.2doseforgottenbrew",
                "obj.1doseforgottenbrew",
            ),
        empty = "obj.vial_empty",
        effect = "dbrow.effect_forgotten_brew",
    ),
    MENAPHITE_REMEDY(
        key = "menaphite_remedy",
        displayName = "Menaphite remedy",
        items =
            listOf(
                "obj.4dosestatrenewal",
                "obj.3dosestatrenewal",
                "obj.2dosestatrenewal",
                "obj.1dosestatrenewal",
            ),
        empty = "obj.vial_empty",
        effect = "dbrow.effect_menaphite_remedy",
    ),

    // Teas and remedies.
    GUTHIX_REST(
        key = "guthix_rest",
        displayName = "Guthix rest",
        items =
            listOf(
                "obj.cup_guthix_rest_4",
                "obj.cup_guthix_rest_3",
                "obj.cup_guthix_rest_2",
                "obj.cup_guthix_rest_1",
            ),
        empty = "obj.cup_empty",
        effect = "dbrow.effect_guthix_rest",
        category = "tea",
    ),

    // Nightmare Zone potions.
    NZONE_OVERLOAD(
        key = "nzone_overload",
        displayName = "Overload",
        items =
            listOf(
                "obj.nzone4doseoverloadpotion",
                "obj.nzone3doseoverloadpotion",
                "obj.nzone2doseoverloadpotion",
                "obj.nzone1doseoverloadpotion",
            ),
        empty = "obj.vial_empty",
        effect = "dbrow.effect_nzone_overload",
        category = "nightmare_zone_potion",
        minigameOnly = "nightmare_zone",
    ),
    NZONE_ABSORPTION(
        key = "nzone_absorption",
        displayName = "Absorption",
        items =
            listOf(
                "obj.nzone4doseabsorptionpotion",
                "obj.nzone3doseabsorptionpotion",
                "obj.nzone2doseabsorptionpotion",
                "obj.nzone1doseabsorptionpotion",
            ),
        empty = "obj.vial_empty",
        effect = "dbrow.effect_nzone_absorption",
        category = "nightmare_zone_potion",
        minigameOnly = "nightmare_zone",
    ),
    NZONE_SUPER_RANGING(
        key = "nzone_super_ranging",
        displayName = "Super ranging potion",
        items =
            listOf(
                "obj.nzone4dose2rangerspotion",
                "obj.nzone3dose2rangerspotion",
                "obj.nzone2dose2rangerspotion",
                "obj.nzone1dose2rangerspotion",
            ),
        empty = "obj.vial_empty",
        effect = "dbrow.effect_nzone_super_ranging",
        category = "nightmare_zone_potion",
        minigameOnly = "nightmare_zone",
    ),
    NZONE_SUPER_MAGIC(
        key = "nzone_super_magic",
        displayName = "Super magic potion",
        items =
            listOf(
                "obj.nzone4dose2magicpotion",
                "obj.nzone3dose2magicpotion",
                "obj.nzone2dose2magicpotion",
                "obj.nzone1dose2magicpotion",
            ),
        empty = "obj.vial_empty",
        effect = "dbrow.effect_nzone_super_magic",
        category = "nightmare_zone_potion",
        minigameOnly = "nightmare_zone",
    ),

    // Chambers of Xeric potions.
    COX_XERICS_AID_WEAK(
        key = "cox_xerics_aid_weak",
        displayName = "Xeric's aid (-)",
        items =
            listOf(
                "obj.raids_vial_xericaid_weak_4",
                "obj.raids_vial_xericaid_weak_3",
                "obj.raids_vial_xericaid_weak_2",
                "obj.raids_vial_xericaid_weak_1",
            ),
        empty = "obj.vial_empty",
        effect = "dbrow.effect_cox_xerics_aid_weak",
        category = "cox_potion",
        raidOnly = "chambers_of_xeric",
    ),
    COX_XERICS_AID(
        key = "cox_xerics_aid",
        displayName = "Xeric's aid",
        items =
            listOf(
                "obj.raids_vial_xericaid_4",
                "obj.raids_vial_xericaid_3",
                "obj.raids_vial_xericaid_2",
                "obj.raids_vial_xericaid_1",
            ),
        empty = "obj.vial_empty",
        effect = "dbrow.effect_cox_xerics_aid",
        category = "cox_potion",
        raidOnly = "chambers_of_xeric",
    ),
    COX_XERICS_AID_STRONG(
        key = "cox_xerics_aid_strong",
        displayName = "Xeric's aid (+)",
        items =
            listOf(
                "obj.raids_vial_xericaid_strong_4",
                "obj.raids_vial_xericaid_strong_3",
                "obj.raids_vial_xericaid_strong_2",
                "obj.raids_vial_xericaid_strong_1",
            ),
        empty = "obj.vial_empty",
        effect = "dbrow.effect_cox_xerics_aid_strong",
        category = "cox_potion",
        raidOnly = "chambers_of_xeric",
    ),
    COX_REVITALISATION_WEAK(
        key = "cox_revitalisation_weak",
        displayName = "Revitalisation (-)",
        items =
            listOf(
                "obj.raids_vial_revitalisation_weak_4",
                "obj.raids_vial_revitalisation_weak_3",
                "obj.raids_vial_revitalisation_weak_2",
                "obj.raids_vial_revitalisation_weak_1",
            ),
        empty = "obj.vial_empty",
        effect = "dbrow.effect_cox_revitalisation_weak",
        category = "cox_potion",
        raidOnly = "chambers_of_xeric",
    ),
    COX_REVITALISATION(
        key = "cox_revitalisation",
        displayName = "Revitalisation",
        items =
            listOf(
                "obj.raids_vial_revitalisation_4",
                "obj.raids_vial_revitalisation_3",
                "obj.raids_vial_revitalisation_2",
                "obj.raids_vial_revitalisation_1",
            ),
        empty = "obj.vial_empty",
        effect = "dbrow.effect_cox_revitalisation",
        category = "cox_potion",
        raidOnly = "chambers_of_xeric",
    ),
    COX_REVITALISATION_STRONG(
        key = "cox_revitalisation_strong",
        displayName = "Revitalisation (+)",
        items =
            listOf(
                "obj.raids_vial_revitalisation_strong_4",
                "obj.raids_vial_revitalisation_strong_3",
                "obj.raids_vial_revitalisation_strong_2",
                "obj.raids_vial_revitalisation_strong_1",
            ),
        empty = "obj.vial_empty",
        effect = "dbrow.effect_cox_revitalisation_strong",
        category = "cox_potion",
        raidOnly = "chambers_of_xeric",
    ),
    COX_PRAYER_ENHANCE_WEAK(
        key = "cox_prayer_enhance_weak",
        displayName = "Prayer enhance (-)",
        items =
            listOf(
                "obj.raids_vial_prayer_weak_4",
                "obj.raids_vial_prayer_weak_3",
                "obj.raids_vial_prayer_weak_2",
                "obj.raids_vial_prayer_weak_1",
            ),
        empty = "obj.vial_empty",
        effect = "dbrow.effect_cox_prayer_enhance_weak",
        category = "cox_potion",
        raidOnly = "chambers_of_xeric",
    ),
    COX_PRAYER_ENHANCE(
        key = "cox_prayer_enhance",
        displayName = "Prayer enhance",
        items =
            listOf(
                "obj.raids_vial_prayer_4",
                "obj.raids_vial_prayer_3",
                "obj.raids_vial_prayer_2",
                "obj.raids_vial_prayer_1",
            ),
        empty = "obj.vial_empty",
        effect = "dbrow.effect_cox_prayer_enhance",
        category = "cox_potion",
        raidOnly = "chambers_of_xeric",
    ),
    COX_PRAYER_ENHANCE_STRONG(
        key = "cox_prayer_enhance_strong",
        displayName = "Prayer enhance (+)",
        items =
            listOf(
                "obj.raids_vial_prayer_strong_4",
                "obj.raids_vial_prayer_strong_3",
                "obj.raids_vial_prayer_strong_2",
                "obj.raids_vial_prayer_strong_1",
            ),
        empty = "obj.vial_empty",
        effect = "dbrow.effect_cox_prayer_enhance_strong",
        category = "cox_potion",
        raidOnly = "chambers_of_xeric",
    ),
    COX_OVERLOAD_WEAK(
        key = "cox_overload_weak",
        displayName = "Overload (-)",
        items =
            listOf(
                "obj.raids_vial_overload_weak_4",
                "obj.raids_vial_overload_weak_3",
                "obj.raids_vial_overload_weak_2",
                "obj.raids_vial_overload_weak_1",
            ),
        empty = "obj.vial_empty",
        effect = "dbrow.effect_cox_overload_weak",
        category = "cox_potion",
        raidOnly = "chambers_of_xeric",
    ),
    COX_OVERLOAD(
        key = "cox_overload",
        displayName = "Overload",
        items =
            listOf(
                "obj.raids_vial_overload_4",
                "obj.raids_vial_overload_3",
                "obj.raids_vial_overload_2",
                "obj.raids_vial_overload_1",
            ),
        empty = "obj.vial_empty",
        effect = "dbrow.effect_cox_overload",
        category = "cox_potion",
        raidOnly = "chambers_of_xeric",
    ),
    COX_OVERLOAD_STRONG(
        key = "cox_overload_strong",
        displayName = "Overload (+)",
        items =
            listOf(
                "obj.raids_vial_overload_strong_4",
                "obj.raids_vial_overload_strong_3",
                "obj.raids_vial_overload_strong_2",
                "obj.raids_vial_overload_strong_1",
            ),
        empty = "obj.vial_empty",
        effect = "dbrow.effect_cox_overload_strong",
        category = "cox_potion",
        raidOnly = "chambers_of_xeric",
    ),
    COX_ELDER_POTION_WEAK(
        key = "cox_elder_potion_weak",
        displayName = "Elder potion (-)",
        items =
            listOf(
                "obj.raids_vial_elder_weak_4",
                "obj.raids_vial_elder_weak_3",
                "obj.raids_vial_elder_weak_2",
                "obj.raids_vial_elder_weak_1",
            ),
        empty = "obj.vial_empty",
        effect = "dbrow.effect_cox_elder_potion_weak",
        category = "cox_potion",
        raidOnly = "chambers_of_xeric",
    ),
    COX_ELDER_POTION(
        key = "cox_elder_potion",
        displayName = "Elder potion",
        items =
            listOf(
                "obj.raids_vial_elder_4",
                "obj.raids_vial_elder_3",
                "obj.raids_vial_elder_2",
                "obj.raids_vial_elder_1",
            ),
        empty = "obj.vial_empty",
        effect = "dbrow.effect_cox_elder_potion",
        category = "cox_potion",
        raidOnly = "chambers_of_xeric",
    ),
    COX_ELDER_POTION_STRONG(
        key = "cox_elder_potion_strong",
        displayName = "Elder potion (+)",
        items =
            listOf(
                "obj.raids_vial_elder_strong_4",
                "obj.raids_vial_elder_strong_3",
                "obj.raids_vial_elder_strong_2",
                "obj.raids_vial_elder_strong_1",
            ),
        empty = "obj.vial_empty",
        effect = "dbrow.effect_cox_elder_potion_strong",
        category = "cox_potion",
        raidOnly = "chambers_of_xeric",
    ),
    COX_KODAI_POTION_WEAK(
        key = "cox_kodai_potion_weak",
        displayName = "Kodai potion (-)",
        items =
            listOf(
                "obj.raids_vial_kodai_weak_4",
                "obj.raids_vial_kodai_weak_3",
                "obj.raids_vial_kodai_weak_2",
                "obj.raids_vial_kodai_weak_1",
            ),
        empty = "obj.vial_empty",
        effect = "dbrow.effect_cox_kodai_potion_weak",
        category = "cox_potion",
        raidOnly = "chambers_of_xeric",
    ),
    COX_KODAI_POTION(
        key = "cox_kodai_potion",
        displayName = "Kodai potion",
        items =
            listOf(
                "obj.raids_vial_kodai_4",
                "obj.raids_vial_kodai_3",
                "obj.raids_vial_kodai_2",
                "obj.raids_vial_kodai_1",
            ),
        empty = "obj.vial_empty",
        effect = "dbrow.effect_cox_kodai_potion",
        category = "cox_potion",
        raidOnly = "chambers_of_xeric",
    ),
    COX_KODAI_POTION_STRONG(
        key = "cox_kodai_potion_strong",
        displayName = "Kodai potion (+)",
        items =
            listOf(
                "obj.raids_vial_kodai_strong_4",
                "obj.raids_vial_kodai_strong_3",
                "obj.raids_vial_kodai_strong_2",
                "obj.raids_vial_kodai_strong_1",
            ),
        empty = "obj.vial_empty",
        effect = "dbrow.effect_cox_kodai_potion_strong",
        category = "cox_potion",
        raidOnly = "chambers_of_xeric",
    ),
    COX_TWISTED_POTION_WEAK(
        key = "cox_twisted_potion_weak",
        displayName = "Twisted potion (-)",
        items =
            listOf(
                "obj.raids_vial_twisted_weak_4",
                "obj.raids_vial_twisted_weak_3",
                "obj.raids_vial_twisted_weak_2",
                "obj.raids_vial_twisted_weak_1",
            ),
        empty = "obj.vial_empty",
        effect = "dbrow.effect_cox_twisted_potion_weak",
        category = "cox_potion",
        raidOnly = "chambers_of_xeric",
    ),
    COX_TWISTED_POTION(
        key = "cox_twisted_potion",
        displayName = "Twisted potion",
        items =
            listOf(
                "obj.raids_vial_twisted_4",
                "obj.raids_vial_twisted_3",
                "obj.raids_vial_twisted_2",
                "obj.raids_vial_twisted_1",
            ),
        empty = "obj.vial_empty",
        effect = "dbrow.effect_cox_twisted_potion",
        category = "cox_potion",
        raidOnly = "chambers_of_xeric",
    ),
    COX_TWISTED_POTION_STRONG(
        key = "cox_twisted_potion_strong",
        displayName = "Twisted potion (+)",
        items =
            listOf(
                "obj.raids_vial_twisted_strong_4",
                "obj.raids_vial_twisted_strong_3",
                "obj.raids_vial_twisted_strong_2",
                "obj.raids_vial_twisted_strong_1",
            ),
        empty = "obj.vial_empty",
        effect = "dbrow.effect_cox_twisted_potion_strong",
        category = "cox_potion",
        raidOnly = "chambers_of_xeric",
    ),

    // Tombs of Amascut supplies.
    TOA_NECTAR(
        key = "toa_nectar",
        displayName = "Nectar",
        items =
            listOf(
                "obj.toa_supply_heal_4",
                "obj.toa_supply_heal_3",
                "obj.toa_supply_heal_2",
                "obj.toa_supply_heal_1",
            ),
        empty = "obj.vial_empty",
        effect = "dbrow.effect_toa_nectar",
        category = "toa_supply",
        raidOnly = "tombs_of_amascut",
    ),
    TOA_AMBROSIA(
        key = "toa_ambrosia",
        displayName = "Ambrosia",
        items = listOf("obj.toa_supply_panicheal_2", "obj.toa_supply_panicheal_1"),
        empty = "obj.vial_empty",
        effect = "dbrow.effect_toa_ambrosia",
        category = "toa_supply",
        raidOnly = "tombs_of_amascut",
    ),
    TOA_TEARS_OF_ELIDINIS(
        key = "toa_tears_of_elidinis",
        displayName = "Tears of Elidinis",
        items =
            listOf(
                "obj.toa_supply_prayer_4",
                "obj.toa_supply_prayer_3",
                "obj.toa_supply_prayer_2",
                "obj.toa_supply_prayer_1",
            ),
        empty = "obj.vial_empty",
        effect = "dbrow.effect_toa_tears_of_elidinis",
        category = "toa_supply",
        raidOnly = "tombs_of_amascut",
    ),
    TOA_LIQUID_ADRENALINE(
        key = "toa_liquid_adrenaline",
        displayName = "Liquid adrenaline",
        items = listOf("obj.toa_supply_energy_2", "obj.toa_supply_energy_1"),
        empty = "obj.vial_empty",
        effect = "dbrow.effect_toa_liquid_adrenaline",
        category = "toa_supply",
        raidOnly = "tombs_of_amascut",
    ),
    TOA_SMELLING_SALTS(
        key = "toa_smelling_salts",
        displayName = "Smelling salts",
        items = listOf("obj.toa_supply_stats_2", "obj.toa_supply_stats_1"),
        empty = "obj.vial_empty",
        effect = "dbrow.effect_toa_smelling_salts",
        category = "toa_supply",
        raidOnly = "tombs_of_amascut",
    ),
    TOA_SILK_DRESSING(
        key = "toa_silk_dressing",
        displayName = "Silk dressing",
        items = listOf("obj.toa_supply_heal_overtime_2", "obj.toa_supply_heal_overtime_1"),
        empty = "obj.vial_empty",
        effect = "dbrow.effect_toa_silk_dressing",
        category = "toa_supply",
        raidOnly = "tombs_of_amascut",
    ),
    TOA_BLESSED_CRYSTAL_SCARAB(
        key = "toa_blessed_crystal_scarab",
        displayName = "Blessed crystal scarab",
        items = listOf("obj.toa_supply_prayer_overtime_2", "obj.toa_supply_prayer_overtime_1"),
        empty = "obj.vial_empty",
        effect = "dbrow.effect_toa_blessed_crystal_scarab",
        category = "toa_supply",
        raidOnly = "tombs_of_amascut",
    ),

    // Standard potions.
    ANTIDOTE_PLUS(
        key = "antidote_plus",
        displayName = "Antidote+",
        items = listOf("obj.antidote+4", "obj.antidote+3", "obj.antidote+2", "obj.antidote+1"),
        empty = "obj.vial_empty",
        effect = "dbrow.effect_antidote_plus",
    ),
    ANTIDOTE_PLUS_PLUS(
        key = "antidote_plus_plus",
        displayName = "Antidote++",
        items = listOf("obj.antidote++4", "obj.antidote++3", "obj.antidote++2", "obj.antidote++1"),
        empty = "obj.vial_empty",
        effect = "dbrow.effect_antidote_plus_plus",
    ),

    // Barbarian mixes.
    ANTIDOTE_PLUS_MIX(
        key = "antidote_plus_mix",
        displayName = "Antidote+ mix",
        items = listOf("obj.brutal_antidote+2", "obj.brutal_antidote+1"),
        empty = "obj.vial_empty",
        effect = "dbrow.effect_antidote_plus",
        category = "barbarian_mix",
        mix = true,
        heal = 6,
    ),
    STAMINA_MIX(
        key = "stamina_mix",
        displayName = "Stamina mix",
        items = listOf("obj.brutal_2dosestamina", "obj.brutal_1dosestamina"),
        empty = "obj.vial_empty",
        effect = "dbrow.effect_stamina",
        category = "barbarian_mix",
        mix = true,
        heal = 6,
    ),

    // Standard potions.
    RELICYMS_BALM(
        key = "relicyms_balm",
        displayName = "Relicym's balm",
        items =
            listOf(
                "obj.relicyms_balm4",
                "obj.relicyms_balm3",
                "obj.relicyms_balm2",
                "obj.relicyms_balm1",
            ),
        empty = "obj.vial_empty",
        effect = "dbrow.effect_relicyms_balm",
    ),

    // Barbarian mixes.
    RELICYMS_MIX(
        key = "relicyms_mix",
        displayName = "Relicym's mix",
        items = listOf("obj.brutal_relicyms_balm2", "obj.brutal_relicyms_balm1"),
        empty = "obj.vial_empty",
        effect = "dbrow.effect_relicyms_balm",
        category = "barbarian_mix",
        mix = true,
        heal = 3,
    ),

    // Standard potions.
    MOONLIGHT_POTION(
        key = "moonlight_potion",
        displayName = "Moonlight potion",
        items =
            listOf(
                "obj.4dosemoonlightpotion",
                "obj.3dosemoonlightpotion",
                "obj.2dosemoonlightpotion",
                "obj.1dosemoonlightpotion",
            ),
        empty = "obj.vial_empty",
        effect = "dbrow.effect_moonlight_potion",
        minigameOnly = "moons_of_peril",
    );

    val row: String
        get() = "dbrow.$key"
}
