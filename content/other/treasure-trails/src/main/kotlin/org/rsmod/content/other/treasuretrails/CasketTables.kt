package org.rsmod.content.other.treasuretrails

import dtx.rs.RSWeightedTable
import org.rsmod.api.droptable.DropRollItem
import org.rsmod.api.droptable.rsPlayerWeightedTable
import org.rsmod.game.entity.Player

object CasketTables {
    fun forDifficulty(difficulty: Int): RSWeightedTable<Player, DropRollItem>? =
        when (difficulty) {
            0 -> beginner
            1 -> easy
            2 -> medium
            3 -> HighCasketTables.hard
            4 -> HighCasketTables.elite
            5 -> MasterCasketTables.master
            else -> null
        }

    private val beginnerUniques =
        rsPlayerWeightedTable(total = 30) {
            name("Beginner clue uniques")
            15 weight "obj.cert_cabbage" count 5..9
            1 weight "obj.mole_slippers" count 1
            1 weight "obj.frog_slippers" count 1
            1 weight "obj.bear_slippers" count 1
            1 weight "obj.demon_slippers" count 1
            1 weight "obj.jester_cape" count 1
            1 weight "obj.shoulder_parrot" count 1
            1 weight "obj.monk_robetop_t" count 1
            1 weight "obj.monk_robebottom_t" count 1
            1 weight "obj.amulet_of_defence_t" count 1
            1 weight "obj.sandwich_lady_hat" count 1
            1 weight "obj.sandwich_lady_top" count 1
            1 weight "obj.sandwich_lady_bottom" count 1
            1 weight "obj.rune_scimitar_ornament_kit_guthix" count 1
            1 weight "obj.rune_scimitar_ornament_kit_saradomin" count 1
            1 weight "obj.rune_scimitar_ornament_kit_zamorak" count 1
        }

    private val beginnerBlackItems =
        rsPlayerWeightedTable(total = 18) {
            name("Beginner black items")
            1 weight "obj.black_2h_sword" count 1
            1 weight "obj.black_axe" count 1
            1 weight "obj.black_battleaxe" count 1
            1 weight "obj.black_chainbody" count 1
            1 weight "obj.black_dagger" count 1
            1 weight "obj.black_full_helm" count 1
            1 weight "obj.black_kiteshield" count 1
            1 weight "obj.black_longsword" count 1
            1 weight "obj.black_mace" count 1
            1 weight "obj.black_med_helm" count 1
            1 weight "obj.black_pickaxe" count 1
            1 weight "obj.black_platebody" count 1
            1 weight "obj.black_plateskirt" count 1
            1 weight "obj.black_platelegs" count 1
            1 weight "obj.black_sq_shield" count 1
            1 weight "obj.black_scimitar" count 1
            1 weight "obj.black_sword" count 1
            1 weight "obj.black_warhammer" count 1
        }

    private val beginner =
        rsPlayerWeightedTable(total = 492) {
            name("Reward casket (beginner)")
            41 weight beginnerUniques
            11 weight beginnerBlackItems
            11 weight "obj.shortbow" count 1
            11 weight "obj.longbow" count 1
            11 weight "obj.oak_shortbow" count 1
            11 weight "obj.oak_longbow" count 1
            11 weight "obj.iron_pickaxe" count 1
            11 weight "obj.staff_of_air" count 1
            11 weight "obj.staff_of_water" count 1
            11 weight "obj.staff_of_earth" count 1
            11 weight "obj.staff_of_fire" count 1
            11 weight "obj.steel_full_helm" count 1
            11 weight "obj.steel_platebody" count 1
            11 weight "obj.steel_platelegs" count 1
            11 weight "obj.steel_plateskirt" count 1
            11 weight "obj.steel_longsword" count 1
            11 weight "obj.steel_dagger" count 1
            11 weight "obj.steel_axe" count 1
            11 weight "obj.steel_battleaxe" count 1
            11 weight "obj.leather_cowl" count 1
            11 weight "obj.leather_armour" count 1
            11 weight "obj.leather_chaps" count 1
            11 weight "obj.leather_vambraces" count 1
            11 weight "obj.hardleather_body" count 1
            11 weight "obj.bluewizhat" count 1
            11 weight "obj.wizards_robe" count 1
            11 weight "obj.blackwizhat" count 1
            11 weight "obj.black_robe" count 1
            11 weight "obj.airrune" count 15..35
            11 weight "obj.mindrune" count 15..35
            11 weight "obj.waterrune" count 15..35
            11 weight "obj.earthrune" count 15..35
            11 weight "obj.firerune" count 15..35
            11 weight "obj.bodyrune" count 15..35
            11 weight "obj.chaosrune" count 2..7
            11 weight "obj.naturerune" count 2..7
            11 weight "obj.lawrune" count 2..7
            11 weight "obj.bronze_arrow" count 15..30
            11 weight "obj.iron_arrow" count 7..15
            11 weight "obj.cert_shrimp" count 5..14
            11 weight "obj.cert_sardine" count 5..12
            11 weight "obj.cert_herring" count 5..9
        }

    internal val blessings =
        rsPlayerWeightedTable(total = 6) {
            name("Clue blessings")
            1 weight "obj.blessing_saradomin" count 1
            1 weight "obj.blessing_zamorak" count 1
            1 weight "obj.blessing_guthix" count 1
            1 weight "obj.blessing_bandos" count 1
            1 weight "obj.blessing_armadyl" count 1
            1 weight "obj.blessing_zaros" count 1
        }

    private val teleportScrolls =
        rsPlayerWeightedTable(total = 12) {
            name("Clue teleport scrolls")
            1 weight "obj.scroll_charge_dragonstone" count 5..15
            1 weight "obj.teleportscroll_nardah" count 5..15
            1 weight "obj.teleportscroll_mosles" count 5..15
            1 weight "obj.teleportscroll_mortton" count 5..15
            1 weight "obj.teleportscroll_feldip" count 5..15
            1 weight "obj.teleportscroll_lunarisle" count 5..15
            1 weight "obj.teleportscroll_digsite" count 5..15
            1 weight "obj.teleportscroll_piscatoris" count 5..15
            1 weight "obj.teleportscroll_pestcontrol" count 5..15
            1 weight "obj.teleportscroll_taibwo" count 5..15
            1 weight "obj.teleportscroll_lumberyard" count 5..15
            1 weight "obj.teleportscroll_elf" count 5..15
        }

    internal val scrollsOrBook =
        rsPlayerWeightedTable(total = 22) {
            name("Clue scrolls or master scroll book")
            21 weight teleportScrolls
            1 weight "obj.bookofscrolls_empty" count 1
        }

    internal val prayerPages =
        rsPlayerWeightedTable(total = 24) {
            name("Clue prayer book pages")
            1 weight "obj.holy_book_s_page1" count 1
            1 weight "obj.holy_book_s_page2" count 1
            1 weight "obj.holy_book_s_page3" count 1
            1 weight "obj.holy_book_s_page4" count 1
            1 weight "obj.holy_book_z_page1" count 1
            1 weight "obj.holy_book_z_page2" count 1
            1 weight "obj.holy_book_z_page3" count 1
            1 weight "obj.holy_book_z_page4" count 1
            1 weight "obj.holy_book_g_page1" count 1
            1 weight "obj.holy_book_g_page2" count 1
            1 weight "obj.holy_book_g_page3" count 1
            1 weight "obj.holy_book_g_page4" count 1
            1 weight "obj.bandos_page1" count 1
            1 weight "obj.bandos_page2" count 1
            1 weight "obj.bandos_page3" count 1
            1 weight "obj.bandos_page4" count 1
            1 weight "obj.armadyl_page1" count 1
            1 weight "obj.armadyl_page2" count 1
            1 weight "obj.armadyl_page3" count 1
            1 weight "obj.armadyl_page4" count 1
            1 weight "obj.zaros_page1" count 1
            1 weight "obj.zaros_page2" count 1
            1 weight "obj.zaros_page3" count 1
            1 weight "obj.zaros_page4" count 1
        }

    private val easyFirelighters =
        rsPlayerWeightedTable(total = 5) {
            name("Easy clue firelighters")
            1 weight "obj.gnomish_firelighter_red" count 3..5
            1 weight "obj.gnomish_firelighter_green" count 3..5
            1 weight "obj.gnomish_firelighter_blue" count 3..5
            1 weight "obj.trail_gnomish_firelighter_purple" count 3..5
            1 weight "obj.trail_gnomish_firelighter_white" count 3..5
        }

    private val mediumColouredFirelighters =
        rsPlayerWeightedTable(total = 5) {
            name("Medium clue coloured firelighters")
            1 weight "obj.gnomish_firelighter_red" count 4..10
            1 weight "obj.gnomish_firelighter_green" count 4..10
            1 weight "obj.gnomish_firelighter_blue" count 4..10
            1 weight "obj.trail_gnomish_firelighter_purple" count 4..10
            1 weight "obj.trail_gnomish_firelighter_white" count 4..10
        }

    private val mediumFirelighters =
        rsPlayerWeightedTable(total = 10) {
            name("Medium clue firelighters")
            9 weight mediumColouredFirelighters
            1 weight "obj.gnomish_firelighter" count 1
        }

    private val easyTeamCapes =
        rsPlayerWeightedTable(total = 4) {
            name("Easy clue team capes")
            1 weight "obj.wilderness_cape_zero" count 1
            1 weight "obj.wilderness_cape_i" count 1
            1 weight "obj.wilderness_cape_x" count 1
            1 weight "obj.cape_of_skulls" count 1
        }

    private val easyGoldenChef =
        rsPlayerWeightedTable(total = 2) {
            name("Easy clue golden chef")
            1 weight "obj.chefs_hat_gold" count 1
            1 weight "obj.golden_apron" count 1
        }

    private val easyMonkRobes =
        rsPlayerWeightedTable(total = 2) {
            name("Easy clue gilded monk robes")
            1 weight "obj.monk_robetop_gold" count 1
            1 weight "obj.monk_robebottom_gold" count 1
        }

    private val easyCoinsOrMonk =
        rsPlayerWeightedTable(total = 5) {
            name("Easy clue coins or monk robes")
            4 weight "obj.coins" count 3400..8200
            1 weight easyMonkRobes
        }

    private val easyElegant =
        rsPlayerWeightedTable(total = 12) {
            name("Easy clue elegant clothing")
            1 weight "obj.trail_elegant_shirt_male_r" count 1
            1 weight "obj.trail_elegant_shirt_female_r" count 1
            1 weight "obj.trail_elegant_pants_male_r" count 1
            1 weight "obj.trail_elegant_pants_female_r" count 1
            1 weight "obj.trail_elegant_shirt_male_g" count 1
            1 weight "obj.trail_elegant_shirt_female_g" count 1
            1 weight "obj.trail_elegant_pants_male_g" count 1
            1 weight "obj.trail_elegant_pants_female_g" count 1
            1 weight "obj.trail_elegant_shirt_male_b" count 1
            1 weight "obj.trail_elegant_shirt_female_b" count 1
            1 weight "obj.trail_elegant_pants_male_b" count 1
            1 weight "obj.trail_elegant_pants_female_b" count 1
        }

    private val easyUniques =
        rsPlayerWeightedTable(total = 117) {
            name("Easy clue uniques")
            1 weight easyTeamCapes
            1 weight easyGoldenChef
            1 weight easyCoinsOrMonk
            6 weight easyElegant
            1 weight "obj.wooden_shield_gold" count 1
            1 weight "obj.black_full_helm_trim" count 1
            1 weight "obj.black_platebody_trim" count 1
            1 weight "obj.black_platelegs_trim" count 1
            1 weight "obj.black_plateskirt_trim" count 1
            1 weight "obj.black_kiteshield_trim" count 1
            1 weight "obj.black_full_helm_gold" count 1
            1 weight "obj.black_platebody_gold" count 1
            1 weight "obj.black_platelegs_gold" count 1
            1 weight "obj.black_plateskirt_gold" count 1
            1 weight "obj.black_kiteshield_gold" count 1
            1 weight "obj.black_heraldic_kiteshield1" count 1
            1 weight "obj.black_heraldic_kiteshield2" count 1
            1 weight "obj.black_heraldic_kiteshield3" count 1
            1 weight "obj.black_heraldic_kiteshield4" count 1
            1 weight "obj.black_heraldic_kiteshield5" count 1
            1 weight "obj.trail_heraldic_helm_1_black" count 1
            1 weight "obj.trail_heraldic_helm_2_black" count 1
            1 weight "obj.trail_heraldic_helm_3_black" count 1
            1 weight "obj.trail_heraldic_helm_4_black" count 1
            1 weight "obj.trail_heraldic_helm_5_black" count 1
            1 weight "obj.black_platebody_h1" count 1
            1 weight "obj.black_platebody_h2" count 1
            1 weight "obj.black_platebody_h3" count 1
            1 weight "obj.black_platebody_h4" count 1
            1 weight "obj.black_platebody_h5" count 1
            1 weight "obj.steel_full_helm_trim" count 1
            1 weight "obj.steel_platebody_trim" count 1
            1 weight "obj.steel_platelegs_trim" count 1
            1 weight "obj.steel_plateskirt_trim" count 1
            1 weight "obj.steel_kiteshield_trim" count 1
            1 weight "obj.steel_full_helm_gold" count 1
            1 weight "obj.steel_platebody_gold" count 1
            1 weight "obj.steel_platelegs_gold" count 1
            1 weight "obj.steel_plateskirt_gold" count 1
            1 weight "obj.steel_kiteshield_gold" count 1
            1 weight "obj.iron_full_helm_trim" count 1
            1 weight "obj.iron_platebody_trim" count 1
            1 weight "obj.iron_platelegs_trim" count 1
            1 weight "obj.iron_plateskirt_trim" count 1
            1 weight "obj.iron_kiteshield_trim" count 1
            1 weight "obj.iron_full_helm_gold" count 1
            1 weight "obj.iron_platebody_gold" count 1
            1 weight "obj.iron_platelegs_gold" count 1
            1 weight "obj.iron_plateskirt_gold" count 1
            1 weight "obj.iron_kiteshield_gold" count 1
            1 weight "obj.bronze_full_helm_trim" count 1
            1 weight "obj.bronze_platebody_trim" count 1
            1 weight "obj.bronze_platelegs_trim" count 1
            1 weight "obj.bronze_plateskirt_trim" count 1
            1 weight "obj.bronze_kiteshield_trim" count 1
            1 weight "obj.bronze_full_helm_gold" count 1
            1 weight "obj.bronze_platebody_gold" count 1
            1 weight "obj.bronze_platelegs_gold" count 1
            1 weight "obj.bronze_plateskirt_gold" count 1
            1 weight "obj.bronze_kiteshield_gold" count 1
            1 weight "obj.studded_body_trim_gold" count 1
            1 weight "obj.studded_chaps_trim_gold" count 1
            1 weight "obj.studded_body_trim_fur" count 1
            1 weight "obj.studded_chaps_trim_fur" count 1
            1 weight "obj.leather_armour_trim_gold" count 1
            1 weight "obj.leather_chaps_trim_gold" count 1
            1 weight "obj.bluewizhat_trim_gold" count 1
            1 weight "obj.wizards_robe_trim_gold" count 1
            1 weight "obj.blue_skirt_trim_gold" count 1
            1 weight "obj.bluewizhat_trim" count 1
            1 weight "obj.wizards_robe_trim" count 1
            1 weight "obj.blue_skirt_trim" count 1
            1 weight "obj.blackwizhat_gold" count 1
            1 weight "obj.black_wizards_robe_gold" count 1
            1 weight "obj.black_skirt_gold" count 1
            1 weight "obj.blackwizhat_trim" count 1
            1 weight "obj.black_wizards_robe_trim" count 1
            1 weight "obj.black_skirt_trim" count 1
            1 weight "obj.trail_saradomin_robe_t" count 1
            1 weight "obj.trail_saradomin_robe_l" count 1
            1 weight "obj.trail_guthix_robe_t" count 1
            1 weight "obj.trail_guthix_robe_l" count 1
            1 weight "obj.trail_zamorak_robe_t" count 1
            1 weight "obj.trail_zamorak_robe_l" count 1
            1 weight "obj.trail_ancient_robe_t" count 1
            1 weight "obj.trail_ancient_robe_l" count 1
            1 weight "obj.trail_armadyl_robe_t" count 1
            1 weight "obj.trail_armadyl_robe_l" count 1
            1 weight "obj.trail_bandos_robe_t" count 1
            1 weight "obj.trail_bandos_robe_l" count 1
            1 weight "obj.trail_bob_shirt_red" count 1
            1 weight "obj.trail_bob_shirt_green" count 1
            1 weight "obj.trail_bob_shirt_blue" count 1
            1 weight "obj.trail_bob_shirt_black" count 1
            1 weight "obj.trail_bob_shirt_purple" count 1
            1 weight "obj.highwaymanmask" count 1
            1 weight "obj.berret_blue" count 1
            1 weight "obj.berret_black" count 1
            1 weight "obj.berret_red" count 1
            1 weight "obj.berret_white" count 1
            1 weight "obj.trail_wig" count 1
            1 weight "obj.beanie_hat" count 1
            1 weight "obj.imp_mask" count 1
            1 weight "obj.goblin_mask" count 1
            1 weight "obj.trail_sleeping_cap" count 1
            1 weight "obj.trail_flared_pants" count 1
            1 weight "obj.trail_pantaloons" count 1
            1 weight "obj.black_cane" count 1
            1 weight "obj.staff_of_bobcat" count 1
            1 weight "obj.trail_power_ammy" count 1
            1 weight "obj.joint_of_ham" count 1
            1 weight "obj.rain_bow" count 1
        }

    private val easyWillowLongbow =
        rsPlayerWeightedTable(total = 10) {
            name("Easy clue willow longbow")
            9 weight "obj.willow_longbow" count 1
            1 weight "obj.trail_composite_bow_willow" count 1
        }

    private val easyAmuletOfMagic =
        rsPlayerWeightedTable(total = 10) {
            name("Easy clue amulet of magic")
            9 weight "obj.amulet_of_magic" count 1
            1 weight "obj.trail_amulet_of_magic" count 1
        }

    private val easyShared =
        rsPlayerWeightedTable(total = 60) {
            name("Easy clue shared rewards")
            40 weight "obj.coins" count 3000..7000
            7 weight "obj.coins" count 10000..15000
            7 weight "obj.trail_sweets" count 8..12
            6 weight blessings
        }

    private val easyStandard =
        rsPlayerWeightedTable(total = 33) {
            name("Easy clue standard rewards")
            1 weight "obj.black_full_helm" count 1
            1 weight "obj.black_platebody" count 1
            1 weight "obj.black_platelegs" count 1
            1 weight "obj.black_longsword" count 1
            1 weight "obj.black_battleaxe" count 1
            1 weight "obj.black_axe" count 1
            1 weight "obj.black_dagger" count 1
            1 weight "obj.steel_pickaxe" count 1
            1 weight "obj.black_pickaxe" count 1
            1 weight "obj.coif" count 1
            1 weight "obj.studded_body" count 1
            1 weight "obj.studded_chaps" count 1
            1 weight "obj.willow_shortbow" count 1
            1 weight "obj.staff_of_air" count 1
            1 weight easyWillowLongbow
            1 weight easyAmuletOfMagic
            1 weight "obj.airrune" count 30..50
            1 weight "obj.mindrune" count 30..50
            1 weight "obj.waterrune" count 30..50
            1 weight "obj.earthrune" count 30..50
            1 weight "obj.firerune" count 30..50
            1 weight "obj.bodyrune" count 30..50
            1 weight "obj.chaosrune" count 5..10
            1 weight "obj.naturerune" count 5..10
            1 weight "obj.lawrune" count 5..10
            1 weight "obj.cert_trout" count 6..10
            1 weight "obj.cert_salmon" count 6..10
            1 weight "obj.coins" count 50..200
            1 weight "obj.trail_sweets" count 2..6
            1 weight easyShared
            1 weight easyFirelighters
            1 weight scrollsOrBook
            1 weight prayerPages
        }

    private val easy =
        rsPlayerWeightedTable(total = 12) {
            name("Reward casket (easy)")
            1 weight easyUniques
            11 weight easyStandard
        }

    private val mediumUnicornMasks =
        rsPlayerWeightedTable(total = 2) {
            name("Medium clue unicorn masks")
            1 weight "obj.black_unicorn_mask" count 1
            1 weight "obj.white_unicorn_mask" count 1
        }

    private val mediumElegant =
        rsPlayerWeightedTable(total = 16) {
            name("Medium clue elegant clothing")
            1 weight "obj.trail_elegant_shirt_male_p" count 1
            1 weight "obj.trail_elegant_shirt_female_p" count 1
            1 weight "obj.trail_elegant_pants_male_p" count 1
            1 weight "obj.trail_elegant_pants_female_p" count 1
            1 weight "obj.trail_elegant_shirt_male" count 1
            1 weight "obj.trail_elegant_shirt_female" count 1
            1 weight "obj.trail_elegant_pants_male" count 1
            1 weight "obj.trail_elegant_pants_female" count 1
            1 weight "obj.trail_elegant_shirt_male_pink" count 1
            1 weight "obj.trail_elegant_shirt_female_pink" count 1
            1 weight "obj.trail_elegant_pants_male_pink" count 1
            1 weight "obj.trail_elegant_pants_female_pink" count 1
            1 weight "obj.trail_elegant_shirt_male_gold" count 1
            1 weight "obj.trail_elegant_shirt_female_gold" count 1
            1 weight "obj.trail_elegant_pants_male_gold" count 1
            1 weight "obj.trail_elegant_pants_female_gold" count 1
        }

    private val mediumUniques =
        rsPlayerWeightedTable(total = 103) {
            name("Medium clue uniques")
            1 weight mediumUnicornMasks
            8 weight mediumElegant
            1 weight "obj.boots_ranger" count 1
            1 weight "obj.boots_wizard" count 1
            1 weight "obj.holy_sandals" count 1
            1 weight "obj.spiked_manacles" count 1
            1 weight "obj.climbing_boots_g" count 1
            1 weight "obj.adamant_full_helm_trim" count 1
            1 weight "obj.adamant_platebody_trim" count 1
            1 weight "obj.adamant_platelegs_trim" count 1
            1 weight "obj.adamant_plateskirt_trim" count 1
            1 weight "obj.adamant_kiteshield_trim" count 1
            1 weight "obj.adamant_full_helm_gold" count 1
            1 weight "obj.adamant_platebody_gold" count 1
            1 weight "obj.adamant_platelegs_gold" count 1
            1 weight "obj.adamant_plateskirt_gold" count 1
            1 weight "obj.adamant_kiteshield_gold" count 1
            1 weight "obj.adamant_heraldic_kiteshield1" count 1
            1 weight "obj.adamant_heraldic_kiteshield2" count 1
            1 weight "obj.adamant_heraldic_kiteshield3" count 1
            1 weight "obj.adamant_heraldic_kiteshield4" count 1
            1 weight "obj.adamant_heraldic_kiteshield5" count 1
            1 weight "obj.trail_heraldic_helm_1_adamant" count 1
            1 weight "obj.trail_heraldic_helm_2_adamant" count 1
            1 weight "obj.trail_heraldic_helm_3_adamant" count 1
            1 weight "obj.trail_heraldic_helm_4_adamant" count 1
            1 weight "obj.trail_heraldic_helm_5_adamant" count 1
            1 weight "obj.adamant_platebody_h1" count 1
            1 weight "obj.adamant_platebody_h2" count 1
            1 weight "obj.adamant_platebody_h3" count 1
            1 weight "obj.adamant_platebody_h4" count 1
            1 weight "obj.adamant_platebody_h5" count 1
            1 weight "obj.mithril_full_helm_gold" count 1
            1 weight "obj.mithril_platebody_gold" count 1
            1 weight "obj.mithril_platelegs_gold" count 1
            1 weight "obj.mithril_plateskirt_gold" count 1
            1 weight "obj.mithril_kiteshield_gold" count 1
            1 weight "obj.mithril_full_helm_trim" count 1
            1 weight "obj.mithril_platebody_trim" count 1
            1 weight "obj.mithril_platelegs_trim" count 1
            1 weight "obj.mithril_plateskirt_trim" count 1
            1 weight "obj.mithril_kiteshield_trim" count 1
            1 weight "obj.dragonhide_body_trim_gold" count 1
            1 weight "obj.dragonhide_body_trim" count 1
            1 weight "obj.dragonhide_chaps_trim_gold" count 1
            1 weight "obj.dragonhide_chaps_trim" count 1
            1 weight "obj.trail_saradomin_mitre" count 1
            1 weight "obj.trail_saradomin_cloak" count 1
            1 weight "obj.trail_guthix_mitre" count 1
            1 weight "obj.trail_guthix_cloak" count 1
            1 weight "obj.trail_zamorak_mitre" count 1
            1 weight "obj.trail_zamorak_cloak" count 1
            1 weight "obj.trail_ancient_mitre" count 1
            1 weight "obj.trail_ancient_cloak" count 1
            1 weight "obj.trail_ancient_scarf" count 1
            1 weight "obj.trail_ancient_staff" count 1
            1 weight "obj.trail_armadyl_mitre" count 1
            1 weight "obj.trail_armadyl_cloak" count 1
            1 weight "obj.trail_armadyl_scarf" count 1
            1 weight "obj.trail_armadyl_staff" count 1
            1 weight "obj.trail_bandos_mitre" count 1
            1 weight "obj.trail_bandos_cloak" count 1
            1 weight "obj.trail_bandos_scarf" count 1
            1 weight "obj.trail_bandos_staff" count 1
            1 weight "obj.strawboater_red" count 1
            1 weight "obj.strawboater_green" count 1
            1 weight "obj.strawboater_orange" count 1
            1 weight "obj.strawboater_black" count 1
            1 weight "obj.strawboater_blue" count 1
            1 weight "obj.strawboater_pink" count 1
            1 weight "obj.strawboater_purple" count 1
            1 weight "obj.strawboater_white" count 1
            1 weight "obj.headband_red" count 1
            1 weight "obj.headband_black" count 1
            1 weight "obj.headband_brown" count 1
            1 weight "obj.headband_white" count 1
            1 weight "obj.headband_blue" count 1
            1 weight "obj.headband_gold" count 1
            1 weight "obj.headband_pink" count 1
            1 weight "obj.headband_green" count 1
            1 weight "obj.trail_crier_hat" count 1
            1 weight "obj.town_crier_coat" count 1
            1 weight "obj.town_crier_bell" count 1
            1 weight "obj.adamant_cane" count 1
            1 weight "obj.arceuus_banner" count 1
            1 weight "obj.piscarilius_banner" count 1
            1 weight "obj.hosidius_banner" count 1
            1 weight "obj.shayzien_banner" count 1
            1 weight "obj.lovakengj_banner" count 1
            1 weight "obj.cabbage_shield" count 1
            1 weight "obj.cat_mask" count 1
            1 weight "obj.penguin_mask" count 1
            1 weight "obj.leprechaun_hat" count 1
            1 weight "obj.black_leprechaun_hat" count 1
            1 weight "obj.wolf_mask" count 1
            1 weight "obj.wolf_cloak" count 1
        }

    private val mediumYewLongbow =
        rsPlayerWeightedTable(total = 10) {
            name("Medium clue yew longbow")
            9 weight "obj.yew_longbow" count 1
            1 weight "obj.trail_composite_bow_yew" count 1
        }

    private val mediumAmuletOfPower =
        rsPlayerWeightedTable(total = 10) {
            name("Medium clue amulet of power")
            9 weight "obj.amulet_of_power" count 1
            1 weight "obj.trail_amulet_of_strength" count 1
        }

    private val mediumShared =
        rsPlayerWeightedTable(total = 20) {
            name("Medium clue shared rewards")
            7 weight "obj.coins" count 10000..15000
            7 weight "obj.trail_sweets" count 8..12
            6 weight blessings
        }

    private val mediumStandard =
        rsPlayerWeightedTable(total = 31) {
            name("Medium clue standard rewards")
            1 weight "obj.adamant_full_helm" count 1
            1 weight "obj.adamant_platebody" count 1
            1 weight "obj.adamant_platelegs" count 1
            1 weight "obj.adamant_longsword" count 1
            1 weight "obj.adamant_dagger" count 1
            1 weight "obj.adamant_battleaxe" count 1
            1 weight "obj.adamant_axe" count 1
            1 weight "obj.adamant_pickaxe" count 1
            1 weight "obj.dragonhide_body" count 1
            1 weight "obj.dragonhide_chaps" count 1
            1 weight "obj.yew_shortbow" count 1
            1 weight "obj.fire_battlestaff" count 1
            1 weight mediumYewLongbow
            1 weight mediumAmuletOfPower
            1 weight "obj.airrune" count 50..100
            1 weight "obj.mindrune" count 50..100
            1 weight "obj.waterrune" count 50..100
            1 weight "obj.earthrune" count 50..100
            1 weight "obj.firerune" count 50..100
            1 weight "obj.chaosrune" count 10..20
            1 weight "obj.naturerune" count 10..20
            1 weight "obj.lawrune" count 10..20
            1 weight "obj.deathrune" count 10..20
            1 weight "obj.cert_lobster" count 8..12
            1 weight "obj.cert_swordfish" count 8..12
            1 weight "obj.coins" count 200..1000
            1 weight "obj.trail_sweets" count 5..10
            1 weight mediumShared
            1 weight mediumFirelighters
            1 weight scrollsOrBook
            1 weight prayerPages
        }

    private val medium =
        rsPlayerWeightedTable(total = 11) {
            name("Reward casket (medium)")
            1 weight mediumUniques
            10 weight mediumStandard
        }
}
