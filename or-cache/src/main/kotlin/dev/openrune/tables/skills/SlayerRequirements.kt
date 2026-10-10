package dev.openrune.tables.skills

import dev.openrune.definition.constants.ConstantProvider
import dev.openrune.definition.dbtables.DBRowBuilder
import dev.openrune.definition.dbtables.DBTableBuilder
import dev.openrune.definition.dbtables.dbTable
import dev.openrune.definition.util.VarType

object SlayerRequirements {

    const val MONSTER_NPCS = 0
    const val MONSTER_LEVEL = 1
    const val MONSTER_GEAR = 2
    const val MONSTER_FINISHER = 3
    const val MONSTER_WEAPON_RULE = 4

    const val GEAR_ITEMS = 0
    const val GEAR_FIXED_HIT = 1
    const val GEAR_MAX_HIT = 2
    const val GEAR_HITPOINTS_PERCENT = 3
    const val GEAR_GUARANTEED = 4
    const val GEAR_PENETRATION = 5
    const val GEAR_DRAINS = 6
    const val GEAR_FIRST_HIT_ONLY = 7
    const val GEAR_REPEL_MESSAGE = 8

    const val FINISHER_ITEMS = 0
    const val FINISHER_CONSUMED = 1
    const val FINISHER_CHARGES = 2
    const val FINISHER_AUTO_WORN = 3
    const val FINISHER_BYPASS_WORN = 4
    const val FINISHER_THRESHOLD = 5
    const val FINISHER_UNLOCK = 6
    const val FINISHER_WASTED_EARLY = 7
    const val FINISHER_THROWN = 8

    const val WEAPON_WEAPONS = 0
    const val WEAPON_AMMO = 1
    const val WEAPON_SPELLS = 2

    private val SLAYER_HELMET_COLOURS =
        listOf(
            "",
            "_black",
            "_green",
            "_red",
            "_purple",
            "_turquoise",
            "_hydra",
            "_twisted",
            "_jad",
            "_verzik",
            "_zuk",
            "_araxyte",
            "_hooded",
        )

    private val SLAYER_HELMETS: List<String> =
        SLAYER_HELMET_COLOURS.flatMap { colour ->
            listOf(
                "obj.slayer_helm$colour",
                "obj.slayer_helm_i$colour",
                "obj.sw_slayer_helm_i$colour",
                "obj.pvpa_slayer_helm_i$colour",
            )
        }

    private val MIRROR_SHIELDS =
        listOf("obj.slayer_mirror_shield", "obj.v_shield", "obj.vikingexile_v_shield")

    private val NOSE_PEG_DRAINS =
        listOf(
            "stat.attack" to 80,
            "stat.strength" to 80,
            "stat.magic" to 80,
            "stat.ranged" to 80,
            "stat.defence" to 40,
            "stat.prayer" to 40,
            "stat.agility" to 40,
        )

    private val EARMUFFS_DRAINS =
        listOf(
            "stat.attack" to 20,
            "stat.strength" to 20,
            "stat.magic" to 20,
            "stat.ranged" to 20,
            "stat.defence" to 10,
            "stat.prayer" to 10,
            "stat.agility" to 10,
        )

    private val DUST_DEVIL_DRAINS =
        listOf(
            "stat.attack" to 100,
            "stat.strength" to 100,
            "stat.magic" to 100,
            "stat.ranged" to 100,
            "stat.defence" to 50,
            "stat.agility" to 50,
        )

    private val SMOKE_DEVIL_DRAINS =
        listOf(
            "stat.attack" to 85,
            "stat.strength" to 85,
            "stat.magic" to 85,
            "stat.ranged" to 85,
            "stat.defence" to 50,
            "stat.agility" to 50,
        )

    private fun slayerHelmetsAnd(item: String): List<String> = listOf(item) + SLAYER_HELMETS

    fun monsters() =
        dbTable("dbtable.slayer_monsters", serverOnly = true) {
            column("npcs", MONSTER_NPCS, VarType.NPC)
            column("level", MONSTER_LEVEL, VarType.INT)
            column("gear", MONSTER_GEAR, VarType.DBROW)
            column("finisher", MONSTER_FINISHER, VarType.DBROW)
            column("weapon_rule", MONSTER_WEAPON_RULE, VarType.DBROW)

        monster(
            name = "crawling_hand",
            level = 5,
            npcs =
                listOf(
                    "npc.slayer_crawling_hand_3", "npc.slayer_crawling_hand_1",
                    "npc.slayer_crawling_hand_2", "npc.slayer_crawling_hand_4",
                    "npc.slayer_crawling_hand_5", "npc.slayer_crawling_hand_big_3",
                    "npc.slayer_crawling_hand_big_1", "npc.slayer_crawling_hand_big_2",
                    "npc.slayer_crawling_hand_big_4", "npc.slayer_crawling_hand_big_5",
                    "npc.superior_crawling_hand", "npc.league_superior_crawling_hand",
                ),
        )
        monster(
            name = "cave_bug",
            level = 7,
            npcs =
                listOf(
                    "npc.swamp_cave_bug", "npc.molanisk_giant_cave_bug",
                ),
        )
        monster(
            name = "cave_crawler",
            level = 10,
            npcs =
                listOf(
                    "npc.cave_crawler_ice_1", "npc.cave_crawler_ice_2",
                    "npc.slayer_cave_crawler_1", "npc.slayer_cave_crawler_2",
                    "npc.slayer_cave_crawler_3", "npc.slayer_cave_crawler_4",
                    "npc.superior_cave_crawler", "npc.league_superior_cave_crawler",
                ),
        )
        monster(
            name = "banshee",
            level = 15,
            gear = "earmuffs",
            npcs =
                listOf(
                    "npc.slayer_banshee_1",
                ),
        )
        monster(
            name = "screaming_banshee",
            level = 15,
            npcs =
                listOf(
                    "npc.superior_banshee", "npc.league_superior_banshee",
                ),
        )
        monster(
            name = "twisted_banshee",
            level = 15,
            gear = "earmuffs_drain",
            npcs =
                listOf(
                    "npc.kourend_banshee",
                ),
        )
        monster(
            name = "screaming_twisted_banshee",
            level = 15,
            npcs =
                listOf(
                    "npc.superior_kourend_banshee", "npc.league_superior_kourend_banshee",
                ),
        )
        monster(
            name = "cave_slime",
            level = 17,
            npcs =
                listOf(
                    "npc.swamp_cave_slime",
                ),
        )
        monster(
            name = "rockslug",
            level = 20,
            finisher = "rockslug",
            npcs =
                listOf(
                    "npc.slayer_rockslug", "npc.slayer_rockslug_baby",
                    "npc.slayer_rockslug_crypt_of_tonali", "npc.superior_rockslug",
                    "npc.league_superior_rockslug",
                ),
        )
        monster(
            name = "desert_lizard",
            level = 22,
            finisher = "desert_lizard",
            npcs =
                listOf(
                    "npc.slayer_lizard_large1_green", "npc.slayer_lizard_large1_green_lowrange",
                    "npc.slayer_lizard_large2_sandy", "npc.slayer_lizard_large3_sandy",
                    "npc.slayer_lizard_massive", "npc.slayer_lizard_small1_green",
                    "npc.slayer_lizard_small1_green_lowrange", "npc.slayer_lizard_small2_sandy",
                ),
        )
        monster(
            name = "cockatrice",
            level = 25,
            gear = "mirror_cockatrice",
            npcs =
                listOf(
                    "npc.slayer_cockatrice", "npc.superior_cockatrice",
                    "npc.league_superior_cockatrice",
                ),
        )
        monster(
            name = "pyrefiend",
            level = 30,
            npcs =
                listOf(
                    "npc.slayer_pyrefiend_1", "npc.slayer_pyrefiend_2", "npc.slayer_pyrefiend_3",
                    "npc.slayer_pyrefiend_4", "npc.godwars_pyrefiend_1",
                    "npc.superior_pyrefiend", "npc.league_superior_pyrefiend",
                    "npc.deadman_breach_superior_pyrefiend",
                ),
        )
        monster(
            name = "pyrelord",
            level = 30,
            npcs =
                listOf(
                    "npc.slayer_pyrebeast_1", "npc.slayer_pyrebeast_2", "npc.superior_pyrelord",
                    "npc.league_superior_pyrelord",
                ),
        )
        monster(
            name = "mogre",
            level = 32,
            npcs =
                listOf(
                    "npc.mudskipper_ogre", "npc.sailing_sea_mogre", "npc.sailing_sea_mogre_f",
                ),
        )
        monster(
            name = "harpie_bug_swarm",
            level = 33,
            npcs =
                listOf(
                    "npc.slayer_harpiebugswarm",
                ),
        )
        monster(
            name = "wall_beast",
            level = 35,
            gear = "spiny_helmet",
            npcs =
                listOf(
                    "npc.swamp_wallbeast_combat", "npc.swamp_wallbeast",
                ),
        )
        monster(
            name = "killerwatt",
            level = 37,
            gear = "insulated_boots",
            npcs =
                listOf(
                    "npc.slayer_killerwatt_ball", "npc.slayer_killerwatt",
                ),
        )
        monster(
            name = "vyrewatch_sentinel",
            level = 38,
            npcs =
                listOf(
                    "npc.vyrewatch_elite_1", "npc.vyrewatch_elite_2", "npc.vyrewatch_elite_3",
                    "npc.vyrewatch_elite_4", "npc.vyrewatch_elite_5", "npc.vyrewatch_elite_6",
                    "npc.vyrewatch_elite_7", "npc.vyrewatch_elite_8",
                ),
        )
        monster(
            name = "molanisk",
            level = 39,
            npcs =
                listOf(
                    "npc.molanisk",
                ),
        )
        monster(
            name = "basilisk",
            level = 40,
            gear = "mirror_basilisk",
            npcs =
                listOf(
                    "npc.slayer_basilisk", "npc.superior_basilisk", "npc.slayer_basilisk_baby",
                    "npc.league_superior_basilisk",
                ),
        )
        monster(
            name = "terror_dog",
            level = 40,
            npcs =
                listOf(
                    "npc.lotr_terror_dog_small", "npc.lotr_terror_dog_large",
                ),
        )
        monster(
            name = "sea_snake",
            level = 40,
            npcs =
                listOf(
                    "npc.royal_sea_snake_baby", "npc.royal_sea_snake_mother_smaller",
                ),
        )
        monster(
            name = "fever_spider",
            level = 42,
            npcs =
                listOf(
                    "npc.deal_fever_spiders1",
                ),
        )
        monster(
            name = "sulphur_lizard",
            level = 44,
            npcs =
                listOf(
                    "npc.sulphur_lizard",
                ),
        )
        monster(
            name = "grimy_lizard",
            level = 44,
            npcs =
                listOf(
                    "npc.crypt_of_tonali_grimy_lizard", "npc.grimy_lizard",
                ),
        )
        monster(
            name = "infernal_mage",
            level = 45,
            npcs =
                listOf(
                    "npc.slayer_infernal_mage_1", "npc.slayer_infernal_mage_2",
                    "npc.slayer_infernal_mage_3", "npc.slayer_infernal_mage_4",
                    "npc.slayer_infernal_mage_5", "npc.superior_infernal_mage",
                    "npc.league_superior_infernal_mage",
                    "npc.deadman_breach_superior_infernal_mage",
                ),
        )
        monster(
            name = "brine_rat",
            level = 47,
            npcs =
                listOf(
                    "npc.olaf2_brine_rats",
                ),
        )
        monster(
            name = "sulphur_nagua",
            level = 48,
            npcs =
                listOf(
                    "npc.pmoon_sulphur_nagua",
                ),
        )
        monster(
            name = "frost_nagua",
            level = 48,
            npcs =
                listOf(
                    "npc.frost_nagua", "npc.frost_nagua_cutscene", "npc.vmq3_ruins_frost_nagua",
                ),
        )
        monster(
            name = "amoxliatl",
            level = 48,
            npcs =
                listOf(
                    "npc.amoxliatl", "npc.amoxliatl_quest",
                ),
        )
        monster(
            name = "earthen_nagua",
            level = 48,
            npcs =
                listOf(
                    "npc.earth_nagua", "npc.earth_nagua_reinforced",
                ),
        )
        monster(
            name = "bloodveld",
            level = 50,
            npcs =
                listOf(
                    "npc.slayer_bloodveld", "npc.slayer_bloodveld_baby",
                    "npc.slayer_bloodveld_baby_strongholdcave",
                    "npc.slayer_bloodveld_strongholdcave", "npc.godwars_bloodveld",
                    "npc.superior_bloodveld", "npc.league_superior_bloodveld",
                ),
        )
        monster(
            name = "mutated_bloodveld",
            level = 50,
            npcs =
                listOf(
                    "npc.kourend_bloodveld", "npc.superior_kourend_bloodveld",
                    "npc.league_superior_kourend_bloodveld",
                ),
        )
        monster(
            name = "gryphon",
            level = 51,
            npcs =
                listOf(
                    "npc.slayer_gryphon_1", "npc.slayer_gryphon_2", "npc.superior_gryphon",
                ),
        )
        monster(
            name = "shellbane_gryphon",
            level = 51,
            npcs =
                listOf(
                    "npc.gryphon_boss",
                ),
        )
        monster(
            name = "jelly",
            level = 52,
            npcs =
                listOf(
                    "npc.slayer_jelly_1", "npc.slayer_jelly_2", "npc.slayer_jelly_3",
                    "npc.slayer_jelly_4", "npc.slayer_jelly_5", "npc.slayer_jelly_6",
                    "npc.wild_cave_jelly_1", "npc.wild_cave_jelly_2", "npc.wild_cave_jelly_3",
                    "npc.wild_cave_jelly_4", "npc.wild_cave_jelly_5", "npc.superior_jelly",
                    "npc.league_superior_jelly",
                ),
        )
        monster(
            name = "warped_jelly",
            level = 52,
            npcs =
                listOf(
                    "npc.kourend_jelly", "npc.superior_kourend_jelly",
                    "npc.league_superior_kourend_jelly",
                    "npc.deadman_breach_superior_kourend_jelly",
                ),
        )
        monster(
            name = "chilled_jelly",
            level = 52,
            npcs =
                listOf(
                    "npc.jelly_tapoyauik", "npc.superior_chilled_jelly",
                ),
        )
        monster(
            name = "juvenile_custodian_stalker",
            level = 54,
            npcs =
                listOf(
                    "npc.juvenile_custodian_stalker",
                ),
        )
        monster(
            name = "turoth",
            level = 55,
            weaponRule = "leafy",
            npcs =
                listOf(
                    "npc.slayer_turoth_baby", "npc.slayer_turoth_child", "npc.slayer_turoth_mum",
                    "npc.slayer_turoth_dad", "npc.superior_turoth", "npc.league_superior_turoth",
                ),
        )
        monster(
            name = "warped_terrorbird",
            level = 56,
            npcs =
                listOf(
                    "npc.pog_mutated_terrorbird_1", "npc.pog_mutated_terrorbird_2",
                    "npc.pog_mutated_terrorbird_3", "npc.pog_mutated_terrorbird_4",
                    "npc.pog_mutated_terrorbird_5", "npc.pog_mutated_terrorbird_6",
                    "npc.pog_mutated_terrorbird_7", "npc.pog_mutated_terrorbird_8",
                    "npc.pog_mutated_terrorbird_boss_1",
                    "npc.pog_mutated_terrorbird_boss_1_background_vis",
                    "npc.pog_mutated_terrorbird_boss_2",
                    "npc.pog_mutated_terrorbird_boss_2_background_vis",
                    "npc.pog_mutated_terrorbird_boss_3",
                    "npc.pog_mutated_terrorbird_boss_3_background_vis",
                    "npc.superior_warped_terrorbird",
                ),
        )
        monster(
            name = "warped_tortoise",
            level = 56,
            npcs =
                listOf(
                    "npc.pog_mutated_tortoise", "npc.superior_warped_tortoise",
                ),
        )
        monster(
            name = "mutated_zygomite",
            level = 57,
            finisher = "zygomite",
            npcs =
                listOf(
                    "npc.slayer_mutated_zygomite_adolescent",
                    "npc.slayer_mutated_zygomite_adult",
                ),
        )
        monster(
            name = "ancient_zygomite",
            level = 57,
            finisher = "zygomite",
            npcs =
                listOf(
                    "npc.fossil_zygomite",
                ),
        )
        monster(
            name = "cave_horror",
            level = 58,
            gear = "witchwood_icon",
            npcs =
                listOf(
                    "npc.harmless_island_cave_horror_alpha",
                    "npc.harmless_island_cave_horror_guard",
                    "npc.harmless_island_cave_horror_small_guard",
                    "npc.harmless_island_cave_horror_worker",
                    "npc.harmless_island_cave_horror_young_worker", "npc.superior_cave_horror",
                    "npc.league_superior_cave_horror", "npc.deadman_breach_superior_cave_horror",
                ),
        )
        monster(
            name = "aberrant_spectre",
            level = 60,
            gear = "nose_peg",
            npcs =
                listOf(
                    "npc.slayer_abberant_spectre_1", "npc.slayer_abberant_spectre_2",
                    "npc.slayer_abberant_spectre_3", "npc.slayer_abberant_spectre_4",
                    "npc.slayer_aberrantspectre_1_strongholdcave",
                    "npc.slayer_aberrantspectre_2_strongholdcave", "npc.kourend_spectre",
                ),
        )
        monster(
            name = "abhorrent_spectre",
            level = 60,
            gear = "nose_peg_drain",
            npcs =
                listOf(
                    "npc.superior_abberant_spectre", "npc.superior_kourend_spectre",
                    "npc.league_superior_abberant_spectre",
                    "npc.league_superior_kourend_spectre",
                ),
        )
        monster(
            name = "basilisk_knight",
            level = 60,
            gear = "mirror_basilisk_knight",
            npcs =
                listOf(
                    "npc.basilisk_knight", "npc.superior_basilisk_knight",
                    "npc.league_superior_basilisk_knight",
                ),
        )
        monster(
            name = "wyrm",
            level = 62,
            npcs =
                listOf(
                    "npc.wyrm_dark", "npc.wyrm_light", "npc.superior_wyrm_dark",
                    "npc.league_superior_wyrm_dark",
                ),
        )
        monster(
            name = "wyrmling",
            level = 62,
            npcs =
                listOf(
                    "npc.varlamore_wyrm_dark01", "npc.varlamore_wyrm_light01",
                ),
        )
        monster(
            name = "strykewyrm",
            level = 62,
            npcs =
                listOf(
                    "npc.slayer_lava_strykewyrm", "npc.superior_lava_strykewyrm",
                ),
        )
        monster(
            name = "spiritual_ranger",
            level = 63,
            npcs =
                listOf(
                    "npc.godwars_spiritual_zamorak_ranger",
                    "npc.godwars_spiritual_saradomin_ranger",
                    "npc.godwars_spiritual_bandos_ranger",
                    "npc.godwars_spiritual_armadyl_ranger", "npc.nex_prison_ranger",
                ),
        )
        monster(
            name = "dust_devil",
            level = 65,
            gear = "facemask_dust",
            npcs =
                listOf(
                    "npc.slayer_dustdevil", "npc.wild_cave_dustdevil", "npc.kourend_dustdevil",
                ),
        )
        monster(
            name = "choke_devil",
            level = 65,
            gear = "facemask_dust_drain",
            npcs =
                listOf(
                    "npc.superior_dustdevil", "npc.league_superior_dustdevil",
                ),
        )
        monster(
            name = "spitting_wyvern",
            level = 66,
            npcs =
                listOf(
                    "npc.wyvern_spitting",
                ),
        )
        monster(
            name = "taloned_wyvern",
            level = 66,
            npcs =
                listOf(
                    "npc.wyvern_taloned",
                ),
        )
        monster(
            name = "long_tailed_wyvern",
            level = 66,
            npcs =
                listOf(
                    "npc.wyvern_long_tailed",
                ),
        )
        monster(
            name = "mature_custodian_stalker",
            level = 67,
            npcs =
                listOf(
                    "npc.mature_custodian_stalker",
                ),
        )
        monster(
            name = "spiritual_warrior",
            level = 68,
            npcs =
                listOf(
                    "npc.godwars_spiritual_zamorak_warrior",
                    "npc.godwars_spiritual_saradomin_warrior",
                    "npc.godwars_spiritual_bandos_warrior",
                    "npc.godwars_spiritual_armadyl_warrior", "npc.nex_prison_warrior",
                ),
        )
        monster(
            name = "kurask",
            level = 70,
            weaponRule = "leafy",
            npcs =
                listOf(
                    "npc.slayer_kursk_1", "npc.slayer_kursk_2", "npc.superior_kurask",
                    "npc.league_superior_kurask",
                ),
        )
        monster(
            name = "skeletal_wyvern",
            level = 72,
            npcs =
                listOf(
                    "npc.skeletal_wyvern1", "npc.skeletal_wyvern2", "npc.skeletal_wyvern3",
                    "npc.skeletal_wyvern4",
                ),
        )
        monster(
            name = "gargoyle",
            level = 75,
            finisher = "gargoyle",
            npcs =
                listOf(
                    "npc.slayer_cave_gargoyle", "npc.slayer_gargoyle_1",
                    "npc.slayer_gargoyle_dead", "npc.superior_gargoyle",
                    "npc.league_superior_gargoyle",
                ),
        )
        monster(
            name = "grotesque_guardians",
            level = 75,
            npcs =
                listOf(
                    "npc.gargboss_dawn_death", "npc.gargboss_dawn_phase1",
                    "npc.gargboss_dawn_phase1_transition", "npc.gargboss_dawn_phase3",
                    "npc.gargboss_dusk_phase1_defensive",
                    "npc.gargboss_dusk_phase1_flytransition",
                    "npc.gargboss_dusk_phase1_transition", "npc.gargboss_dusk_phase2_attacking",
                    "npc.gargboss_dusk_phase3_defensive", "npc.gargboss_dusk_phase3_transition",
                    "npc.gargboss_dusk_death", "npc.gargboss_dusk_phase4",
                    "npc.gargboss_dusk_phase4_spawn",
                ),
        )
        monster(
            name = "elder_custodian_stalker",
            level = 76,
            npcs =
                listOf(
                    "npc.elder_custodian_stalker", "npc.superior_custodian",
                ),
        )
        monster(
            name = "brutal_black_dragon",
            level = 77,
            npcs =
                listOf(
                    "npc.ds2_brut_black_dragon", "npc.ds2_brut_black_dragon_cutscene",
                    "npc.kourend_black_dragon",
                ),
        )
        monster(
            name = "aquanite",
            level = 78,
            npcs =
                listOf(
                    "npc.slayer_aquanite", "npc.superior_aquanite",
                ),
        )
        monster(
            name = "nechryael",
            level = 80,
            npcs =
                listOf(
                    "npc.slayer_nechryael", "npc.slayer_nechryael_strongholdcave",
                    "npc.kourend_nechryael", "npc.wild_cave_nechryael", "npc.superior_nechryael",
                    "npc.league_superior_nechryael",
                ),
        )
        monster(
            name = "ancient_wyvern",
            level = 82,
            npcs =
                listOf(
                    "npc.ancient_wyvern",
                ),
        )
        monster(
            name = "spiritual_mage",
            level = 83,
            npcs =
                listOf(
                    "npc.godwars_spiritual_zamorak_mage", "npc.godwars_spiritual_saradomin_mage",
                    "npc.godwars_spiritual_bandos_mage", "npc.godwars_spiritual_armadyl_mage",
                    "npc.nex_prison_mage",
                ),
        )
        monster(
            name = "drake",
            level = 84,
            npcs =
                listOf(
                    "npc.drake", "npc.drake_death", "npc.superior_drake",
                    "npc.league_superior_drake",
                ),
        )
        monster(
            name = "abyssal_demon",
            level = 85,
            npcs =
                listOf(
                    "npc.kourend_abyssal", "npc.slayer_abyssal",
                    "npc.slayer_abyssal_strongholdcave", "npc.wild_cave_abyssal",
                    "npc.superior_abyssal_demon", "npc.league_superior_abyssal_demon",
                    "npc.deadman_breach_superior_abyssal_demon",
                ),
        )
        monster(
            name = "abyssal_sire",
            level = 85,
            npcs =
                listOf(
                    "npc.abyssalsire_sire_apocalypse", "npc.abyssalsire_sire_panicking",
                    "npc.abyssalsire_sire_puppet", "npc.abyssalsire_sire_stasis_awake",
                    "npc.abyssalsire_sire_stasis_sleeping",
                    "npc.abyssalsire_sire_stasis_stunned", "npc.abyssalsire_sire_wandering",
                ),
        )
        monster(
            name = "cave_kraken",
            level = 87,
            npcs =
                listOf(
                    "npc.slayer_kraken",
                ),
        )
        monster(
            name = "kraken",
            level = 87,
            npcs =
                listOf(
                    "npc.slayer_kraken_boss",
                ),
        )
        monster(
            name = "dark_beast",
            level = 90,
            npcs =
                listOf(
                    "npc.kourend_dark_beast", "npc.mourning_dark_beast",
                    "npc.superior_dark_beast", "npc.league_superior_dark_beast",
                    "npc.deadman_breach_superior_dark_beast",
                ),
        )
        monster(
            name = "cerberus",
            level = 91,
            npcs =
                listOf(
                    "npc.cerberus_attacking", "npc.cerberus_resetting", "npc.cerberus_sitting",
                ),
        )
        monster(
            name = "araxyte",
            level = 92,
            npcs =
                listOf(
                    "npc.araxyte_normal", "npc.araxyte_normal_bigwander", "npc.araxyte_large",
                    "npc.araxyte_large_bigwander", "npc.superior_araxyte",
                ),
        )
        monster(
            name = "araxxor",
            level = 92,
            npcs =
                listOf(
                    "npc.araxxor",
                ),
        )
        monster(
            name = "smoke_devil",
            level = 93,
            gear = "facemask_smoke",
            npcs =
                listOf(
                    "npc.smoke_devil", "npc.superior_smoke_devil",
                    "npc.league_superior_smoke_devil",
                ),
        )
        monster(
            name = "thermonuclear_smoke_devil",
            level = 93,
            gear = "facemask_smoke",
            npcs =
                listOf(
                    "npc.smoke_devil_boss",
                ),
        )
        monster(
            name = "hydra",
            level = 95,
            npcs =
                listOf(
                    "npc.hydra", "npc.superior_hydra", "npc.league_superior_hydra",
                ),
        )
        monster(
            name = "alchemical_hydra",
            level = 95,
            npcs =
                listOf(
                    "npc.hydraboss", "npc.hydraboss_p1_transition", "npc.hydraboss_4",
                    "npc.hydraboss_p2_transition", "npc.hydraboss_3",
                    "npc.hydraboss_p3_transition", "npc.hydraboss_2", "npc.hydraboss_finaldeath",
                ),
        )
        monster(
            name = "moonlight_cockatrice",
            level = 1,
            gear = "mirror_cockatrice",
            npcs =
                listOf(
                    "npc.varlamore_cockatrice_moon01",
                ),
        )
        }

    fun gear() =
        dbTable("dbtable.slayer_gear", serverOnly = true) {
            column("items", GEAR_ITEMS, VarType.OBJ)
            column("fixed_hit", GEAR_FIXED_HIT, VarType.INT)
            column("max_hit", GEAR_MAX_HIT, VarType.INT)
            column("hitpoints_percent", GEAR_HITPOINTS_PERCENT, VarType.INT)
            column("guaranteed", GEAR_GUARANTEED, VarType.BOOLEAN)
            column("penetration", GEAR_PENETRATION, VarType.INT)
            column("drains", GEAR_DRAINS, VarType.STAT, VarType.INT)
            column("first_hit_only", GEAR_FIRST_HIT_ONLY, VarType.BOOLEAN)
            column("repel_message", GEAR_REPEL_MESSAGE, VarType.STRING)

            gear(
                name = "nose_peg",
                items = slayerHelmetsAnd("obj.slayer_nosepeg"),
                fixedHit = 14,
                penetration = 100,
                drains = NOSE_PEG_DRAINS,
            )
            gear(
                name = "nose_peg_drain",
                items = slayerHelmetsAnd("obj.slayer_nosepeg"),
                drains = NOSE_PEG_DRAINS,
            )
            gear(
                name = "earmuffs",
                items = slayerHelmetsAnd("obj.slayer_earmuffs"),
                fixedHit = 6,
                drains = EARMUFFS_DRAINS,
            )
            gear(
                name = "earmuffs_drain",
                items = slayerHelmetsAnd("obj.slayer_earmuffs"),
                drains = EARMUFFS_DRAINS,
            )
            gear(
                name = "facemask_dust",
                items = slayerHelmetsAnd("obj.slayer_facemask"),
                fixedHit = 16,
                drains = DUST_DEVIL_DRAINS,
            )
            gear(
                name = "facemask_dust_drain",
                items = slayerHelmetsAnd("obj.slayer_facemask"),
                drains = DUST_DEVIL_DRAINS,
            )
            gear(
                name = "facemask_smoke",
                items = slayerHelmetsAnd("obj.slayer_facemask"),
                drains = SMOKE_DEVIL_DRAINS,
            )
            gear(
                name = "mirror_cockatrice",
                items = MIRROR_SHIELDS,
                drains = gazeDrains(combat = 40, other = 20),
            )
            gear(
                name = "mirror_basilisk",
                items = MIRROR_SHIELDS,
                drains = gazeDrains(combat = 60, other = 30),
            )
            gear(
                name = "mirror_basilisk_knight",
                items = MIRROR_SHIELDS,
                drains = gazeDrains(combat = 70, other = 35),
            )
            gear(
                name = "spiny_helmet",
                items = slayerHelmetsAnd("obj.wallbeast_spike_helmet"),
                maxHit = 18,
                guaranteed = true,
                firstHitOnly = true,
                repelMessage = "Your helmet repels the wall beast!",
            )
            gear(
                name = "witchwood_icon",
                items = listOf("obj.witchwood_icon"),
                hitpointsPercent = 10,
            )
            gear(name = "insulated_boots", items = listOf("obj.slayer_boots"), maxHit = 15)
        }

    fun finishers() =
        dbTable("dbtable.slayer_finishers", serverOnly = true) {
            column("items", FINISHER_ITEMS, VarType.OBJ)
            column("consumed", FINISHER_CONSUMED, VarType.OBJ)
            column("charges", FINISHER_CHARGES, VarType.OBJ)
            column("auto_worn", FINISHER_AUTO_WORN, VarType.OBJ)
            column("bypass_worn", FINISHER_BYPASS_WORN, VarType.OBJ)
            column("threshold", FINISHER_THRESHOLD, VarType.INT)
            column("unlock", FINISHER_UNLOCK, VarType.STRING)
            column("wasted_early", FINISHER_WASTED_EARLY, VarType.BOOLEAN)
            column("thrown", FINISHER_THROWN, VarType.OBJ)

            finisher(
                name = "gargoyle",
                items =
                    listOf(
                        "obj.slayer_rock_hammer",
                        "obj.slayer_rock_thrownhammer",
                        "obj.granite_hammer",
                    ),
                consumed = "obj.slayer_rock_thrownhammer",
                autoWorn = "obj.granite_hammer",
                thrown = "obj.slayer_rock_thrownhammer",
                threshold = 9,
                unlock = "Gargoyle Smasher",
            )
            finisher(
                name = "rockslug",
                items = listOf("obj.slayer_bag_of_salt"),
                consumed = "obj.slayer_bag_of_salt",
                bypassWorn = "obj.olaf2_brine_sabre",
                threshold = 4,
                unlock = "Slug Salter",
                wastedEarly = true,
            )
            finisher(
                name = "desert_lizard",
                items = listOf("obj.slayer_icy_water"),
                consumed = "obj.slayer_icy_water",
                threshold = 4,
                unlock = "Reptile Freezer",
                wastedEarly = true,
            )
            finisher(
                name = "zygomite",
                charges = (10 downTo 0).map { "obj.slayer_spray_pump_$it" },
                threshold = 8,
                unlock = "'Shroom Sprayer",
            )
        }

    fun weaponRules() =
        dbTable("dbtable.slayer_weapon_rules", serverOnly = true) {
            column("weapons", WEAPON_WEAPONS, VarType.OBJ)
            column("ammo", WEAPON_AMMO, VarType.OBJ)
            column("spells", WEAPON_SPELLS, VarType.OBJ)

            weaponRule(
                name = "leafy",
                weapons =
                    listOf(
                        "obj.slayer_leafbladed_spear",
                        "obj.leafbladed_sword",
                        "obj.leafbladed_battleaxe",
                    ),
                ammo =
                    listOf(
                        "obj.slayer_broad_arrows",
                        "obj.slayer_broad_arrows_2",
                        "obj.slayer_broad_arrows_3",
                        "obj.slayer_broad_arrows_4",
                        "obj.slayer_broad_arrows_5",
                        "obj.seeking_slayer_broad_arrows",
                        "obj.seeking_slayer_broad_arrows_2",
                        "obj.seeking_slayer_broad_arrows_3",
                        "obj.seeking_slayer_broad_arrows_4",
                        "obj.seeking_slayer_broad_arrows_5",
                        "obj.slayer_broad_bolt",
                        "obj.slayer_broad_bolt_amethyst",
                    ),
                spells = listOf("obj.50_magic_dart", "obj.50_slayer_dart"),
            )
        }

    private fun DBTableBuilder.monster(
        name: String,
        level: Int,
        npcs: List<String>,
        gear: String? = null,
        finisher: String? = null,
        weaponRule: String? = null,
    ) {
        row("dbrow.slayer_monster_$name") {
            columnRSCM(MONSTER_NPCS, *npcs.toTypedArray())
            column(MONSTER_LEVEL, level)
            gear?.let { columnRSCM(MONSTER_GEAR, "dbrow.slayer_gear_$it") }
            finisher?.let { columnRSCM(MONSTER_FINISHER, "dbrow.slayer_finisher_$it") }
            weaponRule?.let { columnRSCM(MONSTER_WEAPON_RULE, "dbrow.slayer_weapon_$it") }
        }
    }

    private fun DBTableBuilder.gear(
        name: String,
        items: List<String>,
        fixedHit: Int? = null,
        maxHit: Int? = null,
        hitpointsPercent: Int? = null,
        guaranteed: Boolean = false,
        penetration: Int = 0,
        drains: List<Pair<String, Int>> = emptyList(),
        firstHitOnly: Boolean = false,
        repelMessage: String? = null,
    ) {
        row("dbrow.slayer_gear_$name") {
            columnRSCM(GEAR_ITEMS, *items.toTypedArray())
            fixedHit?.let { column(GEAR_FIXED_HIT, it) }
            maxHit?.let { column(GEAR_MAX_HIT, it) }
            hitpointsPercent?.let { column(GEAR_HITPOINTS_PERCENT, it) }
            if (guaranteed) column(GEAR_GUARANTEED, true)
            if (penetration > 0) column(GEAR_PENETRATION, penetration)
            if (drains.isNotEmpty()) drainColumn(drains)
            if (firstHitOnly) column(GEAR_FIRST_HIT_ONLY, true)
            repelMessage?.let { column(GEAR_REPEL_MESSAGE, it) }
        }
    }

    private fun DBRowBuilder.drainColumn(drains: List<Pair<String, Int>>) {
        val values =
            drains.flatMap { (stat, percent) -> listOf(ConstantProvider.getMapping(stat), percent) }
        column(GEAR_DRAINS, *values.toTypedArray())
    }

    private fun DBTableBuilder.finisher(
        name: String,
        threshold: Int,
        unlock: String,
        items: List<String> = emptyList(),
        consumed: String? = null,
        charges: List<String> = emptyList(),
        autoWorn: String? = null,
        bypassWorn: String? = null,
        wastedEarly: Boolean = false,
        thrown: String? = null,
    ) {
        row("dbrow.slayer_finisher_$name") {
            if (items.isNotEmpty()) columnRSCM(FINISHER_ITEMS, *items.toTypedArray())
            consumed?.let { columnRSCM(FINISHER_CONSUMED, it) }
            if (charges.isNotEmpty()) columnRSCM(FINISHER_CHARGES, *charges.toTypedArray())
            autoWorn?.let { columnRSCM(FINISHER_AUTO_WORN, it) }
            bypassWorn?.let { columnRSCM(FINISHER_BYPASS_WORN, it) }
            column(FINISHER_THRESHOLD, threshold)
            column(FINISHER_UNLOCK, unlock)
            if (wastedEarly) column(FINISHER_WASTED_EARLY, true)
            thrown?.let { columnRSCM(FINISHER_THROWN, it) }
        }
    }

    private fun DBTableBuilder.weaponRule(
        name: String,
        weapons: List<String>,
        ammo: List<String>,
        spells: List<String>,
    ) {
        row("dbrow.slayer_weapon_$name") {
            columnRSCM(WEAPON_WEAPONS, *weapons.toTypedArray())
            columnRSCM(WEAPON_AMMO, *ammo.toTypedArray())
            columnRSCM(WEAPON_SPELLS, *spells.toTypedArray())
        }
    }

    private fun gazeDrains(combat: Int, other: Int): List<Pair<String, Int>> =
        listOf(
            "stat.attack" to combat,
            "stat.strength" to combat,
            "stat.magic" to combat,
            "stat.ranged" to combat,
            "stat.defence" to other,
            "stat.prayer" to other,
            "stat.agility" to other,
        )
}
