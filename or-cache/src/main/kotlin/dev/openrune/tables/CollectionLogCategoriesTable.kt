package dev.openrune.tables

import dev.openrune.definition.dbtables.dbTable
import dev.openrune.definition.util.VarType

object CollectionLogCategoriesTable {

    const val STRUCT_ID = 0
    const val COMPLETED_VARBIT = 1
    const val COUNT_VARP_1 = 2
    const val COUNT_VARP_2 = 3
    const val COUNT_VARP_3 = 4
    const val PB_VARP_1 = 5
    const val PB_VARP_2 = 6

    fun collectionLogCategories() = dbTable("dbtable.collection_log_categories", serverOnly = true) {
        column("struct_id", STRUCT_ID, VarType.INT)
        column("completed_varbit", COMPLETED_VARBIT, VarType.INT)
        column("count_varp_1", COUNT_VARP_1, VarType.INT)
        column("count_varp_2", COUNT_VARP_2, VarType.INT)
        column("count_varp_3", COUNT_VARP_3, VarType.INT)
        column("pb_varp_1", PB_VARP_1, VarType.INT)
        column("pb_varp_2", PB_VARP_2, VarType.INT)

        row("dbrow.collection_log_category_abyssal_sire") {
            column(STRUCT_ID, 476)
            columnRSCM(COMPLETED_VARBIT, "varbit.collection_bosses_sire_completed")
            columnRSCM(COUNT_VARP_1, "varp.total_abyssalsire_kills")
        }

        row("dbrow.collection_log_category_alchemical_hydra") {
            column(STRUCT_ID, 539)
            columnRSCM(COMPLETED_VARBIT, "varbit.collection_bosses_hydra_boss_completed")
            columnRSCM(COUNT_VARP_1, "varp.total_hydraboss_kills")
        }

        row("dbrow.collection_log_category_amoxliatl") {
            column(STRUCT_ID, 1016)
            columnRSCM(COMPLETED_VARBIT, "varbit.collection_bosses_amoxliatl_completed")
            columnRSCM(COUNT_VARP_1, "varp.total_amoxliatl_kills")
        }

        row("dbrow.collection_log_category_araxxor") {
            column(STRUCT_ID, 995)
            columnRSCM(COMPLETED_VARBIT, "varbit.collection_bosses_araxxor_completed")
            columnRSCM(COUNT_VARP_1, "varp.total_araxxor_kills")
        }

        row("dbrow.collection_log_category_barrows_chests") {
            column(STRUCT_ID, 477)
            columnRSCM(COMPLETED_VARBIT, "varbit.collection_bosses_barrows_completed")
            columnRSCM(COUNT_VARP_1, "varp.total_barrows_chests")
        }

        row("dbrow.collection_log_category_brutus") {
            column(STRUCT_ID, 6413)
            columnRSCM(COMPLETED_VARBIT, "varbit.collection_bosses_cowboss_completed")
            columnRSCM(COUNT_VARP_1, "varp.total_cowboss_kills")
        }

        row("dbrow.collection_log_category_bryophyta") {
            column(STRUCT_ID, 478)
            columnRSCM(COMPLETED_VARBIT, "varbit.collection_bosses_bryophyta_completed")
            columnRSCM(COUNT_VARP_1, "varp.total_bryophyta_kills")
        }

        row("dbrow.collection_log_category_callisto_and_artio") {
            column(STRUCT_ID, 479)
            columnRSCM(COMPLETED_VARBIT, "varbit.collection_bosses_callisto_completed")
            columnRSCM(COUNT_VARP_1, "varp.total_artio_kills")
            columnRSCM(COUNT_VARP_2, "varp.total_callisto_kills")
        }

        row("dbrow.collection_log_category_cerberus") {
            column(STRUCT_ID, 480)
            columnRSCM(COMPLETED_VARBIT, "varbit.collection_bosses_cerberus_completed")
            columnRSCM(COUNT_VARP_1, "varp.total_cerberus_kills")
        }

        row("dbrow.collection_log_category_chaos_elemental") {
            column(STRUCT_ID, 481)
            columnRSCM(COMPLETED_VARBIT, "varbit.collection_bosses_elemental_completed")
            columnRSCM(COUNT_VARP_1, "varp.total_chaosele_kills")
        }

        row("dbrow.collection_log_category_chaos_fanatic") {
            column(STRUCT_ID, 482)
            columnRSCM(COMPLETED_VARBIT, "varbit.collection_bosses_fanatic_completed")
            columnRSCM(COUNT_VARP_1, "varp.total_chaosfanatic_kills")
        }

        row("dbrow.collection_log_category_commander_zilyana") {
            column(STRUCT_ID, 483)
            columnRSCM(COMPLETED_VARBIT, "varbit.collection_bosses_saradomin_completed")
            columnRSCM(COUNT_VARP_1, "varp.total_saradomin_kills")
        }

        row("dbrow.collection_log_category_corporeal_beast") {
            column(STRUCT_ID, 484)
            columnRSCM(COMPLETED_VARBIT, "varbit.collection_bosses_corp_completed")
            columnRSCM(COUNT_VARP_1, "varp.total_corp_kills")
        }

        row("dbrow.collection_log_category_crazy_archaeologist") {
            column(STRUCT_ID, 485)
            columnRSCM(COMPLETED_VARBIT, "varbit.collection_bosses_arch_completed")
            columnRSCM(COUNT_VARP_1, "varp.total_crazyarchaeologist_kills")
        }

        row("dbrow.collection_log_category_dagannoth_kings") {
            column(STRUCT_ID, 486)
            columnRSCM(COMPLETED_VARBIT, "varbit.collection_bosses_dagannoth_completed")
            columnRSCM(COUNT_VARP_1, "varp.total_supreme_kills")
            columnRSCM(COUNT_VARP_2, "varp.total_prime_kills")
            columnRSCM(COUNT_VARP_3, "varp.total_rex_kills")
        }

        row("dbrow.collection_log_category_deranged_archaeologist") {
            column(STRUCT_ID, 4867)
            columnRSCM(COMPLETED_VARBIT, "varbit.collection_bosses_deranged_archaeologist_completed")
            columnRSCM(COUNT_VARP_1, "varp.total_derangedarchaeologist_kills")
        }

        row("dbrow.collection_log_category_doom_of_mokhaiotl") {
            column(STRUCT_ID, 5029)
            columnRSCM(COMPLETED_VARBIT, "varbit.collection_bosses_dom_completed")
        }

        row("dbrow.collection_log_category_duke_sucellus") {
            column(STRUCT_ID, 4652)
            columnRSCM(COMPLETED_VARBIT, "varbit.collection_bosses_duke_completed")
            columnRSCM(COUNT_VARP_1, "varp.total_duke_sucellus_kills")
        }

        row("dbrow.collection_log_category_the_fight_caves") {
            column(STRUCT_ID, 500)
            columnRSCM(COMPLETED_VARBIT, "varbit.collection_bosses_fight_completed")
            columnRSCM(COUNT_VARP_1, "varp.total_jad_kills")
        }

        row("dbrow.collection_log_category_fortis_colosseum") {
            column(STRUCT_ID, 909)
            columnRSCM(COMPLETED_VARBIT, "varbit.collection_bosses_colosseum_completed")
            columnRSCM(COUNT_VARP_1, "varp.total_sol_kills")
        }

        row("dbrow.collection_log_category_the_gauntlet") {
            column(STRUCT_ID, 605)
            columnRSCM(COMPLETED_VARBIT, "varbit.collection_bosses_gauntlet_completed")
            columnRSCM(COUNT_VARP_1, "varp.total_completed_gauntlet_hm")
            columnRSCM(COUNT_VARP_2, "varp.total_completed_gauntlet")
        }

        row("dbrow.collection_log_category_general_graardor") {
            column(STRUCT_ID, 487)
            columnRSCM(COMPLETED_VARBIT, "varbit.collection_bosses_bandos_completed")
            columnRSCM(COUNT_VARP_1, "varp.total_bandos_kills")
        }

        row("dbrow.collection_log_category_giant_mole") {
            column(STRUCT_ID, 488)
            columnRSCM(COMPLETED_VARBIT, "varbit.collection_bosses_mole_completed")
            columnRSCM(COUNT_VARP_1, "varp.total_mole_kills")
        }

        row("dbrow.collection_log_category_grotesque_guardians") {
            column(STRUCT_ID, 489)
            columnRSCM(COMPLETED_VARBIT, "varbit.collection_bosses_gargoyle_completed")
            columnRSCM(COUNT_VARP_1, "varp.total_gargboss_kills")
        }

        row("dbrow.collection_log_category_hespori") {
            column(STRUCT_ID, 541)
            columnRSCM(COMPLETED_VARBIT, "varbit.collection_bosses_hespori_completed")
            columnRSCM(COUNT_VARP_1, "varp.total_hespori_kills")
        }

        row("dbrow.collection_log_category_the_hueycoatl") {
            column(STRUCT_ID, 1015)
            columnRSCM(COMPLETED_VARBIT, "varbit.collection_bosses_hueycoatl_completed")
            columnRSCM(COUNT_VARP_1, "varp.total_huey_kills")
        }

        row("dbrow.collection_log_category_the_inferno") {
            column(STRUCT_ID, 499)
            columnRSCM(COMPLETED_VARBIT, "varbit.collection_bosses_inferno_completed")
            columnRSCM(COUNT_VARP_1, "varp.total_zuk_kills")
        }

        row("dbrow.collection_log_category_kalphite_queen") {
            column(STRUCT_ID, 490)
            columnRSCM(COMPLETED_VARBIT, "varbit.collection_bosses_kalphite_completed")
            columnRSCM(COUNT_VARP_1, "varp.total_kalphite_kills")
        }

        row("dbrow.collection_log_category_king_black_dragon") {
            column(STRUCT_ID, 491)
            columnRSCM(COMPLETED_VARBIT, "varbit.collection_bosses_dragon_completed")
            columnRSCM(COUNT_VARP_1, "varp.total_kbd_kills")
        }

        row("dbrow.collection_log_category_kraken") {
            column(STRUCT_ID, 492)
            columnRSCM(COMPLETED_VARBIT, "varbit.collection_bosses_kraken_completed")
            columnRSCM(COUNT_VARP_1, "varp.total_kraken_boss_kills")
        }

        row("dbrow.collection_log_category_kree_arra") {
            column(STRUCT_ID, 493)
            columnRSCM(COMPLETED_VARBIT, "varbit.collection_bosses_armadyl_completed")
            columnRSCM(COUNT_VARP_1, "varp.total_armadyl_kills")
        }

        row("dbrow.collection_log_category_k_ril_tsutsaroth") {
            column(STRUCT_ID, 494)
            columnRSCM(COMPLETED_VARBIT, "varbit.collection_bosses_zamorak_completed")
            columnRSCM(COUNT_VARP_1, "varp.total_zamorak_kills")
        }

        row("dbrow.collection_log_category_the_leviathan") {
            column(STRUCT_ID, 4655)
            columnRSCM(COMPLETED_VARBIT, "varbit.collection_bosses_leviathan_completed")
            columnRSCM(COUNT_VARP_1, "varp.total_leviathan_kills")
        }

        row("dbrow.collection_log_category_the_mad_angel") {
            column(STRUCT_ID, 1203)
            columnRSCM(COMPLETED_VARBIT, "varbit.collection_bosses_mad_angel_completed")
        }

        row("dbrow.collection_log_category_maggot_king") {
            column(STRUCT_ID, 1189)
            columnRSCM(COMPLETED_VARBIT, "varbit.collection_bosses_maggotking_completed")
            columnRSCM(COUNT_VARP_1, "varp.total_maggot_king_kills")
        }

        row("dbrow.collection_log_category_moons_of_peril") {
            column(STRUCT_ID, 910)
            columnRSCM(COMPLETED_VARBIT, "varbit.collection_bosses_perilous_moons_completed")
        }

        row("dbrow.collection_log_category_nex") {
            column(STRUCT_ID, 3769)
            columnRSCM(COMPLETED_VARBIT, "varbit.collection_bosses_nex_completed")
            columnRSCM(COUNT_VARP_1, "varp.total_nex_kills")
        }

        row("dbrow.collection_log_category_the_nightmare") {
            column(STRUCT_ID, 1263)
            columnRSCM(COMPLETED_VARBIT, "varbit.collection_bosses_nightmare_completed")
            columnRSCM(COUNT_VARP_1, "varp.total_nightmare_kills")
            columnRSCM(COUNT_VARP_2, "varp.total_nightmare_challenge_kills")
        }

        row("dbrow.collection_log_category_obor") {
            column(STRUCT_ID, 495)
            columnRSCM(COMPLETED_VARBIT, "varbit.collection_bosses_obor_completed")
            columnRSCM(COUNT_VARP_1, "varp.total_hillgiant_boss_kills")
        }

        row("dbrow.collection_log_category_phantom_muspah") {
            column(STRUCT_ID, 4455)
            columnRSCM(COMPLETED_VARBIT, "varbit.collection_bosses_muspah_completed")
            columnRSCM(COUNT_VARP_1, "varp.total_muspah_kills")
        }

        row("dbrow.collection_log_category_royal_titans") {
            column(STRUCT_ID, 1743)
            columnRSCM(COMPLETED_VARBIT, "varbit.collection_bosses_royal_titans_completed")
            columnRSCM(COUNT_VARP_1, "varp.total_royal_titan_kills")
        }

        row("dbrow.collection_log_category_sarachnis") {
            column(STRUCT_ID, 601)
            columnRSCM(COMPLETED_VARBIT, "varbit.collection_bosses_sarachnis_completed")
            columnRSCM(COUNT_VARP_1, "varp.total_sarachnis_kills")
        }

        row("dbrow.collection_log_category_scorpia") {
            column(STRUCT_ID, 496)
            columnRSCM(COMPLETED_VARBIT, "varbit.collection_bosses_scorpia_completed")
            columnRSCM(COUNT_VARP_1, "varp.total_scorpia_kills")
        }

        row("dbrow.collection_log_category_scurrius") {
            column(STRUCT_ID, 777)
            columnRSCM(COMPLETED_VARBIT, "varbit.collection_bosses_rat_boss_completed")
            columnRSCM(COUNT_VARP_1, "varp.total_rat_boss_kills")
        }

        row("dbrow.collection_log_category_shellbane_gryphon") {
            column(STRUCT_ID, 6337)
            columnRSCM(COMPLETED_VARBIT, "varbit.collection_bosses_gryphon_boss_completed")
            columnRSCM(COUNT_VARP_1, "varp.total_gryphon_boss_kills")
        }

        row("dbrow.collection_log_category_skotizo") {
            column(STRUCT_ID, 497)
            columnRSCM(COMPLETED_VARBIT, "varbit.collection_bosses_skotizo_completed")
            columnRSCM(COUNT_VARP_1, "varp.total_cata_boss_kills")
        }

        row("dbrow.collection_log_category_tempoross") {
            column(STRUCT_ID, 2867)
            columnRSCM(COMPLETED_VARBIT, "varbit.collection_bosses_tempoross_completed")
            columnRSCM(COUNT_VARP_1, "varp.total_tempoross_kills")
        }

        row("dbrow.collection_log_category_thermonuclear_smoke_devil") {
            column(STRUCT_ID, 498)
            columnRSCM(COMPLETED_VARBIT, "varbit.collection_bosses_smoke_completed")
            columnRSCM(COUNT_VARP_1, "varp.total_thermy_kills")
        }

        row("dbrow.collection_log_category_vardorvis") {
            column(STRUCT_ID, 4653)
            columnRSCM(COMPLETED_VARBIT, "varbit.collection_bosses_vardorvis_completed")
            columnRSCM(COUNT_VARP_1, "varp.total_vardorvis_kills")
        }

        row("dbrow.collection_log_category_venenatis_and_spindel") {
            column(STRUCT_ID, 501)
            columnRSCM(COMPLETED_VARBIT, "varbit.collection_bosses_venenatis_completed")
            columnRSCM(COUNT_VARP_1, "varp.total_spindel_kills")
            columnRSCM(COUNT_VARP_2, "varp.total_venenatis_kills")
        }

        row("dbrow.collection_log_category_vet_ion_and_calvar_ion") {
            column(STRUCT_ID, 502)
            columnRSCM(COMPLETED_VARBIT, "varbit.collection_bosses_vetion_completed")
            columnRSCM(COUNT_VARP_1, "varp.total_calvarion_kills")
            columnRSCM(COUNT_VARP_2, "varp.total_vetion_kills")
        }

        row("dbrow.collection_log_category_vorkath") {
            column(STRUCT_ID, 503)
            columnRSCM(COMPLETED_VARBIT, "varbit.collection_bosses_vorkath_completed")
            columnRSCM(COUNT_VARP_1, "varp.total_vorkath_kills")
        }

        row("dbrow.collection_log_category_the_whisperer") {
            column(STRUCT_ID, 4654)
            columnRSCM(COMPLETED_VARBIT, "varbit.collection_bosses_whisperer_completed")
            columnRSCM(COUNT_VARP_1, "varp.total_whisperer_kills")
        }

        row("dbrow.collection_log_category_wintertodt") {
            column(STRUCT_ID, 504)
            columnRSCM(COMPLETED_VARBIT, "varbit.collection_bosses_wintertodt_completed")
            columnRSCM(COUNT_VARP_1, "varp.total_wintertodt_kills")
        }

        row("dbrow.collection_log_category_yama") {
            column(STRUCT_ID, 4736)
            columnRSCM(COMPLETED_VARBIT, "varbit.collection_bosses_yama_completed")
            columnRSCM(COUNT_VARP_1, "varp.total_yama_kills")
        }

        row("dbrow.collection_log_category_zalcano") {
            column(STRUCT_ID, 604)
            columnRSCM(COMPLETED_VARBIT, "varbit.collection_bosses_zalcano_completed")
            columnRSCM(COUNT_VARP_1, "varp.total_zalcano_kills")
        }

        row("dbrow.collection_log_category_zulrah") {
            column(STRUCT_ID, 505)
            columnRSCM(COMPLETED_VARBIT, "varbit.collection_bosses_zulrah_completed")
            columnRSCM(COUNT_VARP_1, "varp.total_snakeboss_kills")
        }

        row("dbrow.collection_log_category_chambers_of_xeric") {
            column(STRUCT_ID, 507)
            columnRSCM(COMPLETED_VARBIT, "varbit.collection_raids_cox_completed")
            columnRSCM(COUNT_VARP_1, "varp.total_completed_xericchambers_challenge")
            columnRSCM(COUNT_VARP_2, "varp.total_completed_xericchambers")
        }

        row("dbrow.collection_log_category_theatre_of_blood") {
            column(STRUCT_ID, 506)
            columnRSCM(COMPLETED_VARBIT, "varbit.collection_raids_tob_completed")
            columnRSCM(COUNT_VARP_1, "varp.total_completed_theatreofblood")
            columnRSCM(COUNT_VARP_2, "varp.total_completed_theatreofblood_story")
            columnRSCM(COUNT_VARP_3, "varp.total_completed_theatreofblood_hard")
        }

        row("dbrow.collection_log_category_tombs_of_amascut") {
            column(STRUCT_ID, 4378)
            columnRSCM(COMPLETED_VARBIT, "varbit.collection_raids_toa_completed")
            columnRSCM(COUNT_VARP_1, "varp.total_completed_tombsofamascut")
            columnRSCM(COUNT_VARP_2, "varp.total_completed_tombsofamascut_entry")
            columnRSCM(COUNT_VARP_3, "varp.total_completed_tombsofamascut_expert")
        }

        row("dbrow.collection_log_category_beginner_treasure_trails") {
            column(STRUCT_ID, 593)
            columnRSCM(COMPLETED_VARBIT, "varbit.collection_clues_beginner_completed")
        }

        row("dbrow.collection_log_category_easy_treasure_trails") {
            column(STRUCT_ID, 508)
            columnRSCM(COMPLETED_VARBIT, "varbit.collection_clues_easy_completed")
        }

        row("dbrow.collection_log_category_medium_treasure_trails") {
            column(STRUCT_ID, 509)
            columnRSCM(COMPLETED_VARBIT, "varbit.collection_clues_medium_completed")
        }

        row("dbrow.collection_log_category_hard_treasure_trails") {
            column(STRUCT_ID, 510)
            columnRSCM(COMPLETED_VARBIT, "varbit.collection_clues_hard_completed")
        }

        row("dbrow.collection_log_category_elite_treasure_trails") {
            column(STRUCT_ID, 511)
            columnRSCM(COMPLETED_VARBIT, "varbit.collection_clues_elite_completed")
        }

        row("dbrow.collection_log_category_master_treasure_trails") {
            column(STRUCT_ID, 512)
            columnRSCM(COMPLETED_VARBIT, "varbit.collection_clues_master_completed")
        }

        row("dbrow.collection_log_category_hard_treasure_trails_rare") {
            column(STRUCT_ID, 2869)
            columnRSCM(COMPLETED_VARBIT, "varbit.collection_clues_hard_megarare_completed")
        }

        row("dbrow.collection_log_category_elite_treasure_trails_rare") {
            column(STRUCT_ID, 2870)
            columnRSCM(COMPLETED_VARBIT, "varbit.collection_clues_elite_megarare_completed")
        }

        row("dbrow.collection_log_category_master_treasure_trails_rare") {
            column(STRUCT_ID, 2871)
            columnRSCM(COMPLETED_VARBIT, "varbit.collection_clues_master_megarare_completed")
        }

        row("dbrow.collection_log_category_shared_treasure_trail_rewards") {
            column(STRUCT_ID, 513)
            columnRSCM(COMPLETED_VARBIT, "varbit.collection_clues_shared_completed")
        }

        row("dbrow.collection_log_category_scroll_cases") {
            column(STRUCT_ID, 4874)
            columnRSCM(COMPLETED_VARBIT, "varbit.collection_clues_scroll_cases_completed")
        }

        row("dbrow.collection_log_category_barbarian_assault") {
            column(STRUCT_ID, 516)
            columnRSCM(COMPLETED_VARBIT, "varbit.collection_minigames_ba_completed")
        }

        row("dbrow.collection_log_category_barracuda_trials") {
            column(STRUCT_ID, 6338)
            columnRSCM(COMPLETED_VARBIT, "varbit.collection_minigames_barracuda_trials_completed")
        }

        row("dbrow.collection_log_category_brimhaven_agility_arena") {
            column(STRUCT_ID, 2873)
            columnRSCM(COMPLETED_VARBIT, "varbit.collection_other_brimhaven_agility_completed")
        }

        row("dbrow.collection_log_category_castle_wars") {
            column(STRUCT_ID, 514)
            columnRSCM(COMPLETED_VARBIT, "varbit.collection_minigames_castle_completed")
        }

        row("dbrow.collection_log_category_fishing_trawler") {
            column(STRUCT_ID, 520)
            columnRSCM(COMPLETED_VARBIT, "varbit.collection_minigames_trawler_completed")
        }

        row("dbrow.collection_log_category_giants_foundry") {
            column(STRUCT_ID, 4359)
            columnRSCM(COMPLETED_VARBIT, "varbit.collection_minigames_giantsfoundry_completed")
        }

        row("dbrow.collection_log_category_gnome_restaurant") {
            column(STRUCT_ID, 521)
            columnRSCM(COMPLETED_VARBIT, "varbit.collection_minigames_gnome_completed")
        }

        row("dbrow.collection_log_category_guardians_of_the_rift") {
            column(STRUCT_ID, 4274)
            columnRSCM(COMPLETED_VARBIT, "varbit.collection_minigames_gotr_completed")
            columnRSCM(COUNT_VARP_1, "varp.total_gotr_kills")
        }

        row("dbrow.collection_log_category_hallowed_sepulchre") {
            column(STRUCT_ID, 1279)
            columnRSCM(COMPLETED_VARBIT, "varbit.collection_minigames_hallowed_sepulchre_completed")
        }

        row("dbrow.collection_log_category_last_man_standing") {
            column(STRUCT_ID, 1720)
            columnRSCM(COMPLETED_VARBIT, "varbit.collection_minigames_lms_completed")
        }

        row("dbrow.collection_log_category_magic_training_arena") {
            column(STRUCT_ID, 515)
            columnRSCM(COMPLETED_VARBIT, "varbit.collection_minigames_mta_completed")
        }

        row("dbrow.collection_log_category_mahogany_homes") {
            column(STRUCT_ID, 1689)
            columnRSCM(COMPLETED_VARBIT, "varbit.collection_minigames_constructioncontracts_completed")
        }

        row("dbrow.collection_log_category_mastering_mixology") {
            column(STRUCT_ID, 1017)
            columnRSCM(COMPLETED_VARBIT, "varbit.collection_minigames_mastering_mixology_completed")
        }

        row("dbrow.collection_log_category_pest_control") {
            column(STRUCT_ID, 518)
            columnRSCM(COMPLETED_VARBIT, "varbit.collection_minigames_pest_completed")
        }

        row("dbrow.collection_log_category_rogues_den") {
            column(STRUCT_ID, 522)
            columnRSCM(COMPLETED_VARBIT, "varbit.collection_minigames_rogues_completed")
        }

        row("dbrow.collection_log_category_shades_of_mort_ton") {
            column(STRUCT_ID, 517)
            columnRSCM(COMPLETED_VARBIT, "varbit.collection_minigames_shades_completed")
        }

        row("dbrow.collection_log_category_soul_wars") {
            column(STRUCT_ID, 2825)
            columnRSCM(COMPLETED_VARBIT, "varbit.collection_minigames_soul_wars_completed")
        }

        row("dbrow.collection_log_category_temple_trekking") {
            column(STRUCT_ID, 519)
            columnRSCM(COMPLETED_VARBIT, "varbit.collection_minigames_temple_completed")
        }

        row("dbrow.collection_log_category_tithe_farm") {
            column(STRUCT_ID, 524)
            columnRSCM(COMPLETED_VARBIT, "varbit.collection_minigames_tithe_completed")
        }

        row("dbrow.collection_log_category_trouble_brewing") {
            column(STRUCT_ID, 523)
            columnRSCM(COMPLETED_VARBIT, "varbit.collection_minigames_brewing_completed")
        }

        row("dbrow.collection_log_category_vale_totems") {
            column(STRUCT_ID, 5055)
            columnRSCM(COMPLETED_VARBIT, "varbit.collection_other_vale_totems_completed")
        }

        row("dbrow.collection_log_category_volcanic_mine") {
            column(STRUCT_ID, 2872)
            columnRSCM(COMPLETED_VARBIT, "varbit.collection_other_volcanic_mine_completed")
        }

        row("dbrow.collection_log_category_aerial_fishing") {
            column(STRUCT_ID, 540)
            columnRSCM(COMPLETED_VARBIT, "varbit.collection_other_aerial_fishing_completed")
        }

        row("dbrow.collection_log_category_all_pets") {
            column(STRUCT_ID, 535)
            columnRSCM(COMPLETED_VARBIT, "varbit.collection_other_pets_completed")
        }

        row("dbrow.collection_log_category_boat_paints") {
            column(STRUCT_ID, 6342)
            columnRSCM(COMPLETED_VARBIT, "varbit.collection_other_sailing_paint_completed")
        }

        row("dbrow.collection_log_category_camdozaal") {
            column(STRUCT_ID, 2885)
            columnRSCM(COMPLETED_VARBIT, "varbit.collection_other_camdozaal_completed")
        }

        row("dbrow.collection_log_category_champion_s_challenge") {
            column(STRUCT_ID, 528)
            columnRSCM(COMPLETED_VARBIT, "varbit.collection_other_champions_completed")
        }

        row("dbrow.collection_log_category_chompy_bird_hunting") {
            column(STRUCT_ID, 537)
            columnRSCM(COMPLETED_VARBIT, "varbit.collection_other_chompy_completed")
        }

        row("dbrow.collection_log_category_colossal_wyrm_agility") {
            column(STRUCT_ID, 1018)
            columnRSCM(COMPLETED_VARBIT, "varbit.collection_other_colossal_wyrm_agility_completed")
        }

        row("dbrow.collection_log_category_creature_creation") {
            column(STRUCT_ID, 546)
            columnRSCM(COMPLETED_VARBIT, "varbit.collection_other_creature_creation_completed")
        }

        row("dbrow.collection_log_category_cyclopes") {
            column(STRUCT_ID, 532)
            columnRSCM(COMPLETED_VARBIT, "varbit.collection_other_cyclopes_completed")
        }

        row("dbrow.collection_log_category_elder_chaos_druids") {
            column(STRUCT_ID, 533)
            columnRSCM(COMPLETED_VARBIT, "varbit.collection_other_chaos_completed")
        }

        row("dbrow.collection_log_category_forestry") {
            column(STRUCT_ID, 4553)
            columnRSCM(COMPLETED_VARBIT, "varbit.collection_other_forestry_completed")
        }

        row("dbrow.collection_log_category_fossil_island_notes") {
            column(STRUCT_ID, 548)
            columnRSCM(COMPLETED_VARBIT, "varbit.collection_other_fossil_notes_completed")
        }

        row("dbrow.collection_log_category_glough_s_experiments") {
            column(STRUCT_ID, 526)
            columnRSCM(COMPLETED_VARBIT, "varbit.collection_other_gorillas_completed")
            columnRSCM(COUNT_VARP_1, "varp.kc_demonic_gorilla")
        }

        row("dbrow.collection_log_category_hunter_guild") {
            column(STRUCT_ID, 911)
            columnRSCM(COMPLETED_VARBIT, "varbit.collection_other_hunter_guild_completed")
        }

        row("dbrow.collection_log_category_lost_schematics") {
            column(STRUCT_ID, 6339)
            columnRSCM(COMPLETED_VARBIT, "varbit.collection_other_lost_schematics_completed")
        }

        row("dbrow.collection_log_category_monkey_backpacks") {
            column(STRUCT_ID, 1397)
            columnRSCM(COMPLETED_VARBIT, "varbit.collection_other_monkey_backpack_completed")
        }

        row("dbrow.collection_log_category_motherlode_mine") {
            column(STRUCT_ID, 530)
            columnRSCM(COMPLETED_VARBIT, "varbit.collection_other_motherlode_completed")
        }

        row("dbrow.collection_log_category_my_notes") {
            column(STRUCT_ID, 549)
            columnRSCM(COMPLETED_VARBIT, "varbit.collection_other_barbarian_notes_completed")
        }

        row("dbrow.collection_log_category_ocean_encounters") {
            column(STRUCT_ID, 6340)
            columnRSCM(COMPLETED_VARBIT, "varbit.collection_other_ocean_encounters_completed")
        }

        row("dbrow.collection_log_category_random_events") {
            column(STRUCT_ID, 538)
            columnRSCM(COMPLETED_VARBIT, "varbit.collection_other_random_completed")
        }

        row("dbrow.collection_log_category_revenants") {
            column(STRUCT_ID, 525)
            columnRSCM(COMPLETED_VARBIT, "varbit.collection_other_revenants_completed")
        }

        row("dbrow.collection_log_category_rooftop_agility") {
            column(STRUCT_ID, 547)
            columnRSCM(COMPLETED_VARBIT, "varbit.collection_other_rooftop_completed")
        }

        row("dbrow.collection_log_category_sailing_miscellaneous") {
            column(STRUCT_ID, 6343)
            columnRSCM(COMPLETED_VARBIT, "varbit.collection_other_sailing_misc_completed")
        }

        row("dbrow.collection_log_category_sea_treasures") {
            column(STRUCT_ID, 6341)
            columnRSCM(COMPLETED_VARBIT, "varbit.collection_other_sea_treasures_completed")
        }

        row("dbrow.collection_log_category_shayzien_armour") {
            column(STRUCT_ID, 531)
            columnRSCM(COMPLETED_VARBIT, "varbit.collection_other_shayzien_completed")
        }

        row("dbrow.collection_log_category_shooting_stars") {
            column(STRUCT_ID, 2858)
            columnRSCM(COMPLETED_VARBIT, "varbit.collection_other_star_completed")
        }

        row("dbrow.collection_log_category_skilling_pets") {
            column(STRUCT_ID, 529)
            columnRSCM(COMPLETED_VARBIT, "varbit.collection_other_skilling_completed")
        }

        row("dbrow.collection_log_category_slayer") {
            column(STRUCT_ID, 527)
            columnRSCM(COMPLETED_VARBIT, "varbit.collection_other_slayer_completed")
        }

        row("dbrow.collection_log_category_tormented_demons") {
            column(STRUCT_ID, 969)
            columnRSCM(COMPLETED_VARBIT, "varbit.collection_other_tormented_demons_completed")
        }

        row("dbrow.collection_log_category_tzhaar") {
            column(STRUCT_ID, 536)
            columnRSCM(COMPLETED_VARBIT, "varbit.collection_other_tzhaar_completed")
        }

        row("dbrow.collection_log_category_miscellaneous") {
            column(STRUCT_ID, 534)
            columnRSCM(COMPLETED_VARBIT, "varbit.collection_other_misc_completed")
        }

    }
}
