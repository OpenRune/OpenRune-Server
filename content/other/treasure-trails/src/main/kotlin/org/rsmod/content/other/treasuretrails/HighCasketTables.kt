package org.rsmod.content.other.treasuretrails

import dtx.rs.RSWeightedTable
import org.rsmod.api.droptable.DropRollItem
import org.rsmod.api.droptable.rsPlayerWeightedTable
import org.rsmod.game.entity.Player

internal object HighCasketTables {
    val hard: RSWeightedTable<Player, DropRollItem>
        get() = hardTable

    val elite: RSWeightedTable<Player, DropRollItem>
        get() = eliteTable

    private val hardGilded =
        rsPlayerWeightedTable(total = 11) {
            name("Hard clue gilded armour")
            1 weight "obj.rune_full_helm_goldplate" count 1
            1 weight "obj.rune_platebody_goldplate" count 1
            1 weight "obj.rune_platelegs_goldplate" count 1
            1 weight "obj.rune_plateskirt_goldplate" count 1
            1 weight "obj.rune_kiteshield_goldplate" count 1
            1 weight "obj.rune_med_helm_gold" count 1
            1 weight "obj.rune_chainbody_gold" count 1
            1 weight "obj.rune_sq_shield_gold" count 1
            1 weight "obj.rune_2h_sword_gold" count 1
            1 weight "obj.rune_spear_gold" count 1
            1 weight "obj.brut_rune_spear_gold" count 1
        }

    private val hardThirdAge =
        rsPlayerWeightedTable(total = 13) {
            name("Hard clue 3rd age")
            1 weight "obj.trail_fighter_helm" count 1
            1 weight "obj.trail_silver_plate_chest" count 1
            1 weight "obj.trail_silver_plate_legs" count 1
            1 weight "obj.trail_silver_plate_skirt" count 1
            1 weight "obj.trail_fighter_shield" count 1
            1 weight "obj.trail_ranger_coif" count 1
            1 weight "obj.trail_ranger_torso" count 1
            1 weight "obj.trail_ranger_legs" count 1
            1 weight "obj.trail_ranger_vambraces" count 1
            1 weight "obj.trail_mage_hat" count 1
            1 weight "obj.trail_mage_torso" count 1
            1 weight "obj.trail_mage_legs" count 1
            1 weight "obj.trail_mage_amulet" count 1
        }

    private val hardPlatebodyH =
        rsPlayerWeightedTable(total = 5) {
            name("Hard clue heraldic rune platebodies")
            1 weight "obj.rune_platebody_h1" count 1
            1 weight "obj.rune_platebody_h2" count 1
            1 weight "obj.rune_platebody_h3" count 1
            1 weight "obj.rune_platebody_h4" count 1
            1 weight "obj.rune_platebody_h5" count 1
        }

    private val hardDhideShields =
        rsPlayerWeightedTable(total = 6) {
            name("Hard clue god d'hide shields")
            1 weight "obj.blessed_dhide_shield_saradomin" count 1
            1 weight "obj.blessed_dhide_shield_guthix" count 1
            1 weight "obj.blessed_dhide_shield_zamorak" count 1
            1 weight "obj.blessed_dhide_shield_bandos" count 1
            1 weight "obj.blessed_dhide_shield_armadyl" count 1
            1 weight "obj.blessed_dhide_shield_ancient" count 1
        }

    private val hardMega =
        rsPlayerWeightedTable(total = 10) {
            name("Hard clue mega-rare")
            1 weight "obj.cert_4dose2energy" count 15
            1 weight "obj.cert_4dose2restore" count 15
            1 weight "obj.cert_4dose1antidragon" count 15
            1 weight "obj.cert_4dose2attack" count 5
            5 weight hardGilded
            1 weight hardThirdAge
        }

    private val hardUniques =
        rsPlayerWeightedTable(total = 125) {
            name("Hard clue uniques")
            1 weight "obj.trail_amulet_of_glory_4" count 1
            1 weight "obj.robinhoodhat" count 1
            1 weight "obj.enchanted_hat" count 1
            1 weight "obj.enchanted_robetop" count 1
            1 weight "obj.enchanted_robelegs" count 1
            1 weight "obj.dragon_boots_kit" count 1
            1 weight "obj.rune_full_helm_trim" count 1
            1 weight "obj.rune_platebody_trim" count 1
            1 weight "obj.rune_platelegs_trim" count 1
            1 weight "obj.rune_plateskirt_trim" count 1
            1 weight "obj.rune_kiteshield_trim" count 1
            1 weight "obj.rune_full_helm_gold" count 1
            1 weight "obj.rune_platebody_gold" count 1
            1 weight "obj.rune_platelegs_gold" count 1
            1 weight "obj.rune_plateskirt_gold" count 1
            1 weight "obj.rune_kiteshield_gold" count 1
            1 weight "obj.rune_full_helm_zamorak" count 1
            1 weight "obj.rune_platebody_zamorak" count 1
            1 weight "obj.rune_platelegs_zamorak" count 1
            1 weight "obj.rune_plateskirt_zamorak" count 1
            1 weight "obj.rune_kiteshield_zamorak" count 1
            1 weight "obj.rune_full_helm_guthix" count 1
            1 weight "obj.rune_platebody_guthix" count 1
            1 weight "obj.rune_platelegs_guthix" count 1
            1 weight "obj.rune_plateskirt_guthix" count 1
            1 weight "obj.rune_kiteshield_guthix" count 1
            1 weight "obj.rune_full_helm_saradomin" count 1
            1 weight "obj.rune_platebody_saradomin" count 1
            1 weight "obj.rune_platelegs_saradomin" count 1
            1 weight "obj.rune_plateskirt_saradomin" count 1
            1 weight "obj.rune_kiteshield_saradomin" count 1
            1 weight "obj.rune_full_helm_ancient" count 1
            1 weight "obj.rune_platebody_ancient" count 1
            1 weight "obj.rune_platelegs_ancient" count 1
            1 weight "obj.rune_plateskirt_ancient" count 1
            1 weight "obj.rune_kiteshield_ancient" count 1
            1 weight "obj.rune_full_helm_armadyl" count 1
            1 weight "obj.rune_platebody_armadyl" count 1
            1 weight "obj.rune_platelegs_armadyl" count 1
            1 weight "obj.rune_plateskirt_armadyl" count 1
            1 weight "obj.rune_kiteshield_armadyl" count 1
            1 weight "obj.rune_full_helm_bandos" count 1
            1 weight "obj.rune_platebody_bandos" count 1
            1 weight "obj.rune_platelegs_bandos" count 1
            1 weight "obj.rune_plateskirt_bandos" count 1
            1 weight "obj.rune_kiteshield_bandos" count 1
            1 weight "obj.red_dragonhide_body_gold" count 1
            1 weight "obj.red_dragonhide_body_trim" count 1
            1 weight "obj.red_dragonhide_chaps_gold" count 1
            1 weight "obj.red_dragonhide_chaps_trim" count 1
            1 weight "obj.blue_dragonhide_body_trim_gold" count 1
            1 weight "obj.blue_dragonhide_body_trim" count 1
            1 weight "obj.blue_dragonhide_chaps_trim_gold" count 1
            1 weight "obj.blue_dragonhide_chaps_trim" count 1
            1 weight "obj.trail_saradomin_coif" count 1
            1 weight "obj.trail_saradomin_chest" count 1
            1 weight "obj.trail_saradomin_chaps" count 1
            1 weight "obj.trail_saradomin_vambraces" count 1
            1 weight "obj.blessed_boots_saradomin" count 1
            1 weight "obj.trail_guthix_coif" count 1
            1 weight "obj.trail_guthix_chest" count 1
            1 weight "obj.trail_guthix_chaps" count 1
            1 weight "obj.trail_guthix_vambraces" count 1
            1 weight "obj.blessed_boots_guthix" count 1
            1 weight "obj.trail_zamorak_coif" count 1
            1 weight "obj.trail_zamorak_chest" count 1
            1 weight "obj.trail_zamorak_chaps" count 1
            1 weight "obj.trail_zamorak_vambraces" count 1
            1 weight "obj.blessed_boots_zamorak" count 1
            1 weight "obj.trail_bandos_coif" count 1
            1 weight "obj.trail_bandos_chest" count 1
            1 weight "obj.trail_bandos_chaps" count 1
            1 weight "obj.trail_bandos_vambraces" count 1
            1 weight "obj.blessed_boots_bandos" count 1
            1 weight "obj.trail_armadyl_coif" count 1
            1 weight "obj.trail_armadyl_chest" count 1
            1 weight "obj.trail_armadyl_chaps" count 1
            1 weight "obj.trail_armadyl_vambraces" count 1
            1 weight "obj.blessed_boots_armadyl" count 1
            1 weight "obj.trail_ancient_coif" count 1
            1 weight "obj.trail_ancient_chest" count 1
            1 weight "obj.trail_ancient_chaps" count 1
            1 weight "obj.trail_ancient_vambraces" count 1
            1 weight "obj.blessed_boots_ancient" count 1
            1 weight "obj.trail_saradomin_scarf" count 1
            1 weight "obj.trail_saradomin_staff" count 1
            1 weight "obj.trail_guthix_scarf" count 1
            1 weight "obj.trail_guthix_staff" count 1
            1 weight "obj.trail_zamorak_scarf" count 1
            1 weight "obj.trail_zamorak_staff" count 1
            1 weight "obj.zombie_head" count 1
            1 weight "obj.cyclops_mask" count 1
            1 weight "obj.piratehat" count 1
            1 weight "obj.cavalier_red" count 1
            1 weight "obj.cavalier_white" count 1
            1 weight "obj.cavalier_navy" count 1
            1 weight "obj.cavalier_brown" count 1
            1 weight "obj.cavalier_dark" count 1
            1 weight "obj.cavalier_black" count 1
            1 weight "obj.pith_helmet" count 1
            1 weight "obj.explorer_backpack" count 1
            1 weight "obj.dragonmask_green" count 1
            1 weight "obj.dragonmask_blue" count 1
            1 weight "obj.dragonmask_red" count 1
            1 weight "obj.dragonmask_black" count 1
            1 weight "obj.nunchucks" count 1
            1 weight "obj.rune_cane" count 1
            1 weight "obj.dual_sai" count 1
            1 weight "obj.thieving_bag" count 1
            1 weight "obj.rd_ornament_kit" count 1
            1 weight "obj.beserker_necklace_ornament_kit" count 1
            1 weight "obj.tzhaar_maul_ornament_kit" count 1
            1 weight "obj.rune_heraldic_kiteshield1" count 1
            1 weight "obj.rune_heraldic_kiteshield2" count 1
            1 weight "obj.rune_heraldic_kiteshield3" count 1
            1 weight "obj.rune_heraldic_kiteshield4" count 1
            1 weight "obj.rune_heraldic_kiteshield5" count 1
            1 weight "obj.trail_heraldic_helm_1_rune" count 1
            1 weight "obj.trail_heraldic_helm_2_rune" count 1
            1 weight "obj.trail_heraldic_helm_3_rune" count 1
            1 weight "obj.trail_heraldic_helm_4_rune" count 1
            1 weight "obj.trail_heraldic_helm_5_rune" count 1
            1 weight hardPlatebodyH
            1 weight hardDhideShields
            1 weight hardMega
        }
    private val hardMagicBow =
        rsPlayerWeightedTable(total = 10) {
            name("Hard clue magic bow")
            9 weight "obj.magic_longbow" count 1
            1 weight "obj.trail_composite_bow_magic" count 1
        }

    private val hardShared =
        rsPlayerWeightedTable(total = 20) {
            name("Hard clue shared rewards")
            7 weight "obj.coins" count 10000..15000
            7 weight "obj.trail_sweets" count 8..12
            6 weight CasketTables.blessings
        }

    private val hardFirelighters =
        rsPlayerWeightedTable(total = 5) {
            name("Hard clue firelighters")
            1 weight "obj.gnomish_firelighter_red" count 9..15
            1 weight "obj.gnomish_firelighter_green" count 9..15
            1 weight "obj.gnomish_firelighter_blue" count 9..15
            1 weight "obj.trail_gnomish_firelighter_purple" count 9..15
            1 weight "obj.trail_gnomish_firelighter_white" count 9..15
        }

    private val hardStandard =
        rsPlayerWeightedTable(total = 25) {
            name("Hard clue standard rewards")
            1 weight "obj.rune_full_helm" count 1
            1 weight "obj.rune_platebody" count 1
            1 weight "obj.rune_platelegs" count 1
            1 weight "obj.rune_plateskirt" count 1
            1 weight "obj.rune_kiteshield" count 1
            1 weight "obj.rune_longsword" count 1
            1 weight "obj.rune_dagger" count 1
            1 weight "obj.rune_battleaxe" count 1
            1 weight "obj.rune_axe" count 1
            1 weight "obj.rune_pickaxe" count 1
            1 weight "obj.black_dragonhide_body" count 1
            1 weight "obj.black_dragonhide_chaps" count 1
            1 weight "obj.magic_shortbow" count 1
            1 weight hardMagicBow
            1 weight "obj.naturerune" count 30..50
            1 weight "obj.lawrune" count 30..50
            1 weight "obj.bloodrune" count 20..30
            1 weight "obj.cert_lobster" count 12..15
            1 weight "obj.cert_shark" count 12..15
            1 weight "obj.coins" count 1000..5000
            1 weight "obj.trail_sweets" count 7..15
            1 weight hardShared
            1 weight hardFirelighters
            1 weight CasketTables.scrollsOrBook
            1 weight CasketTables.prayerPages
        }

    private val hardTable =
        rsPlayerWeightedTable(total = 13) {
            name("Reward casket (hard)")
            12 weight hardStandard
            1 weight hardUniques
        }

    private val eliteGilded =
        rsPlayerWeightedTable(total = 11) {
            name("Elite clue gilded armour")
            1 weight "obj.rune_full_helm_goldplate" count 1
            1 weight "obj.rune_platebody_goldplate" count 1
            1 weight "obj.rune_platelegs_goldplate" count 1
            1 weight "obj.rune_plateskirt_goldplate" count 1
            1 weight "obj.rune_kiteshield_goldplate" count 1
            1 weight "obj.rune_med_helm_gold" count 1
            1 weight "obj.rune_chainbody_gold" count 1
            1 weight "obj.rune_sq_shield_gold" count 1
            1 weight "obj.rune_2h_sword_gold" count 1
            1 weight "obj.rune_spear_gold" count 1
            1 weight "obj.brut_rune_spear_gold" count 1
        }

    private val eliteThirdAge =
        rsPlayerWeightedTable(total = 17) {
            name("Elite clue 3rd age")
            1 weight "obj.trail_fighter_helm" count 1
            1 weight "obj.trail_silver_plate_chest" count 1
            1 weight "obj.trail_silver_plate_legs" count 1
            1 weight "obj.trail_silver_plate_skirt" count 1
            1 weight "obj.trail_fighter_shield" count 1
            1 weight "obj.trail_ranger_coif" count 1
            1 weight "obj.trail_ranger_torso" count 1
            1 weight "obj.trail_ranger_legs" count 1
            1 weight "obj.trail_ranger_vambraces" count 1
            1 weight "obj.trail_mage_hat" count 1
            1 weight "obj.trail_mage_torso" count 1
            1 weight "obj.trail_mage_legs" count 1
            1 weight "obj.trail_mage_amulet" count 1
            1 weight "obj.trail_fighter_sword" count 1
            1 weight "obj.trail_mage_wand" count 1
            1 weight "obj.trail_third_cape" count 1
            1 weight "obj.trail_ranger_bow" count 1
        }

    private val eliteTuxedoDark =
        rsPlayerWeightedTable(total = 5) {
            name("Elite clue dark tuxedo")
            1 weight "obj.tuxedo_bowtie" count 1
            1 weight "obj.tuxedo_body" count 1
            1 weight "obj.tuxedo_hands" count 1
            1 weight "obj.tuxedo_legs" count 1
            1 weight "obj.tuxedo_feet" count 1
        }

    private val eliteTuxedoLight =
        rsPlayerWeightedTable(total = 5) {
            name("Elite clue light tuxedo")
            1 weight "obj.tuxedo_bowtie_white" count 1
            1 weight "obj.tuxedo_body_white" count 1
            1 weight "obj.tuxedo_hands_white" count 1
            1 weight "obj.tuxedo_legs_white" count 1
            1 weight "obj.tuxedo_feet_white" count 1
        }

    private val eliteMega =
        rsPlayerWeightedTable(total = 23) {
            name("Elite clue mega-rare")
            1 weight "obj.ring_of_nature" count 1
            1 weight "obj.crystal_key" count 1
            1 weight "obj.dragonmask_lava" count 1
            1 weight "obj.cert_battlestaff" count 100
            1 weight "obj.cert_4dose2antidragon" count 30
            1 weight "obj.cert_4dose2restore" count 30
            1 weight "obj.cert_4dosepotionofsaradomin" count 30
            1 weight "obj.cert_4doserangerspotion" count 30
            5 weight eliteGilded
            1 weight "obj.rune_scimitar_gold" count 1
            1 weight "obj.rune_boots_gold" count 1
            1 weight "obj.trail_gilded_dhide_coif" count 1
            1 weight "obj.trail_gilded_dhide_vambraces" count 1
            1 weight "obj.trail_gilded_dhide_top" count 1
            1 weight "obj.trail_gilded_dhide_chaps" count 1
            1 weight "obj.trail_gilded_pickaxe" count 1
            1 weight "obj.trail_gilded_axe" count 1
            1 weight "obj.trail_gilded_spade" count 1
            1 weight eliteThirdAge
        }

    private val eliteUniques =
        rsPlayerWeightedTable(total = 52) {
            name("Elite clue uniques")
            1 weight "obj.dragon_full_helm_kit" count 1
            1 weight "obj.dragon_chainbody_kit" count 1
            1 weight "obj.dragon_platelegs_kit" count 1
            1 weight "obj.dragon_sq_shield_kit" count 1
            1 weight "obj.dragon_scimitar_ornament_kit" count 1
            1 weight "obj.fury_kit" count 1
            1 weight "obj.light_inf_kit" count 1
            1 weight "obj.dark_inf_kit" count 1
            1 weight "obj.royal_crown" count 1
            1 weight "obj.royal_top" count 1
            1 weight "obj.royal_bottom" count 1
            1 weight "obj.royal_sceptre" count 1
            1 weight "obj.musketeer_hat" count 1
            1 weight "obj.musketeer_top" count 1
            1 weight "obj.musketeer_legs" count 1
            1 weight "obj.black_dragonhide_body_gold" count 1
            1 weight "obj.black_dragonhide_body_trim" count 1
            1 weight "obj.black_dragonhide_chaps_gold" count 1
            1 weight "obj.black_dragonhide_chaps_trim" count 1
            1 weight "obj.ranger_tunic" count 1
            1 weight "obj.ranger_gloves" count 1
            1 weight "obj.holy_wraps" count 1
            1 weight "obj.dragonmask_bronze" count 1
            1 weight "obj.dragonmask_iron" count 1
            1 weight "obj.dragonmask_steel" count 1
            1 weight "obj.dragonmask_mith" count 1
            1 weight "obj.dragonmask_adamant" count 1
            1 weight "obj.dragonmask_rune" count 1
            1 weight "obj.zeah_scarf_arceuus" count 1
            1 weight "obj.zeah_scarf_hosidius" count 1
            1 weight "obj.zeah_scarf_lovakengj" count 1
            1 weight "obj.zeah_scarf_piscarilius" count 1
            1 weight "obj.zeah_scarf_shayzien" count 1
            1 weight "obj.katana" count 1
            1 weight "obj.dragon_cane" count 1
            1 weight "obj.bucket_helm" count 1
            1 weight "obj.blacksmith_helm" count 1
            1 weight "obj.deerstalker" count 1
            1 weight "obj.afro" count 1
            1 weight "obj.trail_pirate_hat" count 1
            1 weight "obj.top_hat" count 1
            1 weight "obj.monacle" count 1
            1 weight "obj.trail_briefcase" count 1
            1 weight "obj.wise_spectacles" count 1
            1 weight "obj.rangers_tights" count 1
            1 weight "obj.uris_hat" count 1
            1 weight "obj.giant_boot" count 1
            1 weight "obj.fremennik_kilt" count 1
            1 weight eliteTuxedoDark
            1 weight eliteTuxedoLight
            2 weight eliteMega
        }
    private val eliteKeyHalves =
        rsPlayerWeightedTable(total = 2) {
            name("Elite clue key halves")
            1 weight "obj.keyhalf1" count 1
            1 weight "obj.keyhalf2" count 1
        }

    private val eliteSeeds =
        rsPlayerWeightedTable(total = 3) {
            name("Elite clue seeds")
            1 weight "obj.palm_tree_seed" count 1
            1 weight "obj.yew_seed" count 1
            1 weight "obj.magic_tree_seed" count 1
        }

    private val eliteShared =
        rsPlayerWeightedTable(total = 20) {
            name("Elite clue shared rewards")
            7 weight "obj.coins" count 10000..15000
            7 weight "obj.trail_sweets" count 8..12
            6 weight CasketTables.blessings
        }

    private val eliteFirelighters =
        rsPlayerWeightedTable(total = 5) {
            name("Elite clue firelighters")
            1 weight "obj.gnomish_firelighter_red" count 9..15
            1 weight "obj.gnomish_firelighter_green" count 9..15
            1 weight "obj.gnomish_firelighter_blue" count 9..15
            1 weight "obj.trail_gnomish_firelighter_purple" count 9..15
            1 weight "obj.trail_gnomish_firelighter_white" count 9..15
        }

    private val eliteStandard =
        rsPlayerWeightedTable(total = 31) {
            name("Elite clue standard rewards")
            1 weight "obj.rune_platebody" count 1
            1 weight "obj.rune_platelegs" count 1
            1 weight "obj.rune_plateskirt" count 1
            1 weight "obj.rune_kiteshield" count 1
            1 weight "obj.xbows_crossbow_runite" count 1
            1 weight "obj.dragon_dagger" count 1
            1 weight "obj.dragon_mace" count 1
            1 weight "obj.dragon_longsword" count 1
            1 weight "obj.xbows_bolt_tips_onyx" count 8..12
            1 weight "obj.lawrune" count 50..75
            1 weight "obj.deathrune" count 50..75
            1 weight "obj.bloodrune" count 50..75
            1 weight "obj.soulrune" count 50..75
            1 weight "obj.jewl_dragonstone_bracelet" count 1
            1 weight "obj.dragonstone_necklace" count 1
            1 weight "obj.dragonstone_ring" count 1
            1 weight "obj.cert_potato_tuna+sweetcorn" count 15..20
            1 weight "obj.cert_summer_pie" count 15..20
            1 weight "obj.cert_plank_oak" count 60..80
            1 weight "obj.cert_plank_teak" count 40..50
            1 weight "obj.cert_plank_mahogany" count 20..30
            1 weight "obj.cert_runite_bar" count 1..3
            1 weight eliteKeyHalves
            1 weight eliteSeeds
            1 weight "obj.coins" count 20000..30000
            1 weight "obj.trail_sweets" count 9..23
            1 weight eliteShared
            1 weight eliteFirelighters
            2 weight CasketTables.scrollsOrBook
            1 weight CasketTables.prayerPages
        }

    private val eliteTable =
        rsPlayerWeightedTable(total = 25) {
            name("Reward casket (elite)")
            24 weight eliteStandard
            1 weight eliteUniques
        }
}
