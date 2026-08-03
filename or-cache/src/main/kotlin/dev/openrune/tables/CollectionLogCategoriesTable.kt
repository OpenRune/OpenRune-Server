package dev.openrune.tables

import dev.openrune.definition.dbtables.dbTable
import dev.openrune.definition.util.VarType

object CollectionLogCategoriesTable {

    const val NAME = 0
    const val COMPLETED_VARBIT = 1
    const val ITEMS = 2
    const val CATEGORY = 3
    const val COUNT_VARP_1 = 4
    const val COUNT_VARP_2 = 5
    const val COUNT_VARP_3 = 6
    const val PB_VARP_1 = 7
    const val PB_VARP_2 = 8
    const val TAB_INDEX = 9

    fun collectionLogCategories() = dbTable("dbtable.collection_log_categories", serverOnly = true) {
        column("name", NAME, VarType.STRING)
        column("completed_varbit", COMPLETED_VARBIT, VarType.INT)
        column("items", ITEMS, VarType.OBJ)
        column("category", CATEGORY, VarType.STRING)
        column("count_varp_1", COUNT_VARP_1, VarType.INT)
        column("count_varp_2", COUNT_VARP_2, VarType.INT)
        column("count_varp_3", COUNT_VARP_3, VarType.INT)
        column("pb_varp_1", PB_VARP_1, VarType.INT)
        column("pb_varp_2", PB_VARP_2, VarType.INT)
        column("tab_index", TAB_INDEX, VarType.INT)

        row("dbrow.collection_log_category_abyssal_sire") {
            column(NAME, "Abyssal Sire")
            columnRSCM(COMPLETED_VARBIT, "varbit.collection_bosses_sire_completed")
            columnRSCM(ITEMS, "obj.abyssalsire_pet", "obj.abyssalsire_unsired", "obj.poh_trophydrop_abyssaldemon", "obj.abyssal_bludgeon_1", "obj.abyssal_bludgeon_2", "obj.abyssal_bludgeon_3", "obj.jar_of_miasma", "obj.abyssal_dagger", "obj.abyssal_whip")
            column(CATEGORY, "Bosses")
            columnRSCM(COUNT_VARP_1, "varp.total_abyssalsire_kills")
            column(TAB_INDEX, 0)
        }

        row("dbrow.collection_log_category_alchemical_hydra") {
            column(NAME, "Alchemical Hydra")
            columnRSCM(COMPLETED_VARBIT, "varbit.collection_bosses_hydra_boss_completed")
            columnRSCM(ITEMS, "obj.hydrapet", "obj.hydra_claw", "obj.hydra_tail", "obj.hydra_leather", "obj.hydra_fang", "obj.hydra_eye", "obj.hydra_heart", "obj.dragon_knife", "obj.dragon_thrownaxe", "obj.jar_of_chemicals", "obj.poh_alchemical_hydra_head")
            column(CATEGORY, "Bosses")
            columnRSCM(COUNT_VARP_1, "varp.total_hydraboss_kills")
            column(TAB_INDEX, 1)
        }

        row("dbrow.collection_log_category_amoxliatl") {
            column(NAME, "Amoxliatl")
            columnRSCM(COMPLETED_VARBIT, "varbit.collection_bosses_amoxliatl_completed")
            columnRSCM(ITEMS, "obj.amoxliatlpet", "obj.glacial_temotli", "obj.pendant_of_ates_empty", "obj.frozen_tear")
            column(CATEGORY, "Bosses")
            columnRSCM(COUNT_VARP_1, "varp.total_amoxliatl_kills")
            column(TAB_INDEX, 2)
        }

        row("dbrow.collection_log_category_araxxor") {
            column(NAME, "Araxxor")
            columnRSCM(COMPLETED_VARBIT, "varbit.collection_bosses_araxxor_completed")
            columnRSCM(ITEMS, "obj.araxxorpet", "obj.araxyte_venom_sack", "obj.teleportscroll_spidercave", "obj.araxyte_fang", "obj.noxious_halberd_part_1", "obj.noxious_halberd_part_2", "obj.noxious_halberd_part_3", "obj.poh_araxyte_head", "obj.jar_of_venom", "obj.araxxor_pet_morph")
            column(CATEGORY, "Bosses")
            columnRSCM(COUNT_VARP_1, "varp.total_araxxor_kills")
            column(TAB_INDEX, 3)
        }

        row("dbrow.collection_log_category_barrows_chests") {
            column(NAME, "Barrows Chests")
            columnRSCM(COMPLETED_VARBIT, "varbit.collection_bosses_barrows_completed")
            columnRSCM(ITEMS, "obj.barrows_karil_head", "obj.barrows_ahrim_head", "obj.barrows_dharok_head", "obj.barrows_guthan_head", "obj.barrows_torag_head", "obj.barrows_verac_head", "obj.barrows_karil_body", "obj.barrows_ahrim_body", "obj.barrows_dharok_body", "obj.barrows_guthan_body", "obj.barrows_torag_body", "obj.barrows_verac_body", "obj.barrows_karil_legs", "obj.barrows_ahrim_legs", "obj.barrows_dharok_legs", "obj.barrows_guthan_legs", "obj.barrows_torag_legs", "obj.barrows_verac_legs", "obj.barrows_karil_weapon", "obj.barrows_ahrim_weapon", "obj.barrows_dharok_weapon", "obj.barrows_guthan_weapon", "obj.barrows_torag_weapon", "obj.barrows_verac_weapon", "obj.barrows_karil_ammo")
            column(CATEGORY, "Bosses")
            columnRSCM(COUNT_VARP_1, "varp.total_barrows_chests")
            column(TAB_INDEX, 4)
        }

        row("dbrow.collection_log_category_brutus") {
            column(NAME, "Brutus")
            columnRSCM(COMPLETED_VARBIT, "varbit.collection_bosses_cowboss_completed")
            columnRSCM(ITEMS, "obj.cowbosspet", "obj.mooleta", "obj.bottomless_milk_bucket", "obj.cow_slippers")
            column(CATEGORY, "Bosses")
            columnRSCM(COUNT_VARP_1, "varp.total_cowboss_kills")
            column(TAB_INDEX, 5)
        }

        row("dbrow.collection_log_category_bryophyta") {
            column(NAME, "Bryophyta")
            columnRSCM(COMPLETED_VARBIT, "varbit.collection_bosses_bryophyta_completed")
            columnRSCM(ITEMS, "obj.gb_moss_essence")
            column(CATEGORY, "Bosses")
            columnRSCM(COUNT_VARP_1, "varp.total_bryophyta_kills")
            column(TAB_INDEX, 6)
        }

        row("dbrow.collection_log_category_callisto_and_artio") {
            column(NAME, "Callisto and Artio")
            columnRSCM(COMPLETED_VARBIT, "varbit.collection_bosses_callisto_completed")
            columnRSCM(ITEMS, "obj.callisto_pet", "obj.heavy_ring", "obj.dragon_pickaxe", "obj.dragon_2h_sword", "obj.wbr_callisto_claws", "obj.wbr_voidwaker_hilt")
            column(CATEGORY, "Bosses")
            columnRSCM(COUNT_VARP_1, "varp.total_artio_kills")
            columnRSCM(COUNT_VARP_2, "varp.total_callisto_kills")
            column(TAB_INDEX, 7)
        }

        row("dbrow.collection_log_category_cerberus") {
            column(NAME, "Cerberus")
            columnRSCM(COMPLETED_VARBIT, "varbit.collection_bosses_cerberus_completed")
            columnRSCM(ITEMS, "obj.hell_pet", "obj.eternal_crystal", "obj.pegasian_crystal", "obj.primordial_crystal", "obj.jar_of_souls", "obj.smouldering_stone", "obj.teleportscroll_cerberus")
            column(CATEGORY, "Bosses")
            columnRSCM(COUNT_VARP_1, "varp.total_cerberus_kills")
            column(TAB_INDEX, 8)
        }

        row("dbrow.collection_log_category_chaos_elemental") {
            column(NAME, "Chaos Elemental")
            columnRSCM(COMPLETED_VARBIT, "varbit.collection_bosses_elemental_completed")
            columnRSCM(ITEMS, "obj.chaoselepet", "obj.dragon_pickaxe", "obj.dragon_2h_sword")
            column(CATEGORY, "Bosses")
            columnRSCM(COUNT_VARP_1, "varp.total_chaosele_kills")
            column(TAB_INDEX, 9)
        }

        row("dbrow.collection_log_category_chaos_fanatic") {
            column(NAME, "Chaos Fanatic")
            columnRSCM(COMPLETED_VARBIT, "varbit.collection_bosses_fanatic_completed")
            columnRSCM(ITEMS, "obj.chaoselepet", "obj.odium_shard1", "obj.malediction_shard1")
            column(CATEGORY, "Bosses")
            columnRSCM(COUNT_VARP_1, "varp.total_chaosfanatic_kills")
            column(TAB_INDEX, 10)
        }

        row("dbrow.collection_log_category_commander_zilyana") {
            column(NAME, "Commander Zilyana")
            columnRSCM(COMPLETED_VARBIT, "varbit.collection_bosses_saradomin_completed")
            columnRSCM(ITEMS, "obj.saradominpet", "obj.acb", "obj.godwars_godsword_hilt_saradomin", "obj.saradomin_sword", "obj.saradomin_light", "obj.godwars_godsword_blade1", "obj.godwars_godsword_blade2", "obj.godwars_godsword_blade3")
            column(CATEGORY, "Bosses")
            columnRSCM(COUNT_VARP_1, "varp.total_saradomin_kills")
            column(TAB_INDEX, 11)
        }

        row("dbrow.collection_log_category_corporeal_beast") {
            column(NAME, "Corporeal Beast")
            columnRSCM(COMPLETED_VARBIT, "varbit.collection_bosses_corp_completed")
            columnRSCM(ITEMS, "obj.corepet", "obj.elysian_sigil", "obj.spectral_sigil", "obj.arcane_sigil", "obj.holy_elixir", "obj.spirit_shield", "obj.jar_of_spirits")
            column(CATEGORY, "Bosses")
            columnRSCM(COUNT_VARP_1, "varp.total_corp_kills")
            column(TAB_INDEX, 12)
        }

        row("dbrow.collection_log_category_crazy_archaeologist") {
            column(NAME, "Crazy Archaeologist")
            columnRSCM(COMPLETED_VARBIT, "varbit.collection_bosses_arch_completed")
            columnRSCM(ITEMS, "obj.odium_shard2", "obj.malediction_shard2", "obj.fedora")
            column(CATEGORY, "Bosses")
            columnRSCM(COUNT_VARP_1, "varp.total_crazyarchaeologist_kills")
            column(TAB_INDEX, 13)
        }

        row("dbrow.collection_log_category_dagannoth_kings") {
            column(NAME, "Dagannoth Kings")
            columnRSCM(COMPLETED_VARBIT, "varbit.collection_bosses_dagannoth_completed")
            columnRSCM(ITEMS, "obj.primepet", "obj.supremepet", "obj.rexpet", "obj.berzerker_ring", "obj.ranger_ring", "obj.seer_ring", "obj.warrior_ring", "obj.dragon_axe", "obj.daganoth_cave_magic_shortbow", "obj.mud_battlestaff")
            column(CATEGORY, "Bosses")
            columnRSCM(COUNT_VARP_1, "varp.total_supreme_kills")
            columnRSCM(COUNT_VARP_2, "varp.total_prime_kills")
            columnRSCM(COUNT_VARP_3, "varp.total_rex_kills")
            column(TAB_INDEX, 14)
        }

        row("dbrow.collection_log_category_deranged_archaeologist") {
            column(NAME, "Deranged Archaeologist")
            columnRSCM(COMPLETED_VARBIT, "varbit.collection_bosses_deranged_archaeologist_completed")
            columnRSCM(ITEMS, "obj.steel_ring")
            column(CATEGORY, "Bosses")
            columnRSCM(COUNT_VARP_1, "varp.total_derangedarchaeologist_kills")
            column(TAB_INDEX, 15)
        }

        row("dbrow.collection_log_category_doom_of_mokhaiotl") {
            column(NAME, "Doom of Mokhaiotl")
            columnRSCM(COMPLETED_VARBIT, "varbit.collection_bosses_dom_completed")
            columnRSCM(ITEMS, "obj.dompet", "obj.avernic_treads", "obj.eye_of_ayak_uncharged", "obj.mokhaiotl_cloth", "obj.dom_teleport_item", "obj.demon_tear")
            column(CATEGORY, "Bosses")
            column(TAB_INDEX, 16)
        }

        row("dbrow.collection_log_category_duke_sucellus") {
            column(NAME, "Duke Sucellus")
            columnRSCM(COMPLETED_VARBIT, "varbit.collection_bosses_duke_completed")
            columnRSCM(ITEMS, "obj.dukesucelluspet", "obj.soulreaper_axe_eye", "obj.virtus_mask", "obj.virtus_top", "obj.virtus_legs", "obj.magus_vestige", "obj.ice_quartz", "obj.duke_sucellus_tablet", "obj.chromium_ingot", "obj.dt2_awakeners_orb")
            column(CATEGORY, "Bosses")
            columnRSCM(COUNT_VARP_1, "varp.total_duke_sucellus_kills")
            column(TAB_INDEX, 17)
        }

        row("dbrow.collection_log_category_the_fight_caves") {
            column(NAME, "The Fight Caves")
            columnRSCM(COMPLETED_VARBIT, "varbit.collection_bosses_fight_completed")
            columnRSCM(ITEMS, "obj.jad_pet", "obj.tzhaar_cape_fire")
            column(CATEGORY, "Bosses")
            columnRSCM(COUNT_VARP_1, "varp.total_jad_kills")
            column(TAB_INDEX, 18)
        }

        row("dbrow.collection_log_category_fortis_colosseum") {
            column(NAME, "Fortis Colosseum")
            columnRSCM(COMPLETED_VARBIT, "varbit.collection_bosses_colosseum_completed")
            columnRSCM(ITEMS, "obj.solhereditpet", "obj.dizanas_quiver_uncharged", "obj.sunfire_body", "obj.sunfire_legs", "obj.sunfire_helm", "obj.echo_crystal", "obj.tonalztics_of_ralos_uncharged", "obj.sunfiresplinter", "obj.uncut_onyx")
            column(CATEGORY, "Bosses")
            columnRSCM(COUNT_VARP_1, "varp.total_sol_kills")
            column(TAB_INDEX, 19)
        }

        row("dbrow.collection_log_category_the_gauntlet") {
            column(NAME, "The Gauntlet")
            columnRSCM(COMPLETED_VARBIT, "varbit.collection_bosses_gauntlet_completed")
            columnRSCM(ITEMS, "obj.gauntletpet", "obj.prif_armour_seed", "obj.crystal_seed_old", "obj.prif_weapon_seed_enhanced", "obj.gauntlet_crystalline_cape")
            column(CATEGORY, "Bosses")
            columnRSCM(COUNT_VARP_1, "varp.total_completed_gauntlet_hm")
            columnRSCM(COUNT_VARP_2, "varp.total_completed_gauntlet")
            column(TAB_INDEX, 20)
        }

        row("dbrow.collection_log_category_general_graardor") {
            column(NAME, "General Graardor")
            columnRSCM(COMPLETED_VARBIT, "varbit.collection_bosses_bandos_completed")
            columnRSCM(ITEMS, "obj.bandospet", "obj.bandos_chestplate", "obj.bandos_skirt", "obj.bandos_boots", "obj.godwars_godsword_hilt_bandos", "obj.godwars_godsword_blade1", "obj.godwars_godsword_blade2", "obj.godwars_godsword_blade3")
            column(CATEGORY, "Bosses")
            columnRSCM(COUNT_VARP_1, "varp.total_bandos_kills")
            column(TAB_INDEX, 21)
        }

        row("dbrow.collection_log_category_giant_mole") {
            column(NAME, "Giant Mole")
            columnRSCM(COMPLETED_VARBIT, "varbit.collection_bosses_mole_completed")
            columnRSCM(ITEMS, "obj.molepet", "obj.mole_skin", "obj.mole_claw", "obj.immaculate_mole_skin")
            column(CATEGORY, "Bosses")
            columnRSCM(COUNT_VARP_1, "varp.total_mole_kills")
            column(TAB_INDEX, 22)
        }

        row("dbrow.collection_log_category_grotesque_guardians") {
            column(NAME, "Grotesque Guardians")
            columnRSCM(COMPLETED_VARBIT, "varbit.collection_bosses_gargoyle_completed")
            columnRSCM(ITEMS, "obj.dawnpet", "obj.tourmaline_core", "obj.granite_gloves", "obj.granite_ring", "obj.granite_hammer", "obj.jar_of_stone", "obj.granite_dust")
            column(CATEGORY, "Bosses")
            columnRSCM(COUNT_VARP_1, "varp.total_gargboss_kills")
            column(TAB_INDEX, 23)
        }

        row("dbrow.collection_log_category_hespori") {
            column(NAME, "Hespori")
            columnRSCM(COMPLETED_VARBIT, "varbit.collection_bosses_hespori_completed")
            columnRSCM(ITEMS, "obj.bottomless_compost_bucket", "obj.iasor_seed", "obj.kronos_seed", "obj.attas_seed")
            column(CATEGORY, "Bosses")
            columnRSCM(COUNT_VARP_1, "varp.total_hespori_kills")
            column(TAB_INDEX, 24)
        }

        row("dbrow.collection_log_category_the_hueycoatl") {
            column(NAME, "The Hueycoatl")
            columnRSCM(COMPLETED_VARBIT, "varbit.collection_bosses_hueycoatl_completed")
            columnRSCM(ITEMS, "obj.hueypet", "obj.dragonhunter_wand", "obj.tome_of_earth_uncharged", "obj.soiled_page", "obj.huey_hide", "obj.huasca_seed")
            column(CATEGORY, "Bosses")
            columnRSCM(COUNT_VARP_1, "varp.total_huey_kills")
            column(TAB_INDEX, 25)
        }

        row("dbrow.collection_log_category_the_inferno") {
            column(NAME, "The Inferno")
            columnRSCM(COMPLETED_VARBIT, "varbit.collection_bosses_inferno_completed")
            columnRSCM(ITEMS, "obj.infernopet", "obj.infernal_cape")
            column(CATEGORY, "Bosses")
            columnRSCM(COUNT_VARP_1, "varp.total_zuk_kills")
            column(TAB_INDEX, 26)
        }

        row("dbrow.collection_log_category_kalphite_queen") {
            column(NAME, "Kalphite Queen")
            columnRSCM(COMPLETED_VARBIT, "varbit.collection_bosses_kalphite_completed")
            columnRSCM(ITEMS, "obj.kqpet_walking", "obj.poh_trophydrop_kalphitequeen", "obj.jar_of_sand", "obj.dragon_2h_sword", "obj.dragon_chainbody", "obj.dragon_pickaxe")
            column(CATEGORY, "Bosses")
            columnRSCM(COUNT_VARP_1, "varp.total_kalphite_kills")
            column(TAB_INDEX, 27)
        }

        row("dbrow.collection_log_category_king_black_dragon") {
            column(NAME, "King Black Dragon")
            columnRSCM(COMPLETED_VARBIT, "varbit.collection_bosses_dragon_completed")
            columnRSCM(ITEMS, "obj.kbdpet", "obj.poh_trophydrop_kbd", "obj.dragon_pickaxe", "obj.dragonfire_visage")
            column(CATEGORY, "Bosses")
            columnRSCM(COUNT_VARP_1, "varp.total_kbd_kills")
            column(TAB_INDEX, 28)
        }

        row("dbrow.collection_log_category_kraken") {
            column(NAME, "Kraken")
            columnRSCM(COMPLETED_VARBIT, "varbit.collection_bosses_kraken_completed")
            columnRSCM(ITEMS, "obj.krakenpet", "obj.kraken_tentacle", "obj.tots", "obj.jar_of_dirt")
            column(CATEGORY, "Bosses")
            columnRSCM(COUNT_VARP_1, "varp.total_kraken_boss_kills")
            column(TAB_INDEX, 29)
        }

        row("dbrow.collection_log_category_kree_arra") {
            column(NAME, "Kree'arra")
            columnRSCM(COMPLETED_VARBIT, "varbit.collection_bosses_armadyl_completed")
            columnRSCM(ITEMS, "obj.armadylpet", "obj.armadyl_helmet", "obj.armadyl_chestplate", "obj.armadyl_skirt", "obj.godwars_godsword_hilt_armadyl", "obj.godwars_godsword_blade1", "obj.godwars_godsword_blade2", "obj.godwars_godsword_blade3")
            column(CATEGORY, "Bosses")
            columnRSCM(COUNT_VARP_1, "varp.total_armadyl_kills")
            column(TAB_INDEX, 30)
        }

        row("dbrow.collection_log_category_k_ril_tsutsaroth") {
            column(NAME, "K'ril Tsutsaroth")
            columnRSCM(COMPLETED_VARBIT, "varbit.collection_bosses_zamorak_completed")
            columnRSCM(ITEMS, "obj.zamorakpet", "obj.sotd", "obj.zamorak_spear", "obj.steam_battlestaff", "obj.godwars_godsword_hilt_zamorak", "obj.godwars_godsword_blade1", "obj.godwars_godsword_blade2", "obj.godwars_godsword_blade3")
            column(CATEGORY, "Bosses")
            columnRSCM(COUNT_VARP_1, "varp.total_zamorak_kills")
            column(TAB_INDEX, 31)
        }

        row("dbrow.collection_log_category_the_leviathan") {
            column(NAME, "The Leviathan")
            columnRSCM(COMPLETED_VARBIT, "varbit.collection_bosses_leviathan_completed")
            columnRSCM(ITEMS, "obj.leviathanpet", "obj.soulreaper_axe_lure", "obj.virtus_mask", "obj.virtus_top", "obj.virtus_legs", "obj.venator_vestige", "obj.smoke_quartz", "obj.leviathan_tablet", "obj.chromium_ingot", "obj.dt2_awakeners_orb")
            column(CATEGORY, "Bosses")
            columnRSCM(COUNT_VARP_1, "varp.total_leviathan_kills")
            column(TAB_INDEX, 32)
        }

        row("dbrow.collection_log_category_maggot_king") {
            column(NAME, "Maggot King")
            columnRSCM(COMPLETED_VARBIT, "varbit.collection_bosses_maggotking_completed")
            columnRSCM(ITEMS, "obj.maggotkingpet", "obj.crimson_kisten", "obj.elder_venator_fang")
            column(CATEGORY, "Bosses")
            columnRSCM(COUNT_VARP_1, "varp.total_maggot_king_kills")
            column(TAB_INDEX, 34)
        }

        row("dbrow.collection_log_category_moons_of_peril") {
            column(NAME, "Moons of Peril")
            columnRSCM(COMPLETED_VARBIT, "varbit.collection_bosses_perilous_moons_completed")
            columnRSCM(ITEMS, "obj.eclipse_moon_chestplate", "obj.eclipse_moon_tassets", "obj.eclipse_moon_helm", "obj.eclipse_atlatl", "obj.frost_moon_chestplate", "obj.frost_moon_tassets", "obj.frost_moon_helm", "obj.frostmoon_spear", "obj.blood_moon_chestplate", "obj.blood_moon_tassets", "obj.blood_moon_helm", "obj.dual_macuahuitl", "obj.atlatl_dart")
            column(CATEGORY, "Bosses")
            column(TAB_INDEX, 35)
        }

        row("dbrow.collection_log_category_nex") {
            column(NAME, "Nex")
            columnRSCM(COMPLETED_VARBIT, "varbit.collection_bosses_nex_completed")
            columnRSCM(ITEMS, "obj.nexpet", "obj.godwars_godsword_hilt_ancient", "obj.nihil_horn", "obj.zaryte_vambraces", "obj.broken_torva_helm", "obj.broken_torva_chest", "obj.broken_torva_legs", "obj.nihil_shard")
            column(CATEGORY, "Bosses")
            columnRSCM(COUNT_VARP_1, "varp.total_nex_kills")
            column(TAB_INDEX, 36)
        }

        row("dbrow.collection_log_category_the_nightmare") {
            column(NAME, "The Nightmare")
            columnRSCM(COMPLETED_VARBIT, "varbit.collection_bosses_nightmare_completed")
            columnRSCM(ITEMS, "obj.nightmarepet", "obj.inquisitors_mace", "obj.inquisitors_helm", "obj.inquisitors_body", "obj.inquisitors_skirt", "obj.nightmare_staff", "obj.volatile_orb", "obj.harmonised_orb", "obj.eldritch_orb", "obj.jar_of_dreams", "obj.slepe_teleport_consumable", "obj.nightmare_challenge_morph")
            column(CATEGORY, "Bosses")
            columnRSCM(COUNT_VARP_1, "varp.total_nightmare_kills")
            columnRSCM(COUNT_VARP_2, "varp.total_nightmare_challenge_kills")
            column(TAB_INDEX, 37)
        }

        row("dbrow.collection_log_category_obor") {
            column(NAME, "Obor")
            columnRSCM(COMPLETED_VARBIT, "varbit.collection_bosses_obor_completed")
            columnRSCM(ITEMS, "obj.hillgiant_boss_club")
            column(CATEGORY, "Bosses")
            columnRSCM(COUNT_VARP_1, "varp.total_hillgiant_boss_kills")
            column(TAB_INDEX, 38)
        }

        row("dbrow.collection_log_category_phantom_muspah") {
            column(NAME, "Phantom Muspah")
            columnRSCM(COMPLETED_VARBIT, "varbit.collection_bosses_muspah_completed")
            columnRSCM(ITEMS, "obj.muspahpet", "obj.venator_shard", "obj.ancient_icon", "obj.muspah_pet_morph", "obj.frozen_cache", "obj.ancient_essence")
            column(CATEGORY, "Bosses")
            columnRSCM(COUNT_VARP_1, "varp.total_muspah_kills")
            column(TAB_INDEX, 39)
        }

        row("dbrow.collection_log_category_royal_titans") {
            column(NAME, "Royal Titans")
            columnRSCM(COMPLETED_VARBIT, "varbit.collection_bosses_royal_titans_completed")
            columnRSCM(ITEMS, "obj.rtbrandapet", "obj.deadeye_prayer_scroll", "obj.mystic_vigour_prayer_scroll", "obj.giantsoul_amulet_uncharged", "obj.twinflame_piece_1", "obj.twinflame_piece_2", "obj.desiccated_page")
            column(CATEGORY, "Bosses")
            columnRSCM(COUNT_VARP_1, "varp.total_royal_titan_kills")
            column(TAB_INDEX, 40)
        }

        row("dbrow.collection_log_category_sarachnis") {
            column(NAME, "Sarachnis")
            columnRSCM(COMPLETED_VARBIT, "varbit.collection_bosses_sarachnis_completed")
            columnRSCM(ITEMS, "obj.sarachnispet", "obj.jar_of_eyes", "obj.hosdun_egg_sac_full", "obj.sarachnis_cudgel", "obj.slayer_spider_silk")
            column(CATEGORY, "Bosses")
            columnRSCM(COUNT_VARP_1, "varp.total_sarachnis_kills")
            column(TAB_INDEX, 41)
        }

        row("dbrow.collection_log_category_scorpia") {
            column(NAME, "Scorpia")
            columnRSCM(COMPLETED_VARBIT, "varbit.collection_bosses_scorpia_completed")
            columnRSCM(ITEMS, "obj.scorpia_pet", "obj.odium_shard3", "obj.malediction_shard3", "obj.dragon_2h_sword")
            column(CATEGORY, "Bosses")
            columnRSCM(COUNT_VARP_1, "varp.total_scorpia_kills")
            column(TAB_INDEX, 42)
        }

        row("dbrow.collection_log_category_scurrius") {
            column(NAME, "Scurrius")
            columnRSCM(COMPLETED_VARBIT, "varbit.collection_bosses_rat_boss_completed")
            columnRSCM(ITEMS, "obj.scurriuspet", "obj.rat_boss_spine")
            column(CATEGORY, "Bosses")
            columnRSCM(COUNT_VARP_1, "varp.total_rat_boss_kills")
            column(TAB_INDEX, 43)
        }

        row("dbrow.collection_log_category_shellbane_gryphon") {
            column(NAME, "Shellbane Gryphon")
            columnRSCM(COMPLETED_VARBIT, "varbit.collection_bosses_gryphon_boss_completed")
            columnRSCM(ITEMS, "obj.gryphonbosspet", "obj.jar_of_feathers", "obj.belles_folly_tarnished", "obj.gryphon_feather")
            column(CATEGORY, "Bosses")
            columnRSCM(COUNT_VARP_1, "varp.total_gryphon_boss_kills")
            column(TAB_INDEX, 44)
        }

        row("dbrow.collection_log_category_skotizo") {
            column(NAME, "Skotizo")
            columnRSCM(COMPLETED_VARBIT, "varbit.collection_bosses_skotizo_completed")
            columnRSCM(ITEMS, "obj.skotizopet", "obj.jar_of_darkness", "obj.cata_boss_claw", "obj.cata_totem", "obj.uncut_onyx", "obj.cata_shard")
            column(CATEGORY, "Bosses")
            columnRSCM(COUNT_VARP_1, "varp.total_cata_boss_kills")
            column(TAB_INDEX, 45)
        }

        row("dbrow.collection_log_category_tempoross") {
            column(NAME, "Tempoross")
            columnRSCM(COMPLETED_VARBIT, "varbit.collection_bosses_tempoross_completed")
            columnRSCM(ITEMS, "obj.temporosspet", "obj.poh_trophydrop_harpoonfish", "obj.spirit_angler_hat", "obj.spirit_angler_top", "obj.spirit_angler_legs", "obj.spirit_angler_boots", "obj.tome_of_water_uncharged", "obj.soaked_page", "obj.tackle_box", "obj.fish_barrel_closed", "obj.dragon_harpoon", "obj.spirit_flakes")
            column(CATEGORY, "Bosses")
            columnRSCM(COUNT_VARP_1, "varp.total_tempoross_kills")
            column(TAB_INDEX, 46)
        }

        row("dbrow.collection_log_category_thermonuclear_smoke_devil") {
            column(NAME, "Thermonuclear Smoke Devil")
            columnRSCM(COMPLETED_VARBIT, "varbit.collection_bosses_smoke_completed")
            columnRSCM(ITEMS, "obj.smokepet", "obj.occult_necklace", "obj.smoke_battlestaff", "obj.dragon_chainbody", "obj.jar_of_smoke")
            column(CATEGORY, "Bosses")
            columnRSCM(COUNT_VARP_1, "varp.total_thermy_kills")
            column(TAB_INDEX, 47)
        }

        row("dbrow.collection_log_category_vardorvis") {
            column(NAME, "Vardorvis")
            columnRSCM(COMPLETED_VARBIT, "varbit.collection_bosses_vardorvis_completed")
            columnRSCM(ITEMS, "obj.vardorvispet", "obj.soulreaper_axe_head", "obj.virtus_mask", "obj.virtus_top", "obj.virtus_legs", "obj.ultor_vestige", "obj.blood_quartz", "obj.vardorvis_tablet", "obj.chromium_ingot", "obj.dt2_awakeners_orb")
            column(CATEGORY, "Bosses")
            columnRSCM(COUNT_VARP_1, "varp.total_vardorvis_kills")
            column(TAB_INDEX, 48)
        }

        row("dbrow.collection_log_category_venenatis_and_spindel") {
            column(NAME, "Venenatis and Spindel")
            columnRSCM(COMPLETED_VARBIT, "varbit.collection_bosses_venenatis_completed")
            columnRSCM(ITEMS, "obj.venenatis_pet", "obj.sharp_ring", "obj.dragon_pickaxe", "obj.dragon_2h_sword", "obj.wbr_venenatis_fang", "obj.wbr_voidwaker_gem")
            column(CATEGORY, "Bosses")
            columnRSCM(COUNT_VARP_1, "varp.total_spindel_kills")
            columnRSCM(COUNT_VARP_2, "varp.total_venenatis_kills")
            column(TAB_INDEX, 49)
        }

        row("dbrow.collection_log_category_vet_ion_and_calvar_ion") {
            column(NAME, "Vet'ion and Calvar'ion")
            columnRSCM(COMPLETED_VARBIT, "varbit.collection_bosses_vetion_completed")
            columnRSCM(ITEMS, "obj.vetion_pet", "obj.rotg", "obj.dragon_pickaxe", "obj.dragon_2h_sword", "obj.wbr_vetion_skull", "obj.wbr_voidwaker_blade")
            column(CATEGORY, "Bosses")
            columnRSCM(COUNT_VARP_1, "varp.total_calvarion_kills")
            columnRSCM(COUNT_VARP_2, "varp.total_vetion_kills")
            column(TAB_INDEX, 50)
        }

        row("dbrow.collection_log_category_vorkath") {
            column(NAME, "Vorkath")
            columnRSCM(COMPLETED_VARBIT, "varbit.collection_bosses_vorkath_completed")
            columnRSCM(ITEMS, "obj.vorkathpet", "obj.vorkath_head", "obj.dragonfire_visage", "obj.skeletal_visage", "obj.jar_of_decay", "obj.dragonbone_necklace")
            column(CATEGORY, "Bosses")
            columnRSCM(COUNT_VARP_1, "varp.total_vorkath_kills")
            column(TAB_INDEX, 51)
        }

        row("dbrow.collection_log_category_the_whisperer") {
            column(NAME, "The Whisperer")
            columnRSCM(COMPLETED_VARBIT, "varbit.collection_bosses_whisperer_completed")
            columnRSCM(ITEMS, "obj.whispererpet", "obj.soulreaper_axe_staff", "obj.virtus_mask", "obj.virtus_top", "obj.virtus_legs", "obj.bellator_vestige", "obj.shadow_quartz", "obj.whisperer_tablet", "obj.chromium_ingot", "obj.dt2_awakeners_orb")
            column(CATEGORY, "Bosses")
            columnRSCM(COUNT_VARP_1, "varp.total_whisperer_kills")
            column(TAB_INDEX, 52)
        }

        row("dbrow.collection_log_category_wintertodt") {
            column(NAME, "Wintertodt")
            columnRSCM(COMPLETED_VARBIT, "varbit.collection_bosses_wintertodt_completed")
            columnRSCM(ITEMS, "obj.phoenixpet", "obj.tome_of_fire_uncharged", "obj.wint_burnt_page", "obj.pyromancer_top", "obj.pyromancer_hood", "obj.pyromancer_bottom", "obj.pyromancer_boots", "obj.pyromancer_gloves", "obj.wint_torch", "obj.dragon_axe")
            column(CATEGORY, "Bosses")
            columnRSCM(COUNT_VARP_1, "varp.total_wintertodt_kills")
            column(TAB_INDEX, 53)
        }

        row("dbrow.collection_log_category_yama") {
            column(NAME, "Yama")
            columnRSCM(COMPLETED_VARBIT, "varbit.collection_bosses_yama_completed")
            columnRSCM(ITEMS, "obj.yamapet", "obj.teleportscroll_chasmoffire", "obj.oathplate_shards", "obj.oathplate_helm", "obj.oathplate_chest", "obj.oathplate_legs", "obj.soulflame_horn", "obj.death_charge_scroll", "obj.forgotten_lockbox", "obj.yama_dossier", "obj.demonic_tallow_barrel_full")
            column(CATEGORY, "Bosses")
            columnRSCM(COUNT_VARP_1, "varp.total_yama_kills")
            column(TAB_INDEX, 54)
        }

        row("dbrow.collection_log_category_zalcano") {
            column(NAME, "Zalcano")
            columnRSCM(COMPLETED_VARBIT, "varbit.collection_bosses_zalcano_completed")
            columnRSCM(ITEMS, "obj.zalcanopet", "obj.prif_tool_seed", "obj.zalcano_pickaxe_kit", "obj.uncut_onyx")
            column(CATEGORY, "Bosses")
            columnRSCM(COUNT_VARP_1, "varp.total_zalcano_kills")
            column(TAB_INDEX, 55)
        }

        row("dbrow.collection_log_category_zulrah") {
            column(NAME, "Zulrah")
            columnRSCM(COMPLETED_VARBIT, "varbit.collection_bosses_zulrah_completed")
            columnRSCM(ITEMS, "obj.snakepet", "obj.cyan_mutagen", "obj.red_mutagen", "obj.jar_of_swamp", "obj.magic_fang", "obj.serpentine_visage", "obj.blowpipe_fang", "obj.teleportscroll_zulandra", "obj.uncut_onyx", "obj.snakeboss_scale")
            column(CATEGORY, "Bosses")
            columnRSCM(COUNT_VARP_1, "varp.total_snakeboss_kills")
            column(TAB_INDEX, 56)
        }

        row("dbrow.collection_log_category_chambers_of_xeric") {
            column(NAME, "Chambers of Xeric")
            columnRSCM(COMPLETED_VARBIT, "varbit.collection_raids_cox_completed")
            columnRSCM(ITEMS, "obj.olmpet", "obj.raids_challenge_morph", "obj.twisted_bow", "obj.elder_maul", "obj.kodai_insignia", "obj.dragon_claws", "obj.ancestral_hat", "obj.ancestral_robe_top", "obj.ancestral_robe_bottom", "obj.dinhs_bulwark", "obj.raids_prayerscroll", "obj.raids_prayerscroll_augury", "obj.dragonhunter_xbow", "obj.twisted_buckler", "obj.raids_prayerscroll_preserve", "obj.raids_ancient_relic", "obj.onyx", "obj.ancestral_robes_twisted_kit", "obj.cox_challenge_cape_t1", "obj.cox_challenge_cape_t2", "obj.cox_challenge_cape_t3", "obj.cox_challenge_cape_t4", "obj.cox_challenge_cape_t5")
            column(CATEGORY, "Raids")
            columnRSCM(COUNT_VARP_1, "varp.total_completed_xericchambers_challenge")
            columnRSCM(COUNT_VARP_2, "varp.total_completed_xericchambers")
            column(TAB_INDEX, 0)
        }

        row("dbrow.collection_log_category_theatre_of_blood") {
            column(NAME, "Theatre of Blood")
            columnRSCM(COMPLETED_VARBIT, "varbit.collection_raids_tob_completed")
            columnRSCM(ITEMS, "obj.verzikpet", "obj.scythe_of_vitur_uncharged", "obj.ghrazi_rapier", "obj.sanguinesti_staff_uncharged", "obj.justiciar_faceguard", "obj.justiciar_chestguard", "obj.justiciar_leg_guards", "obj.infernal_defender_hilt", "obj.vial_blood", "obj.sinhaza_shroud_tier1", "obj.sinhaza_shroud_tier2", "obj.sinhaza_shroud_tier3", "obj.sinhaza_shroud_tier4", "obj.sinhaza_shroud_tier5", "obj.tob_hardmode_dust", "obj.tob_hardmode_kit", "obj.tob_hardmode_kit_blood")
            column(CATEGORY, "Raids")
            columnRSCM(COUNT_VARP_1, "varp.total_completed_theatreofblood")
            columnRSCM(COUNT_VARP_2, "varp.total_completed_theatreofblood_story")
            columnRSCM(COUNT_VARP_3, "varp.total_completed_theatreofblood_hard")
            column(TAB_INDEX, 1)
        }

        row("dbrow.collection_log_category_tombs_of_amascut") {
            column(NAME, "Tombs of Amascut")
            columnRSCM(COMPLETED_VARBIT, "varbit.collection_raids_toa_completed")
            columnRSCM(ITEMS, "obj.wardenpet_tumeken", "obj.tumekens_shadow_uncharged", "obj.elidinis_ward", "obj.masori_mask", "obj.masori_body", "obj.masori_chaps", "obj.lightbearer", "obj.osmumtens_fang", "obj.thread_of_elidinis", "obj.breach_of_the_scarab", "obj.eye_of_the_corruptor", "obj.jewel_of_the_sun", "obj.jewel_of_amascut", "obj.elidinis_ward_ornament_kit", "obj.osmumtens_fang_ornament_kit", "obj.avas_assembler_ornament_kit", "obj.toa_rune_cache", "obj.icthlarins_shroud_1", "obj.icthlarins_shroud_2", "obj.icthlarins_shroud_3", "obj.icthlarins_shroud_4", "obj.icthlarins_shroud_5", "obj.toa_pet_morph_akkha", "obj.toa_pet_morph_baba", "obj.toa_pet_morph_kephri", "obj.toa_pet_morph_zebak", "obj.toa_pet_morph_wardens")
            column(CATEGORY, "Raids")
            columnRSCM(COUNT_VARP_1, "varp.total_completed_tombsofamascut")
            columnRSCM(COUNT_VARP_2, "varp.total_completed_tombsofamascut_entry")
            columnRSCM(COUNT_VARP_3, "varp.total_completed_tombsofamascut_expert")
            column(TAB_INDEX, 2)
        }

        row("dbrow.collection_log_category_beginner_treasure_trails") {
            column(NAME, "Beginner Treasure Trails")
            columnRSCM(COMPLETED_VARBIT, "varbit.collection_clues_beginner_completed")
            columnRSCM(ITEMS, "obj.mole_slippers", "obj.frog_slippers", "obj.bear_slippers", "obj.demon_slippers", "obj.jester_cape", "obj.shoulder_parrot", "obj.monk_robetop_t", "obj.monk_robebottom_t", "obj.amulet_of_defence_t", "obj.sandwich_lady_hat", "obj.sandwich_lady_top", "obj.sandwich_lady_bottom", "obj.rune_scimitar_ornament_kit_guthix", "obj.rune_scimitar_ornament_kit_saradomin", "obj.rune_scimitar_ornament_kit_zamorak", "obj.black_pickaxe")
            column(CATEGORY, "Clues")
            column(TAB_INDEX, 0)
        }

        row("dbrow.collection_log_category_easy_treasure_trails") {
            column(NAME, "Easy Treasure Trails")
            columnRSCM(COMPLETED_VARBIT, "varbit.collection_clues_easy_completed")
            columnRSCM(ITEMS, "obj.wilderness_cape_zero", "obj.wilderness_cape_i", "obj.wilderness_cape_x", "obj.cape_of_skulls", "obj.chefs_hat_gold", "obj.golden_apron", "obj.wooden_shield_gold", "obj.black_full_helm_trim", "obj.black_platebody_trim", "obj.black_platelegs_trim", "obj.black_plateskirt_trim", "obj.black_kiteshield_trim", "obj.black_full_helm_gold", "obj.black_platebody_gold", "obj.black_platelegs_gold", "obj.black_plateskirt_gold", "obj.black_kiteshield_gold", "obj.black_heraldic_kiteshield1", "obj.black_heraldic_kiteshield2", "obj.black_heraldic_kiteshield3", "obj.black_heraldic_kiteshield4", "obj.black_heraldic_kiteshield5", "obj.trail_heraldic_helm_1_black", "obj.trail_heraldic_helm_2_black", "obj.trail_heraldic_helm_3_black", "obj.trail_heraldic_helm_4_black", "obj.trail_heraldic_helm_5_black", "obj.black_platebody_h1", "obj.black_platebody_h2", "obj.black_platebody_h3", "obj.black_platebody_h4", "obj.black_platebody_h5", "obj.steel_full_helm_trim", "obj.steel_platebody_trim", "obj.steel_platelegs_trim", "obj.steel_plateskirt_trim", "obj.steel_kiteshield_trim", "obj.steel_full_helm_gold", "obj.steel_platebody_gold", "obj.steel_platelegs_gold", "obj.steel_plateskirt_gold", "obj.steel_kiteshield_gold", "obj.iron_platebody_trim", "obj.iron_platelegs_trim", "obj.iron_plateskirt_trim", "obj.iron_kiteshield_trim", "obj.iron_full_helm_trim", "obj.iron_platebody_gold", "obj.iron_platelegs_gold", "obj.iron_plateskirt_gold", "obj.iron_kiteshield_gold", "obj.iron_full_helm_gold", "obj.bronze_platebody_trim", "obj.bronze_platelegs_trim", "obj.bronze_plateskirt_trim", "obj.bronze_kiteshield_trim", "obj.bronze_full_helm_trim", "obj.bronze_platebody_gold", "obj.bronze_platelegs_gold", "obj.bronze_plateskirt_gold", "obj.bronze_kiteshield_gold", "obj.bronze_full_helm_gold", "obj.studded_body_trim_gold", "obj.studded_chaps_trim_gold", "obj.studded_body_trim_fur", "obj.studded_chaps_trim_fur", "obj.leather_armour_trim_gold", "obj.leather_chaps_trim_gold", "obj.bluewizhat_trim_gold", "obj.wizards_robe_trim_gold", "obj.blue_skirt_trim_gold", "obj.bluewizhat_trim", "obj.wizards_robe_trim", "obj.blue_skirt_trim", "obj.blackwizhat_gold", "obj.black_wizards_robe_gold", "obj.black_skirt_gold", "obj.blackwizhat_trim", "obj.black_wizards_robe_trim", "obj.black_skirt_trim", "obj.monk_robetop_gold", "obj.monk_robebottom_gold", "obj.trail_saradomin_robe_t", "obj.trail_saradomin_robe_l", "obj.trail_guthix_robe_t", "obj.trail_guthix_robe_l", "obj.trail_zamorak_robe_t", "obj.trail_zamorak_robe_l", "obj.trail_ancient_robe_t", "obj.trail_ancient_robe_l", "obj.trail_armadyl_robe_t", "obj.trail_armadyl_robe_l", "obj.trail_bandos_robe_t", "obj.trail_bandos_robe_l", "obj.trail_bob_shirt_red", "obj.trail_bob_shirt_green", "obj.trail_bob_shirt_blue", "obj.trail_bob_shirt_black", "obj.trail_bob_shirt_purple", "obj.highwaymanmask", "obj.berret_blue", "obj.berret_black", "obj.berret_white", "obj.berret_red", "obj.trail_wig", "obj.beanie_hat", "obj.imp_mask", "obj.goblin_mask", "obj.trail_sleeping_cap", "obj.trail_flared_pants", "obj.trail_pantaloons", "obj.black_cane", "obj.staff_of_bobcat", "obj.trail_elegant_shirt_male_r", "obj.trail_elegant_shirt_female_r", "obj.trail_elegant_pants_male_r", "obj.trail_elegant_pants_female_r", "obj.trail_elegant_shirt_male_g", "obj.trail_elegant_shirt_female_g", "obj.trail_elegant_pants_male_g", "obj.trail_elegant_pants_female_g", "obj.trail_elegant_shirt_male_b", "obj.trail_elegant_shirt_female_b", "obj.trail_elegant_pants_male_b", "obj.trail_elegant_pants_female_b", "obj.trail_amulet_of_magic", "obj.trail_power_ammy", "obj.black_pickaxe", "obj.joint_of_ham", "obj.rain_bow", "obj.trail_composite_bow_willow")
            column(CATEGORY, "Clues")
            column(TAB_INDEX, 1)
        }

        row("dbrow.collection_log_category_medium_treasure_trails") {
            column(NAME, "Medium Treasure Trails")
            columnRSCM(COMPLETED_VARBIT, "varbit.collection_clues_medium_completed")
            columnRSCM(ITEMS, "obj.boots_ranger", "obj.boots_wizard", "obj.holy_sandals", "obj.climbing_boots_g", "obj.spiked_manacles", "obj.adamant_full_helm_trim", "obj.adamant_platebody_trim", "obj.adamant_platelegs_trim", "obj.adamant_plateskirt_trim", "obj.adamant_kiteshield_trim", "obj.adamant_full_helm_gold", "obj.adamant_platebody_gold", "obj.adamant_platelegs_gold", "obj.adamant_plateskirt_gold", "obj.adamant_kiteshield_gold", "obj.adamant_heraldic_kiteshield1", "obj.adamant_heraldic_kiteshield2", "obj.adamant_heraldic_kiteshield3", "obj.adamant_heraldic_kiteshield4", "obj.adamant_heraldic_kiteshield5", "obj.trail_heraldic_helm_1_adamant", "obj.trail_heraldic_helm_2_adamant", "obj.trail_heraldic_helm_3_adamant", "obj.trail_heraldic_helm_4_adamant", "obj.trail_heraldic_helm_5_adamant", "obj.adamant_platebody_h1", "obj.adamant_platebody_h2", "obj.adamant_platebody_h3", "obj.adamant_platebody_h4", "obj.adamant_platebody_h5", "obj.mithril_full_helm_gold", "obj.mithril_platebody_gold", "obj.mithril_platelegs_gold", "obj.mithril_plateskirt_gold", "obj.mithril_kiteshield_gold", "obj.mithril_full_helm_trim", "obj.mithril_platebody_trim", "obj.mithril_platelegs_trim", "obj.mithril_plateskirt_trim", "obj.mithril_kiteshield_trim", "obj.dragonhide_body_trim_gold", "obj.dragonhide_body_trim", "obj.dragonhide_chaps_trim_gold", "obj.dragonhide_chaps_trim", "obj.trail_saradomin_mitre", "obj.trail_saradomin_cloak", "obj.trail_guthix_mitre", "obj.trail_guthix_cloak", "obj.trail_zamorak_mitre", "obj.trail_zamorak_cloak", "obj.trail_ancient_mitre", "obj.trail_ancient_cloak", "obj.trail_ancient_scarf", "obj.trail_ancient_staff", "obj.trail_armadyl_mitre", "obj.trail_armadyl_cloak", "obj.trail_armadyl_scarf", "obj.trail_armadyl_staff", "obj.trail_bandos_mitre", "obj.trail_bandos_cloak", "obj.trail_bandos_scarf", "obj.trail_bandos_staff", "obj.strawboater_red", "obj.strawboater_green", "obj.strawboater_orange", "obj.strawboater_black", "obj.strawboater_blue", "obj.strawboater_pink", "obj.strawboater_purple", "obj.strawboater_white", "obj.headband_red", "obj.headband_black", "obj.headband_brown", "obj.headband_white", "obj.headband_blue", "obj.headband_gold", "obj.headband_pink", "obj.headband_green", "obj.trail_crier_hat", "obj.town_crier_coat", "obj.town_crier_bell", "obj.adamant_cane", "obj.arceuus_banner", "obj.piscarilius_banner", "obj.hosidius_banner", "obj.shayzien_banner", "obj.lovakengj_banner", "obj.cabbage_shield", "obj.black_unicorn_mask", "obj.white_unicorn_mask", "obj.cat_mask", "obj.penguin_mask", "obj.leprechaun_hat", "obj.black_leprechaun_hat", "obj.wolf_mask", "obj.wolf_cloak", "obj.trail_elegant_shirt_male_p", "obj.trail_elegant_shirt_female_p", "obj.trail_elegant_pants_male_p", "obj.trail_elegant_pants_female_p", "obj.trail_elegant_shirt_male", "obj.trail_elegant_shirt_female", "obj.trail_elegant_pants_male", "obj.trail_elegant_pants_female", "obj.trail_elegant_shirt_male_pink", "obj.trail_elegant_shirt_female_pink", "obj.trail_elegant_pants_male_pink", "obj.trail_elegant_pants_female_pink", "obj.trail_elegant_shirt_male_gold", "obj.trail_elegant_shirt_female_gold", "obj.trail_elegant_pants_male_gold", "obj.trail_elegant_pants_female_gold", "obj.gnomish_firelighter", "obj.trail_amulet_of_strength", "obj.trail_composite_bow_yew")
            column(CATEGORY, "Clues")
            column(TAB_INDEX, 2)
        }

        row("dbrow.collection_log_category_hard_treasure_trails") {
            column(NAME, "Hard Treasure Trails")
            columnRSCM(COMPLETED_VARBIT, "varbit.collection_clues_hard_completed")
            columnRSCM(ITEMS, "obj.robinhoodhat", "obj.dragon_boots_kit", "obj.rd_ornament_kit", "obj.tzhaar_maul_ornament_kit", "obj.beserker_necklace_ornament_kit", "obj.rune_full_helm_trim", "obj.rune_platebody_trim", "obj.rune_platelegs_trim", "obj.rune_plateskirt_trim", "obj.rune_kiteshield_trim", "obj.rune_full_helm_gold", "obj.rune_platebody_gold", "obj.rune_platelegs_gold", "obj.rune_plateskirt_gold", "obj.rune_kiteshield_gold", "obj.rune_full_helm_zamorak", "obj.rune_platebody_zamorak", "obj.rune_platelegs_zamorak", "obj.rune_plateskirt_zamorak", "obj.rune_kiteshield_zamorak", "obj.rune_full_helm_guthix", "obj.rune_platebody_guthix", "obj.rune_platelegs_guthix", "obj.rune_plateskirt_guthix", "obj.rune_kiteshield_guthix", "obj.rune_full_helm_saradomin", "obj.rune_platebody_saradomin", "obj.rune_platelegs_saradomin", "obj.rune_plateskirt_saradomin", "obj.rune_kiteshield_saradomin", "obj.rune_full_helm_ancient", "obj.rune_platebody_ancient", "obj.rune_platelegs_ancient", "obj.rune_plateskirt_ancient", "obj.rune_kiteshield_ancient", "obj.rune_full_helm_armadyl", "obj.rune_platebody_armadyl", "obj.rune_platelegs_armadyl", "obj.rune_plateskirt_armadyl", "obj.rune_kiteshield_armadyl", "obj.rune_full_helm_bandos", "obj.rune_platebody_bandos", "obj.rune_platelegs_bandos", "obj.rune_plateskirt_bandos", "obj.rune_kiteshield_bandos", "obj.rune_heraldic_kiteshield1", "obj.rune_heraldic_kiteshield2", "obj.rune_heraldic_kiteshield3", "obj.rune_heraldic_kiteshield4", "obj.rune_heraldic_kiteshield5", "obj.trail_heraldic_helm_1_rune", "obj.trail_heraldic_helm_2_rune", "obj.trail_heraldic_helm_3_rune", "obj.trail_heraldic_helm_4_rune", "obj.trail_heraldic_helm_5_rune", "obj.rune_platebody_h1", "obj.rune_platebody_h2", "obj.rune_platebody_h3", "obj.rune_platebody_h4", "obj.rune_platebody_h5", "obj.trail_saradomin_coif", "obj.trail_saradomin_chest", "obj.trail_saradomin_chaps", "obj.trail_saradomin_vambraces", "obj.blessed_boots_saradomin", "obj.blessed_dhide_shield_saradomin", "obj.trail_guthix_coif", "obj.trail_guthix_chest", "obj.trail_guthix_chaps", "obj.trail_guthix_vambraces", "obj.blessed_boots_guthix", "obj.blessed_dhide_shield_guthix", "obj.trail_zamorak_coif", "obj.trail_zamorak_chest", "obj.trail_zamorak_chaps", "obj.trail_zamorak_vambraces", "obj.blessed_boots_zamorak", "obj.blessed_dhide_shield_zamorak", "obj.trail_bandos_coif", "obj.trail_bandos_chest", "obj.trail_bandos_chaps", "obj.trail_bandos_vambraces", "obj.blessed_boots_bandos", "obj.blessed_dhide_shield_bandos", "obj.trail_armadyl_coif", "obj.trail_armadyl_chest", "obj.trail_armadyl_chaps", "obj.trail_armadyl_vambraces", "obj.blessed_boots_armadyl", "obj.blessed_dhide_shield_armadyl", "obj.trail_ancient_coif", "obj.trail_ancient_chest", "obj.trail_ancient_chaps", "obj.trail_ancient_vambraces", "obj.blessed_boots_ancient", "obj.blessed_dhide_shield_ancient", "obj.red_dragonhide_body_trim", "obj.red_dragonhide_chaps_trim", "obj.red_dragonhide_body_gold", "obj.red_dragonhide_chaps_gold", "obj.blue_dragonhide_body_trim", "obj.blue_dragonhide_chaps_trim", "obj.blue_dragonhide_body_trim_gold", "obj.blue_dragonhide_chaps_trim_gold", "obj.enchanted_hat", "obj.enchanted_robetop", "obj.enchanted_robelegs", "obj.trail_saradomin_scarf", "obj.trail_saradomin_staff", "obj.trail_guthix_scarf", "obj.trail_guthix_staff", "obj.trail_zamorak_scarf", "obj.trail_zamorak_staff", "obj.zombie_head", "obj.cyclops_mask", "obj.piratehat", "obj.cavalier_red", "obj.cavalier_white", "obj.cavalier_navy", "obj.cavalier_brown", "obj.cavalier_dark", "obj.cavalier_black", "obj.pith_helmet", "obj.explorer_backpack", "obj.thieving_bag", "obj.dragonmask_green", "obj.dragonmask_blue", "obj.dragonmask_red", "obj.dragonmask_black", "obj.nunchucks", "obj.dual_sai", "obj.rune_cane", "obj.trail_amulet_of_glory_4", "obj.trail_composite_bow_magic")
            column(CATEGORY, "Clues")
            column(TAB_INDEX, 3)
        }

        row("dbrow.collection_log_category_elite_treasure_trails") {
            column(NAME, "Elite Treasure Trails")
            columnRSCM(COMPLETED_VARBIT, "varbit.collection_clues_elite_completed")
            columnRSCM(ITEMS, "obj.ring_of_3rd_age", "obj.fury_kit", "obj.dragon_chainbody_kit", "obj.dragon_platelegs_kit", "obj.dragon_sq_shield_kit", "obj.dragon_full_helm_kit", "obj.dragon_scimitar_ornament_kit", "obj.light_inf_kit", "obj.dark_inf_kit", "obj.holy_wraps", "obj.ranger_gloves", "obj.ranger_tunic", "obj.rangers_tights", "obj.black_dragonhide_body_gold", "obj.black_dragonhide_chaps_gold", "obj.black_dragonhide_body_trim", "obj.black_dragonhide_chaps_trim", "obj.royal_crown", "obj.royal_sceptre", "obj.royal_top", "obj.royal_bottom", "obj.musketeer_hat", "obj.musketeer_top", "obj.musketeer_legs", "obj.tuxedo_body", "obj.tuxedo_legs", "obj.tuxedo_feet", "obj.tuxedo_hands", "obj.tuxedo_bowtie", "obj.tuxedo_body_white", "obj.tuxedo_legs_white", "obj.tuxedo_feet_white", "obj.tuxedo_hands_white", "obj.tuxedo_bowtie_white", "obj.zeah_scarf_arceuus", "obj.zeah_scarf_hosidius", "obj.zeah_scarf_piscarilius", "obj.zeah_scarf_shayzien", "obj.zeah_scarf_lovakengj", "obj.dragonmask_bronze", "obj.dragonmask_iron", "obj.dragonmask_steel", "obj.dragonmask_mith", "obj.dragonmask_adamant", "obj.dragonmask_rune", "obj.katana", "obj.dragon_cane", "obj.trail_briefcase", "obj.bucket_helm", "obj.blacksmith_helm", "obj.deerstalker", "obj.afro", "obj.trail_pirate_hat", "obj.top_hat", "obj.monacle", "obj.wise_spectacles", "obj.fremennik_kilt", "obj.giant_boot", "obj.uris_hat")
            column(CATEGORY, "Clues")
            column(TAB_INDEX, 4)
        }

        row("dbrow.collection_log_category_master_treasure_trails") {
            column(NAME, "Master Treasure Trails")
            columnRSCM(COMPLETED_VARBIT, "varbit.collection_clues_master_completed")
            columnRSCM(ITEMS, "obj.bloodhound_pet", "obj.ring_of_3rd_age", "obj.ags_ornament_kit", "obj.bgs_ornament_kit", "obj.sgs_ornament_kit", "obj.zgs_ornament_kit", "obj.occult_kit", "obj.torture_kit", "obj.anguish_kit", "obj.dd_ornament_kit", "obj.dragon_kiteshield_kit", "obj.dragon_platebody_kit", "obj.tormented_kit", "obj.robe_darkness_head", "obj.robe_darkness_top", "obj.robe_darkness_legs", "obj.robe_darkness_hands", "obj.robe_darkness_feet", "obj.samurai_hat", "obj.samurai_top", "obj.samurai_legs", "obj.samurai_boots", "obj.samurai_gloves", "obj.ankou_head", "obj.ankou_body", "obj.ankou_hands", "obj.ankou_feet", "obj.ankou_legs", "obj.mummy_head", "obj.mummy_feet", "obj.mummy_hands", "obj.mummy_legs", "obj.mummy_body", "obj.zeah_hood_shayzien", "obj.zeah_hood_hosidius", "obj.zeah_hood_arceuus", "obj.zeah_hood_piscarilius", "obj.zeah_hood_lovakengj", "obj.lesser_demon_mask", "obj.greater_demon_mask", "obj.black_demon_mask", "obj.jungle_demon_mask", "obj.old_demon_mask", "obj.eye_patch_02", "obj.the_bowl", "obj.ale_gods", "obj.tzhaar_cape_obsidian_r", "obj.half_moon_spectacles", "obj.fancy_tiara")
            column(CATEGORY, "Clues")
            column(TAB_INDEX, 5)
        }

        row("dbrow.collection_log_category_hard_treasure_trails_rare") {
            column(NAME, "Hard Treasure Trails (Rare)")
            columnRSCM(COMPLETED_VARBIT, "varbit.collection_clues_hard_megarare_completed")
            columnRSCM(ITEMS, "obj.trail_ranger_coif", "obj.trail_ranger_torso", "obj.trail_ranger_legs", "obj.trail_ranger_vambraces", "obj.trail_mage_torso", "obj.trail_mage_legs", "obj.trail_mage_hat", "obj.trail_mage_amulet", "obj.trail_silver_plate_skirt", "obj.trail_silver_plate_legs", "obj.trail_silver_plate_chest", "obj.trail_fighter_helm", "obj.trail_fighter_shield", "obj.rune_platebody_goldplate", "obj.rune_platelegs_goldplate", "obj.rune_plateskirt_goldplate", "obj.rune_full_helm_goldplate", "obj.rune_kiteshield_goldplate", "obj.rune_med_helm_gold", "obj.rune_chainbody_gold", "obj.rune_sq_shield_gold", "obj.rune_2h_sword_gold", "obj.rune_spear_gold", "obj.brut_rune_spear_gold")
            column(CATEGORY, "Clues")
            column(TAB_INDEX, 6)
        }

        row("dbrow.collection_log_category_elite_treasure_trails_rare") {
            column(NAME, "Elite Treasure Trails (Rare)")
            columnRSCM(COMPLETED_VARBIT, "varbit.collection_clues_elite_megarare_completed")
            columnRSCM(ITEMS, "obj.trail_fighter_sword", "obj.trail_mage_wand", "obj.trail_third_cape", "obj.trail_ranger_bow", "obj.trail_ranger_coif", "obj.trail_ranger_torso", "obj.trail_ranger_legs", "obj.trail_ranger_vambraces", "obj.trail_mage_torso", "obj.trail_mage_legs", "obj.trail_mage_hat", "obj.trail_mage_amulet", "obj.trail_silver_plate_skirt", "obj.trail_silver_plate_legs", "obj.trail_silver_plate_chest", "obj.trail_fighter_helm", "obj.trail_fighter_shield", "obj.rune_scimitar_gold", "obj.rune_boots_gold", "obj.rune_platebody_goldplate", "obj.rune_platelegs_goldplate", "obj.rune_plateskirt_goldplate", "obj.rune_full_helm_goldplate", "obj.rune_kiteshield_goldplate", "obj.rune_med_helm_gold", "obj.rune_chainbody_gold", "obj.rune_sq_shield_gold", "obj.rune_2h_sword_gold", "obj.rune_spear_gold", "obj.brut_rune_spear_gold", "obj.trail_gilded_dhide_coif", "obj.trail_gilded_dhide_vambraces", "obj.trail_gilded_dhide_top", "obj.trail_gilded_dhide_chaps", "obj.trail_gilded_pickaxe", "obj.trail_gilded_axe", "obj.trail_gilded_spade", "obj.ring_of_nature", "obj.dragonmask_lava")
            column(CATEGORY, "Clues")
            column(TAB_INDEX, 7)
        }

        row("dbrow.collection_log_category_master_treasure_trails_rare") {
            column(NAME, "Master Treasure Trails (Rare)")
            columnRSCM(COMPLETED_VARBIT, "varbit.collection_clues_master_megarare_completed")
            columnRSCM(ITEMS, "obj.3a_pickaxe", "obj.3a_axe", "obj.trail_fighter_sword", "obj.trail_mage_wand", "obj.trail_third_cape", "obj.trail_ranger_bow", "obj.trail_ranger_coif", "obj.trail_ranger_torso", "obj.trail_ranger_legs", "obj.trail_ranger_vambraces", "obj.trail_mage_torso", "obj.trail_mage_legs", "obj.trail_mage_hat", "obj.trail_mage_amulet", "obj.trail_silver_plate_skirt", "obj.trail_silver_plate_legs", "obj.trail_silver_plate_chest", "obj.trail_fighter_helm", "obj.trail_fighter_shield", "obj.3a_druidic_bottoms", "obj.3a_druidic_top", "obj.3a_druidic_staff", "obj.3a_druidic_cloak", "obj.rune_scimitar_gold", "obj.rune_boots_gold", "obj.rune_platebody_goldplate", "obj.rune_platelegs_goldplate", "obj.rune_plateskirt_goldplate", "obj.rune_full_helm_goldplate", "obj.rune_kiteshield_goldplate", "obj.rune_med_helm_gold", "obj.rune_chainbody_gold", "obj.rune_sq_shield_gold", "obj.rune_2h_sword_gold", "obj.rune_spear_gold", "obj.brut_rune_spear_gold", "obj.trail_gilded_dhide_coif", "obj.trail_gilded_dhide_vambraces", "obj.trail_gilded_dhide_top", "obj.trail_gilded_dhide_chaps", "obj.trail_gilded_pickaxe", "obj.trail_gilded_axe", "obj.trail_gilded_spade", "obj.bucket_helm_gold", "obj.ring_of_coins")
            column(CATEGORY, "Clues")
            column(TAB_INDEX, 8)
        }

        row("dbrow.collection_log_category_shared_treasure_trail_rewards") {
            column(NAME, "Shared Treasure Trail Rewards")
            columnRSCM(COMPLETED_VARBIT, "varbit.collection_clues_shared_completed")
            columnRSCM(ITEMS, "obj.holy_book_s_page1", "obj.holy_book_z_page1", "obj.holy_book_g_page1", "obj.bandos_page1", "obj.armadyl_page1", "obj.zaros_page1", "obj.holy_book_s_page2", "obj.holy_book_z_page2", "obj.holy_book_g_page2", "obj.bandos_page2", "obj.armadyl_page2", "obj.zaros_page2", "obj.holy_book_s_page3", "obj.holy_book_z_page3", "obj.holy_book_g_page3", "obj.bandos_page3", "obj.armadyl_page3", "obj.zaros_page3", "obj.holy_book_s_page4", "obj.holy_book_z_page4", "obj.holy_book_g_page4", "obj.bandos_page4", "obj.armadyl_page4", "obj.zaros_page4", "obj.blessing_saradomin", "obj.blessing_zamorak", "obj.blessing_guthix", "obj.blessing_bandos", "obj.blessing_armadyl", "obj.blessing_zaros", "obj.teleportscroll_nardah", "obj.teleportscroll_mosles", "obj.teleportscroll_mortton", "obj.teleportscroll_feldip", "obj.teleportscroll_lunarisle", "obj.teleportscroll_digsite", "obj.teleportscroll_piscatoris", "obj.teleportscroll_pestcontrol", "obj.teleportscroll_taibwo", "obj.teleportscroll_lumberyard", "obj.teleportscroll_elf", "obj.bookofscrolls_empty", "obj.gnomish_firelighter_red", "obj.gnomish_firelighter_green", "obj.gnomish_firelighter_blue", "obj.trail_gnomish_firelighter_purple", "obj.trail_gnomish_firelighter_white", "obj.scroll_charge_dragonstone", "obj.trail_sweets")
            column(CATEGORY, "Clues")
            column(TAB_INDEX, 9)
        }

        row("dbrow.collection_log_category_scroll_cases") {
            column(NAME, "Scroll Cases")
            columnRSCM(COMPLETED_VARBIT, "varbit.collection_clues_scroll_cases_completed")
            columnRSCM(ITEMS, "obj.scroll_case_beginner_minor", "obj.scroll_case_beginner_major", "obj.scroll_case_easy_minor", "obj.scroll_case_easy_major", "obj.scroll_case_medium_minor", "obj.scroll_case_medium_major", "obj.scroll_case_hard_minor", "obj.scroll_case_hard_major", "obj.scroll_case_elite_minor", "obj.scroll_case_elite_major", "obj.scroll_case_master_minor", "obj.scroll_case_master_major", "obj.scroll_case_mimic")
            column(CATEGORY, "Clues")
            column(TAB_INDEX, 10)
        }

        row("dbrow.collection_log_category_barbarian_assault") {
            column(NAME, "Barbarian Assault")
            columnRSCM(COMPLETED_VARBIT, "varbit.collection_minigames_ba_completed")
            columnRSCM(ITEMS, "obj.penancepet", "obj.barbassault_penance_fighter_hat", "obj.barbassault_penance_ranger_hat", "obj.barbassault_penance_runner_hat", "obj.barbassault_penance_healer_hat", "obj.barbassault_penance_fighter_torso", "obj.barbassault_penance_ranger_legs", "obj.barbassault_penance_runner_boots", "obj.barbassault_penance_gloves", "obj.granite_helm", "obj.granite_body")
            column(CATEGORY, "Minigames")
            column(TAB_INDEX, 0)
        }

        row("dbrow.collection_log_category_barracuda_trials") {
            column(NAME, "Barracuda Trials")
            columnRSCM(COMPLETED_VARBIT, "varbit.collection_minigames_barracuda_trials_completed")
            columnRSCM(ITEMS, "obj.stormy_key", "obj.barrel_stand", "obj.ralphs_fabric_roll", "obj.fetid_key", "obj.captured_wind_mote", "obj.gurtobs_fabric_roll", "obj.serrated_key", "obj.heart_of_ithell", "obj.gwynas_fabric_roll")
            column(CATEGORY, "Minigames")
            column(TAB_INDEX, 1)
        }

        row("dbrow.collection_log_category_brimhaven_agility_arena") {
            column(NAME, "Brimhaven Agility Arena")
            columnRSCM(COMPLETED_VARBIT, "varbit.collection_other_brimhaven_agility_completed")
            columnRSCM(ITEMS, "obj.agilityarena_ticket", "obj.agilityarena_voucher", "obj.piratehook", "obj.graceful_hood_skillcapecolour", "obj.graceful_top_skillcapecolour", "obj.graceful_legs_skillcapecolour", "obj.graceful_gloves_skillcapecolour", "obj.graceful_boots_skillcapecolour", "obj.graceful_cape_skillcapecolour")
            column(CATEGORY, "Minigames")
            column(TAB_INDEX, 2)
        }

        row("dbrow.collection_log_category_castle_wars") {
            column(NAME, "Castle Wars")
            columnRSCM(COMPLETED_VARBIT, "varbit.collection_minigames_castle_completed")
            columnRSCM(ITEMS, "obj.castlewars_med_helm", "obj.castlewars_full_helm", "obj.castlewars_armour_body", "obj.castlewars_sword", "obj.castlewars_shield", "obj.castlewars_armour_legs", "obj.castlewars_armour_skirt", "obj.castlewars_boots", "obj.castlewars_med_helm_2", "obj.castlewars_full_helm_2", "obj.castlewars_armour_body_2", "obj.castlewars_sword_2", "obj.castlewars_shield_2", "obj.castlewars_armour_legs_2", "obj.castlewars_armour_skirt_2", "obj.castlewars_boots_2", "obj.castlewars_med_helm_3", "obj.castlewars_full_helm_3", "obj.castlewars_armour_body_3", "obj.castlewars_sword_3", "obj.castlewars_shield_3", "obj.castlewars_armour_legs_3", "obj.castlewars_armour_skirt_3", "obj.castlewars_boots_3", "obj.castlewars_hood_saradomin_prize", "obj.castlewars_cloak_saradomin_prize", "obj.castlewars_hood_zamorak_prize", "obj.castlewars_cloak_zamorak_prize", "obj.saradomin_reward_banner", "obj.zamorak_reward_banner", "obj.castlewars_mage_hat", "obj.castlewars_mage_top", "obj.castlewars_mage_legs", "obj.castlewars_range_top", "obj.castlewars_range_legs", "obj.castlewars_range_quiver", "obj.castlewars_saradomin_halo", "obj.castlewars_zamorak_halo", "obj.castlewars_guthix_halo")
            column(CATEGORY, "Minigames")
            column(TAB_INDEX, 3)
        }

        row("dbrow.collection_log_category_fishing_trawler") {
            column(NAME, "Fishing Trawler")
            columnRSCM(COMPLETED_VARBIT, "varbit.collection_minigames_trawler_completed")
            columnRSCM(ITEMS, "obj.trawler_reward_hat", "obj.trawler_reward_top", "obj.trawler_reward_legs", "obj.trawler_reward_boots")
            column(CATEGORY, "Minigames")
            column(TAB_INDEX, 4)
        }

        row("dbrow.collection_log_category_giants_foundry") {
            column(NAME, "Giants' Foundry")
            columnRSCM(COMPLETED_VARBIT, "varbit.collection_minigames_giantsfoundry_completed")
            columnRSCM(ITEMS, "obj.smithing_uniform_torso", "obj.smithing_uniform_legs", "obj.smithing_uniform_boots", "obj.smithing_uniform_gloves", "obj.giants_foundry_colossal_blade", "obj.double_ammo_mould", "obj.kovacs_grog", "obj.smithing_catalyst", "obj.giants_foundry_ore_pack")
            column(CATEGORY, "Minigames")
            column(TAB_INDEX, 5)
        }

        row("dbrow.collection_log_category_gnome_restaurant") {
            column(NAME, "Gnome Restaurant")
            columnRSCM(COMPLETED_VARBIT, "varbit.collection_minigames_gnome_completed")
            columnRSCM(ITEMS, "obj.aluft_seed_pod", "obj.aluft_gnome_scarf", "obj.aluft_gnome_goggles", "obj.aluft_gnome_mint_cake")
            column(CATEGORY, "Minigames")
            column(TAB_INDEX, 6)
        }

        row("dbrow.collection_log_category_guardians_of_the_rift") {
            column(NAME, "Guardians of the Rift")
            columnRSCM(COMPLETED_VARBIT, "varbit.collection_minigames_gotr_completed")
            columnRSCM(ITEMS, "obj.abyssalpet", "obj.abyssal_pearl", "obj.catalytic_talisman", "obj.abyssal_needle", "obj.abyssal_green_dye", "obj.abyssal_blue_dye", "obj.abyssal_red_dye", "obj.hat_of_the_eye", "obj.robe_top_of_the_eye", "obj.robe_bottom_of_the_eye", "obj.boots_of_the_eye", "obj.ring_of_elements", "obj.abyssal_lantern", "obj.guardians_eye", "obj.gotr_intricate_pouch", "obj.gotr_lost_bag", "obj.gotr_tarnished_locket")
            column(CATEGORY, "Minigames")
            columnRSCM(COUNT_VARP_1, "varp.total_gotr_kills")
            column(TAB_INDEX, 7)
        }

        row("dbrow.collection_log_category_hallowed_sepulchre") {
            column(NAME, "Hallowed Sepulchre")
            columnRSCM(COMPLETED_VARBIT, "varbit.collection_minigames_hallowed_sepulchre_completed")
            columnRSCM(ITEMS, "obj.hallowed_mark", "obj.hallowed_token", "obj.hallowed_grapple", "obj.hallowed_focus", "obj.hallowed_symbol", "obj.hallowed_hammer", "obj.hallowed_ring", "obj.dark_dye", "obj.dark_acorn", "obj.strange_old_lockpick_full", "obj.ring_of_endurance_nocharges", "obj.hallowed_floor1_page", "obj.hallowed_floor2_page", "obj.hallowed_floor3_page", "obj.hallowed_floor4_page", "obj.hallowed_floor5_page")
            column(CATEGORY, "Minigames")
            column(TAB_INDEX, 8)
        }

        row("dbrow.collection_log_category_last_man_standing") {
            column(NAME, "Last Man Standing")
            columnRSCM(COMPLETED_VARBIT, "varbit.collection_minigames_lms_completed")
            columnRSCM(ITEMS, "obj.br_deadman_body", "obj.br_deadman_legs", "obj.br_deadman_cape", "obj.armadyl_halo", "obj.bandos_halo", "obj.seren_halo", "obj.zaros_halo", "obj.brassica_halo", "obj.bh_ags_spec", "obj.bh_bgs_spec", "obj.bh_sgs_spec", "obj.bh_zgs_spec", "obj.br_cape_1", "obj.br_cape_10", "obj.br_cape_50", "obj.br_cape_100", "obj.br_cape_500", "obj.br_cape_1000", "obj.granite_clamp", "obj.granite_maul_upgrade", "obj.steam_staff_upgrade_kit", "obj.lava_staff_upgrade_kit", "obj.dragon_pickaxe_upgrade_kit", "obj.ward_upgrade_kit", "obj.bh_green_paint", "obj.bh_yellow_paint", "obj.bh_white_paint", "obj.bh_blue_paint", "obj.bh_lava_paint", "obj.bh_ice_paint", "obj.icon_of_guthix", "obj.swift_blade")
            column(CATEGORY, "Minigames")
            column(TAB_INDEX, 9)
        }

        row("dbrow.collection_log_category_magic_training_arena") {
            column(NAME, "Magic Training Arena")
            columnRSCM(COMPLETED_VARBIT, "varbit.collection_minigames_mta_completed")
            columnRSCM(ITEMS, "obj.magictraining_wand_beg", "obj.magictraining_wand_appr", "obj.magictraining_wand_teach", "obj.magictraining_wand_master", "obj.magictraining_infinityhat", "obj.magictraining_infinitytop", "obj.magictraining_infinitybottom", "obj.magictraining_infinityboots", "obj.magictraining_infinitygloves", "obj.magictraining_bookofmagic", "obj.magictraining_peachspell")
            column(CATEGORY, "Minigames")
            column(TAB_INDEX, 10)
        }

        row("dbrow.collection_log_category_mahogany_homes") {
            column(NAME, "Mahogany Homes")
            columnRSCM(COMPLETED_VARBIT, "varbit.collection_minigames_constructioncontracts_completed")
            columnRSCM(ITEMS, "obj.construction_supply_crate", "obj.construction_hat", "obj.construction_shirt", "obj.construction_trousers", "obj.construction_boots", "obj.wearable_saw", "obj.plank_sack", "obj.hosidius_blueprints")
            column(CATEGORY, "Minigames")
            column(TAB_INDEX, 11)
        }

        row("dbrow.collection_log_category_mastering_mixology") {
            column(NAME, "Mastering Mixology")
            columnRSCM(COMPLETED_VARBIT, "varbit.collection_minigames_mastering_mixology_completed")
            columnRSCM(ITEMS, "obj.mm_alchemist_hat", "obj.mm_alchemist_body", "obj.mm_alchemist_legs", "obj.mm_alchemist_gloves", "obj.amulet_of_chemistry_imbued_charged", "obj.mm_secondary_pouch", "obj.mm_prepot_device_disassembled")
            column(CATEGORY, "Minigames")
            column(TAB_INDEX, 12)
        }

        row("dbrow.collection_log_category_pest_control") {
            column(NAME, "Pest Control")
            columnRSCM(COMPLETED_VARBIT, "varbit.collection_minigames_pest_completed")
            columnRSCM(ITEMS, "obj.pest_void_knight_mace", "obj.pest_void_knight_top", "obj.pest_void_knight_robes", "obj.pest_void_knight_gloves", "obj.game_pest_mage_helm", "obj.game_pest_melee_helm", "obj.game_pest_archer_helm", "obj.pest_seal_8", "obj.elite_void_knight_top", "obj.elite_void_knight_robes")
            column(CATEGORY, "Minigames")
            column(TAB_INDEX, 13)
        }

        row("dbrow.collection_log_category_rogues_den") {
            column(NAME, "Rogues' Den")
            columnRSCM(COMPLETED_VARBIT, "varbit.collection_minigames_rogues_completed")
            columnRSCM(ITEMS, "obj.roguesden_helm", "obj.roguesden_body", "obj.roguesden_legs", "obj.roguesden_boots", "obj.roguesden_gloves")
            column(CATEGORY, "Minigames")
            column(TAB_INDEX, 14)
        }

        row("dbrow.collection_log_category_shades_of_mort_ton") {
            column(NAME, "Shades of Mort'ton")
            columnRSCM(COMPLETED_VARBIT, "varbit.collection_minigames_shades_completed")
            columnRSCM(ITEMS, "obj.damned_amulet", "obj.flamtaer_bag", "obj.fine_cloth", "obj.shades_lock_bronze", "obj.shades_lock_steel", "obj.shades_lock_black", "obj.shades_lock_silver", "obj.shades_lock_gold", "obj.shades_prayer_helm", "obj.shades_prayer_top", "obj.shades_prayer_bottom", "obj.shades_prayer_boots", "obj.shades_swamp_diary", "obj.shades_blood_diary")
            column(CATEGORY, "Minigames")
            column(TAB_INDEX, 15)
        }

        row("dbrow.collection_log_category_soul_wars") {
            column(NAME, "Soul Wars")
            columnRSCM(COMPLETED_VARBIT, "varbit.collection_minigames_soul_wars_completed")
            columnRSCM(ITEMS, "obj.soulwarspet_blue", "obj.soul_cape_blue", "obj.soul_wars_ectoplasmator")
            column(CATEGORY, "Minigames")
            column(TAB_INDEX, 16)
        }

        row("dbrow.collection_log_category_temple_trekking") {
            column(NAME, "Temple Trekking")
            columnRSCM(COMPLETED_VARBIT, "varbit.collection_minigames_temple_completed")
            columnRSCM(ITEMS, "obj.ramble_lumberjack_hat", "obj.ramble_lumberjack_top", "obj.ramble_lumberjack_legs", "obj.ramble_lumberjack_boots")
            column(CATEGORY, "Minigames")
            column(TAB_INDEX, 17)
        }

        row("dbrow.collection_log_category_tithe_farm") {
            column(NAME, "Tithe Farm")
            columnRSCM(COMPLETED_VARBIT, "varbit.collection_minigames_tithe_completed")
            columnRSCM(ITEMS, "obj.tithe_reward_hat_male", "obj.tithe_reward_torso_male", "obj.tithe_reward_legs_male", "obj.tithe_reward_feet_male", "obj.seed_box", "obj.zeah_wateringcan", "obj.slayer_herb_sack")
            column(CATEGORY, "Minigames")
            column(TAB_INDEX, 18)
        }

        row("dbrow.collection_log_category_trouble_brewing") {
            column(NAME, "Trouble Brewing")
            columnRSCM(COMPLETED_VARBIT, "varbit.collection_minigames_brewing_completed")
            columnRSCM(ITEMS, "obj.brew_uniform_blue", "obj.brew_tricorn_blue", "obj.brew_navy_slacks_blue", "obj.brew_uniform_green", "obj.brew_tricorn_green", "obj.brew_navy_slacks_green", "obj.brew_uniform_red", "obj.brew_tricorn_red", "obj.brew_navy_slacks_red", "obj.brew_uniform_brown", "obj.brew_tricorn_brown", "obj.brew_navy_slacks_brown", "obj.brew_uniform_black", "obj.brew_tricorn_black", "obj.brew_navy_slacks_black", "obj.brew_uniform_purple", "obj.brew_tricorn_purple", "obj.brew_navy_slacks_purple", "obj.brew_uniform_grey", "obj.brew_tricorn_grey", "obj.brew_navy_slacks_grey", "obj.brew_flag_1", "obj.brew_flag_2", "obj.brew_flag_3", "obj.brew_flag_4", "obj.brew_flag_5", "obj.brew_flag_6", "obj.brew_hyper_yeast", "obj.brew_red_rum", "obj.brew_blue_rum")
            column(CATEGORY, "Minigames")
            column(TAB_INDEX, 19)
        }

        row("dbrow.collection_log_category_vale_totems") {
            column(NAME, "Vale Totems")
            columnRSCM(COMPLETED_VARBIT, "varbit.collection_other_vale_totems_completed")
            columnRSCM(ITEMS, "obj.fletching_knife", "obj.bowstring_spool", "obj.ent_branch", "obj.greenman_mask")
            column(CATEGORY, "Minigames")
            column(TAB_INDEX, 20)
        }

        row("dbrow.collection_log_category_volcanic_mine") {
            column(NAME, "Volcanic Mine")
            columnRSCM(COMPLETED_VARBIT, "varbit.collection_other_volcanic_mine_completed")
            columnRSCM(ITEMS, "obj.fossil_mine_ultrasoil_book", "obj.fossil_mine_water_container_dummy", "obj.fossil_tablet_volcanoteleport", "obj.wbr_dragon_pickaxe_broken", "obj.fossil_motherlode_reward_hat", "obj.fossil_motherlode_reward_top", "obj.fossil_motherlode_reward_legs", "obj.fossil_motherlode_reward_boots")
            column(CATEGORY, "Minigames")
            column(TAB_INDEX, 21)
        }

        row("dbrow.collection_log_category_aerial_fishing") {
            column(NAME, "Aerial Fishing")
            columnRSCM(COMPLETED_VARBIT, "varbit.collection_other_aerial_fishing_completed")
            columnRSCM(ITEMS, "obj.golden_tench", "obj.fishingrod_pearl", "obj.fishingrod_pearl_fly", "obj.fishingrod_pearl_brut", "obj.fish_sack", "obj.trawler_reward_hat", "obj.trawler_reward_top", "obj.trawler_reward_legs", "obj.trawler_reward_boots")
            column(CATEGORY, "Other")
            column(TAB_INDEX, 0)
        }

        row("dbrow.collection_log_category_all_pets") {
            column(NAME, "All Pets")
            columnRSCM(COMPLETED_VARBIT, "varbit.collection_other_pets_completed")
            columnRSCM(ITEMS, "obj.abyssalsire_pet", "obj.hydrapet", "obj.callisto_pet", "obj.hell_pet", "obj.chaoselepet", "obj.saradominpet", "obj.corepet", "obj.primepet", "obj.supremepet", "obj.rexpet", "obj.jad_pet", "obj.bandospet", "obj.molepet", "obj.dawnpet", "obj.infernopet", "obj.kqpet_walking", "obj.kbdpet", "obj.krakenpet", "obj.armadylpet", "obj.zamorakpet", "obj.scorpia_pet", "obj.skotizopet", "obj.smokepet", "obj.venenatis_pet", "obj.vetion_pet", "obj.vorkathpet", "obj.phoenixpet", "obj.snakepet", "obj.olmpet", "obj.verzikpet", "obj.bloodhound_pet", "obj.penancepet", "obj.skillpetfish", "obj.skillpetmining", "obj.skillpetwc", "obj.skillpethunter_grey", "obj.skillpetagility", "obj.skillpetfarming", "obj.skillpetthieving", "obj.skillpetrunecrafting_fire", "obj.herbiboarpet", "obj.chompybird_pet", "obj.sarachnispet", "obj.zalcanopet", "obj.gauntletpet", "obj.nightmarepet", "obj.soulwarspet_blue", "obj.temporosspet", "obj.nexpet", "obj.abyssalpet", "obj.wardenpet_tumeken", "obj.muspahpet", "obj.whispererpet", "obj.dukesucelluspet", "obj.vardorvispet", "obj.leviathanpet", "obj.scurriuspet", "obj.solhereditpet", "obj.quetzalpet", "obj.araxxorpet", "obj.hueypet", "obj.amoxliatlpet", "obj.rtbrandapet", "obj.yamapet", "obj.dompet", "obj.skillpetsailing", "obj.gryphonbosspet", "obj.cowbosspet", "obj.maggotkingpet")
            column(CATEGORY, "Other")
            column(TAB_INDEX, 1)
        }

        row("dbrow.collection_log_category_boat_paints") {
            column(NAME, "Boat Paints")
            columnRSCM(COMPLETED_VARBIT, "varbit.collection_other_sailing_paint_completed")
            columnRSCM(ITEMS, "obj.sailing_paint_barracuda", "obj.sailing_paint_shark", "obj.sailing_paint_inky", "obj.sailing_paint_anglers", "obj.sailing_paint_salvors", "obj.sailing_paint_armadylean", "obj.sailing_paint_zamorakian", "obj.sailing_paint_guthixian", "obj.sailing_paint_saradominist", "obj.sailing_paint_merchants", "obj.sailing_paint_sandy")
            column(CATEGORY, "Other")
            column(TAB_INDEX, 2)
        }

        row("dbrow.collection_log_category_camdozaal") {
            column(NAME, "Camdozaal")
            columnRSCM(COMPLETED_VARBIT, "varbit.collection_other_camdozaal_completed")
            columnRSCM(ITEMS, "obj.barronite_mace", "obj.barronite_mace_1", "obj.barronite_mace_2", "obj.barronite_mace_3", "obj.camdozaal_relic_1", "obj.camdozaal_relic_2", "obj.camdozaal_relic_3", "obj.camdozaal_relic_4", "obj.camdozaal_relic_5", "obj.imcando_hammer")
            column(CATEGORY, "Other")
            column(TAB_INDEX, 3)
        }

        row("dbrow.collection_log_category_champion_s_challenge") {
            column(NAME, "Champion's Challenge")
            columnRSCM(COMPLETED_VARBIT, "varbit.collection_other_champions_completed")
            columnRSCM(ITEMS, "obj.champions_challenge_earthwarrior", "obj.champions_challenge_ghoul", "obj.champions_challenge_giant", "obj.champions_challenge_goblin", "obj.champions_challenge_hobgoblin", "obj.champions_challenge_imp", "obj.champions_challenge_jogre", "obj.champions_challenge_lesserdemon", "obj.champions_challenge_skeleton", "obj.champions_challenge_zombie", "obj.champion_cape")
            column(CATEGORY, "Other")
            column(TAB_INDEX, 4)
        }

        row("dbrow.collection_log_category_chompy_bird_hunting") {
            column(NAME, "Chompy Bird Hunting")
            columnRSCM(COMPLETED_VARBIT, "varbit.collection_other_chompy_completed")
            columnRSCM(ITEMS, "obj.chompybird_pet", "obj.cbhat1", "obj.cbhat2", "obj.cbhat3", "obj.cbhat4", "obj.cbhat5", "obj.cbhat6", "obj.cbhat7", "obj.cbhat8", "obj.cbhat9", "obj.cbhat10", "obj.cbhat11", "obj.cbhat12", "obj.cbhat13", "obj.cbhat14", "obj.cbhat15", "obj.cbhat16", "obj.cbhat17", "obj.cbhat18")
            column(CATEGORY, "Other")
            column(TAB_INDEX, 5)
        }

        row("dbrow.collection_log_category_colossal_wyrm_agility") {
            column(NAME, "Colossal Wyrm Agility")
            columnRSCM(COMPLETED_VARBIT, "varbit.collection_other_colossal_wyrm_agility_completed")
            columnRSCM(ITEMS, "obj.teleportscroll_colossal_wyrm", "obj.calcified_acorn", "obj.graceful_hood_wyrm", "obj.graceful_top_wyrm", "obj.graceful_legs_wyrm", "obj.graceful_gloves_wyrm", "obj.graceful_boots_wyrm", "obj.graceful_cape_wyrm")
            column(CATEGORY, "Other")
            column(TAB_INDEX, 6)
        }

        row("dbrow.collection_log_category_creature_creation") {
            column(NAME, "Creature Creation")
            columnRSCM(COMPLETED_VARBIT, "varbit.collection_other_creature_creation_completed")
            columnRSCM(ITEMS, "obj.tol_tea", "obj.tol_plain_sack", "obj.tol_green_sack", "obj.tol_red_sack", "obj.tol_black_sack", "obj.tol_gold_sack", "obj.tol_rune_sack")
            column(CATEGORY, "Other")
            column(TAB_INDEX, 7)
        }

        row("dbrow.collection_log_category_cyclopes") {
            column(NAME, "Cyclopes")
            columnRSCM(COMPLETED_VARBIT, "varbit.collection_other_cyclopes_completed")
            columnRSCM(ITEMS, "obj.bronze_parryingdagger", "obj.iron_parryingdagger", "obj.steel_parryingdagger", "obj.black_parryingdagger", "obj.mithril_parryingdagger", "obj.adamant_parryingdagger", "obj.rune_parryingdagger", "obj.dragon_parryingdagger")
            column(CATEGORY, "Other")
            column(TAB_INDEX, 8)
        }

        row("dbrow.collection_log_category_elder_chaos_druids") {
            column(NAME, "Elder Chaos Druids")
            columnRSCM(COMPLETED_VARBIT, "varbit.collection_other_chaos_completed")
            columnRSCM(ITEMS, "obj.elderchaos_top", "obj.elderchaos_bottom", "obj.elderchaos_hood")
            column(CATEGORY, "Other")
            column(TAB_INDEX, 9)
        }

        row("dbrow.collection_log_category_forestry") {
            column(NAME, "Forestry")
            columnRSCM(COMPLETED_VARBIT, "varbit.collection_other_forestry_completed")
            columnRSCM(ITEMS, "obj.forestry_fox_pet_whistle", "obj.forestry_pheasant_pet_egg", "obj.ramble_lumberjack_hat", "obj.ramble_lumberjack_top", "obj.ramble_lumberjack_legs", "obj.ramble_lumberjack_boots", "obj.forestry_lumberjack_hat", "obj.forestry_lumberjack_top", "obj.forestry_lumberjack_legs", "obj.forestry_lumberjack_boots", "obj.forestry_gloves", "obj.forestry_funky_shaped_log", "obj.log_basket_closed", "obj.forestry_log_brace", "obj.forestry_clothes_pouch_blueprint", "obj.forestry_cape_pouch", "obj.forestry_2h_axe_handle", "obj.forestry_pheasant_hat", "obj.forestry_pheasant_legs", "obj.forestry_pheasant_boots", "obj.forestry_pheasant_cape", "obj.gathering_event_enchanted_ritual_garland", "obj.forestry_poh_beehive_part")
            column(CATEGORY, "Other")
            column(TAB_INDEX, 10)
        }

        row("dbrow.collection_log_category_fossil_island_notes") {
            column(NAME, "Fossil Island Notes")
            columnRSCM(COMPLETED_VARBIT, "varbit.collection_other_fossil_notes_completed")
            columnRSCM(ITEMS, "obj.fossil_note1", "obj.fossil_note2", "obj.fossil_note3", "obj.fossil_note4", "obj.fossil_note5", "obj.fossil_note6", "obj.fossil_note7", "obj.fossil_note8", "obj.fossil_note9", "obj.fossil_note10")
            column(CATEGORY, "Other")
            column(TAB_INDEX, 11)
        }

        row("dbrow.collection_log_category_glough_s_experiments") {
            column(NAME, "Glough's Experiments")
            columnRSCM(COMPLETED_VARBIT, "varbit.collection_other_gorillas_completed")
            columnRSCM(ITEMS, "obj.zenyte_shard", "obj.ballista_frame_light", "obj.ballista_frame_heavy", "obj.ballista_limbs", "obj.ballista_rope", "obj.ballista_spring")
            column(CATEGORY, "Other")
            column(TAB_INDEX, 12)
        }

        row("dbrow.collection_log_category_hunter_guild") {
            column(NAME, "Hunter Guild")
            columnRSCM(COMPLETED_VARBIT, "varbit.collection_other_hunter_guild_completed")
            columnRSCM(ITEMS, "obj.quetzalpet", "obj.huntsmans_kit", "obj.hg_hunter_hood", "obj.hg_hunter_top", "obj.hg_hunter_legs", "obj.hg_hunter_boots")
            column(CATEGORY, "Other")
            column(TAB_INDEX, 13)
        }

        row("dbrow.collection_log_category_lost_schematics") {
            column(NAME, "Lost Schematics")
            columnRSCM(COMPLETED_VARBIT, "varbit.collection_other_lost_schematics_completed")
            columnRSCM(ITEMS, "obj.lost_schematic_salvaging_station", "obj.lost_schematic_gale_catcher", "obj.lost_schematic_eternal_brazier", "obj.lost_schematic_rosewood_cargohold", "obj.lost_schematic_rosewood_hull", "obj.lost_schematic_rosewood_sail", "obj.lost_schematic_dragon_tiller", "obj.lost_schematic_dragon_keel", "obj.lost_schematic_dragon_salvaging_hook", "obj.lost_schematic_dragon_cannon", "obj.lost_schematic_ballistic_attractor", "obj.lost_schematic_bosuns_workbench")
            column(CATEGORY, "Other")
            column(TAB_INDEX, 14)
        }

        row("dbrow.collection_log_category_monkey_backpacks") {
            column(NAME, "Monkey Backpacks")
            columnRSCM(COMPLETED_VARBIT, "varbit.collection_other_monkey_backpack_completed")
            columnRSCM(ITEMS, "obj.mm2_monkey_karamja", "obj.mm2_monkey_kruk", "obj.mm2_monkey_maniacal", "obj.mm2_monkey_awowogei", "obj.mm2_monkey_skeleton", "obj.mm2_monkey_zombie")
            column(CATEGORY, "Other")
            column(TAB_INDEX, 15)
        }

        row("dbrow.collection_log_category_motherlode_mine") {
            column(NAME, "Motherlode Mine")
            columnRSCM(COMPLETED_VARBIT, "varbit.collection_other_motherlode_completed")
            columnRSCM(ITEMS, "obj.coal_bag", "obj.gem_bag", "obj.motherlode_reward_hat", "obj.motherlode_reward_top", "obj.motherlode_reward_legs", "obj.motherlode_reward_boots")
            column(CATEGORY, "Other")
            column(TAB_INDEX, 16)
        }

        row("dbrow.collection_log_category_my_notes") {
            column(NAME, "My Notes")
            columnRSCM(COMPLETED_VARBIT, "varbit.collection_other_barbarian_notes_completed")
            columnRSCM(ITEMS, "obj.brut_document_0", "obj.brut_document_1", "obj.brut_document_2", "obj.brut_document_3", "obj.brut_document_4", "obj.brut_document_5", "obj.brut_document_6", "obj.brut_document_7", "obj.brut_document_8", "obj.brut_document_9", "obj.brut_document_10", "obj.brut_document_11", "obj.brut_document_12", "obj.brut_document_13", "obj.brut_document_14", "obj.brut_document_15", "obj.brut_document_16", "obj.brut_document_17", "obj.brut_document_18", "obj.brut_document_19", "obj.brut_document_20", "obj.brut_document_21", "obj.brut_document_22", "obj.brut_document_23", "obj.brut_document_24", "obj.brut_document_25")
            column(CATEGORY, "Other")
            column(TAB_INDEX, 17)
        }

        row("dbrow.collection_log_category_ocean_encounters") {
            column(NAME, "Ocean Encounters")
            columnRSCM(COMPLETED_VARBIT, "varbit.collection_other_ocean_encounters_completed")
            columnRSCM(ITEMS, "obj.sailing_chance_encounters_clam_pearl_500", "obj.sailing_chance_encounters_clam_pearl_1000", "obj.sailing_chance_encounters_clam_pearl_5000", "obj.sailing_chance_encounters_clam_pearl_10000", "obj.sailing_chance_encounters_clam_pearl_25000", "obj.sailing_chance_encounters_clam_pearl_50000", "obj.sailing_chance_encounters_clam_pearl_100000", "obj.sailing_chance_encounters_clam_pearl_250000", "obj.sailing_chance_encounters_clam_pearl_500000", "obj.sailing_chance_encounters_clam_pearl_1000000", "obj.sailing_chance_encounters_clam_pearl_2500000")
            column(CATEGORY, "Other")
            column(TAB_INDEX, 18)
        }

        row("dbrow.collection_log_category_random_events") {
            column(NAME, "Random Events")
            columnRSCM(COMPLETED_VARBIT, "varbit.collection_other_random_completed")
            columnRSCM(ITEMS, "obj.drill_top", "obj.drill_bottoms", "obj.drill_helm", "obj.laderhosen_top", "obj.laderhosen_legs", "obj.laderhosen_hat", "obj.macro_digger_shirt", "obj.macro_digger_legs", "obj.macro_digger_mask", "obj.macro_digger_gloves", "obj.macro_digger_boots", "obj.macro_mime_mask", "obj.macro_mime_top", "obj.macro_mime_legs", "obj.macro_mime_gloves", "obj.macro_mime_boots", "obj.macro_frog_token", "obj.stale_baguette", "obj.beekeeper_hat", "obj.beekeeper_top", "obj.beekeeper_legs", "obj.beekeeper_gloves", "obj.beekeeper_boots")
            column(CATEGORY, "Other")
            column(TAB_INDEX, 19)
        }

        row("dbrow.collection_log_category_revenants") {
            column(NAME, "Revenants")
            columnRSCM(COMPLETED_VARBIT, "varbit.collection_other_revenants_completed")
            columnRSCM(ITEMS, "obj.wild_cave_chainmace_uncharged", "obj.wild_cave_bow_uncharged", "obj.wild_cave_sceptre_uncharged", "obj.wild_cave_amulet", "obj.wild_cave_bracelet_uncharged", "obj.wild_cave_obelisk_crystal", "obj.wild_cave_artifact_16000", "obj.wild_cave_artifact_8000", "obj.wild_cave_artifact_4000", "obj.wild_cave_artifact_2000", "obj.wild_cave_artifact_1000", "obj.wild_cave_artifact_500", "obj.teleportscroll_revenants", "obj.wild_cave_shard")
            column(CATEGORY, "Other")
            column(TAB_INDEX, 20)
        }

        row("dbrow.collection_log_category_rooftop_agility") {
            column(NAME, "Rooftop Agility")
            columnRSCM(COMPLETED_VARBIT, "varbit.collection_other_rooftop_completed")
            columnRSCM(ITEMS, "obj.grace", "obj.graceful_hood", "obj.graceful_cape", "obj.graceful_top", "obj.graceful_legs", "obj.graceful_gloves", "obj.graceful_boots")
            column(CATEGORY, "Other")
            column(TAB_INDEX, 21)
        }

        row("dbrow.collection_log_category_sailing_miscellaneous") {
            column(NAME, "Sailing Miscellaneous")
            columnRSCM(COMPLETED_VARBIT, "varbit.collection_other_sailing_misc_completed")
            columnRSCM(ITEMS, "obj.dragon_sheet", "obj.nails_dragon", "obj.dragon_cannonball", "obj.echo_pearl", "obj.swift_albatross_feather", "obj.narwhal_horn", "obj.ray_barbs", "obj.broken_dragon_hook", "obj.bottled_storm", "obj.dragon_cannon_barrel", "obj.sailing_boat_bottle_empty", "obj.sailing_facility_bottle_empty")
            column(CATEGORY, "Other")
            column(TAB_INDEX, 22)
        }

        row("dbrow.collection_log_category_sea_treasures") {
            column(NAME, "Sea Treasures")
            columnRSCM(COMPLETED_VARBIT, "varbit.collection_other_sea_treasures_completed")
            columnRSCM(ITEMS, "obj.motd_frag_1", "obj.motd_frag_2", "obj.motd_frag_3", "obj.motd_frag_4", "obj.motd_frag_5", "obj.motd_frag_6", "obj.motd_frag_7", "obj.motd_frag_8", "obj.sailors_amulet_empty", "obj.salvaging_rare_rusty_locket", "obj.salvaging_rare_mouldy_block", "obj.salvaging_rare_dull_knife", "obj.salvaging_rare_broken_compass", "obj.salvaging_rare_rusty_coin", "obj.salvaging_rare_broken_sextant", "obj.salvaging_rare_mouldy_doll", "obj.salvaging_rare_smashed_mirror")
            column(CATEGORY, "Other")
            column(TAB_INDEX, 23)
        }

        row("dbrow.collection_log_category_shayzien_armour") {
            column(NAME, "Shayzien Armour")
            columnRSCM(COMPLETED_VARBIT, "varbit.collection_other_shayzien_completed")
            columnRSCM(ITEMS, "obj.shayzien_gloves_1", "obj.shayzien_boots_1", "obj.shayzien_helm_1", "obj.shayzien_legs_1", "obj.shayzien_body_1", "obj.shayzien_gloves_2", "obj.shayzien_boots_2", "obj.shayzien_helm_2", "obj.shayzien_legs_2", "obj.shayzien_body_2", "obj.shayzien_gloves_3", "obj.shayzien_boots_3", "obj.shayzien_helm_3", "obj.shayzien_legs_3", "obj.shayzien_body_3", "obj.shayzien_gloves_4", "obj.shayzien_boots_4", "obj.shayzien_helm_4", "obj.shayzien_legs_4", "obj.shayzien_body_4", "obj.shayzien_gloves_5", "obj.shayzien_boots_5", "obj.shayzien_helm_5", "obj.shayzien_legs_5", "obj.shayzien_body_5")
            column(CATEGORY, "Other")
            column(TAB_INDEX, 24)
        }

        row("dbrow.collection_log_category_shooting_stars") {
            column(NAME, "Shooting Stars")
            columnRSCM(COMPLETED_VARBIT, "varbit.collection_other_star_completed")
            columnRSCM(ITEMS, "obj.celestial_ring", "obj.star_fragment")
            column(CATEGORY, "Other")
            column(TAB_INDEX, 25)
        }

        row("dbrow.collection_log_category_skilling_pets") {
            column(NAME, "Skilling Pets")
            columnRSCM(COMPLETED_VARBIT, "varbit.collection_other_skilling_completed")
            columnRSCM(ITEMS, "obj.skillpetfish", "obj.skillpetmining", "obj.skillpetwc", "obj.skillpethunter_grey", "obj.skillpetagility", "obj.skillpetfarming", "obj.skillpetthieving", "obj.skillpetrunecrafting_fire", "obj.skillpetsailing")
            column(CATEGORY, "Other")
            column(TAB_INDEX, 26)
        }

        row("dbrow.collection_log_category_slayer") {
            column(NAME, "Slayer")
            columnRSCM(COMPLETED_VARBIT, "varbit.collection_other_slayer_completed")
            columnRSCM(ITEMS, "obj.poh_trophydrop_crawlinghand", "obj.poh_trophydrop_cockatrice", "obj.poh_trophydrop_basilisk", "obj.poh_trophydrop_kurask", "obj.poh_trophydrop_abyssaldemon", "obj.imbued_heart", "obj.slayer_eternal_gem", "obj.dust_battlestaff", "obj.mist_battlestaff", "obj.abyssal_whip", "obj.granite_maul", "obj.mudskipper_hat", "obj.mudskipper_flippers", "obj.olaf2_brine_sabre", "obj.leafbladed_sword", "obj.leafbladed_battleaxe", "obj.harmless_black_mask_10", "obj.granite_longsword", "obj.granite_boots", "obj.wyvern_visage", "obj.granite_legs", "obj.granite_helm", "obj.dragonfire_visage", "obj.bronze_armoured_boots", "obj.iron_armoured_boots", "obj.steel_armoured_boots", "obj.black_armoured_boots", "obj.mithril_armoured_boots", "obj.adamant_armoured_boots", "obj.rune_armoured_boots", "obj.dragon_boots", "obj.abyssal_dagger", "obj.tots_uncharged", "obj.kraken_tentacle", "obj.darkbow", "obj.occult_necklace", "obj.dragon_chainbody", "obj.dragon_thrownaxe", "obj.dragon_harpoon", "obj.dragon_shortsword", "obj.dragon_knife", "obj.broken_dragon_hasta", "obj.drake_tooth", "obj.drake_claw", "obj.hydra_tail", "obj.hydra_fang", "obj.hydra_eye", "obj.hydra_heart", "obj.mystic_hat_light", "obj.mystic_robe_top_light", "obj.mystic_robe_bottom_light", "obj.mystic_gloves_light", "obj.mystic_boots_light", "obj.mystic_hat_dark", "obj.mystic_robe_top_dark", "obj.mystic_robe_bottom_dark", "obj.mystic_gloves_dark", "obj.mystic_boots_dark", "obj.mystic_hat_dusk", "obj.mystic_robe_top_dusk", "obj.mystic_robe_bottom_dusk", "obj.mystic_gloves_dusk", "obj.mystic_boots_dusk", "obj.basilisk_jaw", "obj.aquanite_tendon", "obj.dagonhai_hat", "obj.dagonhai_robe_top", "obj.dagonhai_robe_bottom", "obj.blood_shard", "obj.ancient_ceremonial_mask", "obj.ancient_ceremonial_top", "obj.ancient_ceremonial_legs", "obj.ancient_ceremonial_gloves", "obj.ancient_ceremonial_boots", "obj.warped_sceptre_uncharged", "obj.sulphur_blades", "obj.wilderness_blip_blocking_scroll", "obj.aranea_boots", "obj.glacial_temotli", "obj.pendant_of_ates_empty", "obj.frozen_tear", "obj.earthbound_tecpatl", "obj.custodian_antler_guard", "obj.alchemist_ring", "obj.custodian_broken_antler", "obj.dragon_sheet", "obj.horn_of_plenty_uncharged", "obj.gryphon_feather", "obj.venator_tooth", "obj.venator_fang")
            column(CATEGORY, "Other")
            column(TAB_INDEX, 27)
        }

        row("dbrow.collection_log_category_tormented_demons") {
            column(NAME, "Tormented Demons")
            columnRSCM(COMPLETED_VARBIT, "varbit.collection_other_tormented_demons_completed")
            columnRSCM(ITEMS, "obj.tormented_synapse", "obj.bone_claw", "obj.teleportscroll_guthixian_temple")
            column(CATEGORY, "Other")
            column(TAB_INDEX, 28)
        }

        row("dbrow.collection_log_category_tzhaar") {
            column(NAME, "TzHaar")
            columnRSCM(COMPLETED_VARBIT, "varbit.collection_other_tzhaar_completed")
            columnRSCM(ITEMS, "obj.tzhaar_cape_obsidian", "obj.tzhaar_spikeshield", "obj.tzhaar_maul", "obj.tzhaar_splitsword", "obj.tzhaar_knife", "obj.tzhaar_staff", "obj.tzhaar_throwingring", "obj.obsidian_helmet", "obj.obsidian_platebody", "obj.obsidian_platelegs")
            column(CATEGORY, "Other")
            column(TAB_INDEX, 29)
        }

        row("dbrow.collection_log_category_miscellaneous") {
            column(NAME, "Miscellaneous")
            columnRSCM(COMPLETED_VARBIT, "varbit.collection_other_misc_completed")
            columnRSCM(ITEMS, "obj.herbiboarpet", "obj.chompybird_pet", "obj.dragon_warhammer", "obj.poh_trophydrop_swordfish", "obj.poh_trophydrop_shark", "obj.poh_trophydrop_bass", "obj.poh_trophydrop_giant_krill", "obj.poh_trophydrop_haddock", "obj.poh_trophydrop_yellowfin", "obj.poh_trophydrop_halibut", "obj.poh_trophydrop_bluefin", "obj.poh_trophydrop_marlin", "obj.dorgesh_construction_bone", "obj.dorgesh_construction_bone_curved", "obj.ecumenical_key", "obj.pharaohs_sceptre", "obj.cata_totem1", "obj.cata_totem2", "obj.cata_totem3", "obj.brut_barbarian_bones", "obj.brut_dragon_full_helm", "obj.dragonshield_a", "obj.dragon_slice", "obj.dragon_lump", "obj.xbows_crossbow_limbs_dragon", "obj.dragon_spear", "obj.amulet_of_glory_inf", "obj.ogre_helmet", "obj.evil_chicken_head", "obj.evil_chicken_wings", "obj.evil_chicken_legs", "obj.evil_chicken_feet", "obj.mguild_gloves", "obj.mguild_gloves_superior", "obj.mguild_gloves_expert", "obj.sos_half_skull1", "obj.sos_half_skull2", "obj.sos_half_sceptre1", "obj.sos_half_sceptre2", "obj.mossy_key", "obj.hillgiant_boss_key", "obj.hespori_seed", "obj.hundred_pirate_crab_shell_claw", "obj.hundred_pirate_crab_shell_head", "obj.xeric_talisman_empty", "obj.hosdun_temple_mask", "obj.elven_signet", "obj.prif_crystal_grail", "obj.prif_teleport_seed", "obj.dragonstone_helmet", "obj.dragonstone_platebody", "obj.dragonstone_platelegs", "obj.dragonstone_gauntlets", "obj.dragonstone_armoured_boots", "obj.uncut_onyx", "obj.merfolk_trident", "obj.hosdun_orange_egg_sac", "obj.hosdun_blue_egg_sac", "obj.zombie_axe_broken", "obj.zombie_helmet_broken", "obj.moon_helmet", "obj.squid_beak")
            column(CATEGORY, "Other")
            column(TAB_INDEX, 30)
        }

    }
}
