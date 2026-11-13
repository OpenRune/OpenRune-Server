package org.rsmod.content.other.treasuretrails

import dtx.rs.RSWeightedTable
import org.rsmod.api.droptable.DropRollItem
import org.rsmod.api.droptable.rsPlayerWeightedTable
import org.rsmod.game.entity.Player

internal object MasterCasketTables {
    val master: RSWeightedTable<Player, DropRollItem>
        get() = masterTable

    private val godswordKits =
        rsPlayerWeightedTable(total = 4) {
            name("Master clue godsword ornament kits")
            1 weight "obj.ags_ornament_kit" count 1
            1 weight "obj.bgs_ornament_kit" count 1
            1 weight "obj.sgs_ornament_kit" count 1
            1 weight "obj.zgs_ornament_kit" count 1
        }

    private val gildedArmour =
        rsPlayerWeightedTable(total = 11) {
            name("Master clue gilded armour")
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

    private val gilded =
        rsPlayerWeightedTable(total = 10) {
            name("Master clue gilded")
            1 weight gildedArmour
            1 weight "obj.rune_scimitar_gold" count 1
            1 weight "obj.rune_boots_gold" count 1
            1 weight "obj.trail_gilded_dhide_coif" count 1
            1 weight "obj.trail_gilded_dhide_vambraces" count 1
            1 weight "obj.trail_gilded_dhide_top" count 1
            1 weight "obj.trail_gilded_dhide_chaps" count 1
            1 weight "obj.trail_gilded_pickaxe" count 1
            1 weight "obj.trail_gilded_axe" count 1
            1 weight "obj.trail_gilded_spade" count 1
        }

    private val thirdAge =
        rsPlayerWeightedTable(total = 23) {
            name("Master clue 3rd age")
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
            1 weight "obj.3a_druidic_top" count 1
            1 weight "obj.3a_druidic_bottoms" count 1
            1 weight "obj.3a_druidic_cloak" count 1
            1 weight "obj.trail_fighter_sword" count 1
            1 weight "obj.trail_ranger_bow" count 1
            1 weight "obj.trail_mage_wand" count 1
            1 weight "obj.3a_druidic_staff" count 1
            1 weight "obj.trail_third_cape" count 1
            1 weight "obj.3a_pickaxe" count 1
            1 weight "obj.3a_axe" count 1
        }

    private val mega =
        rsPlayerWeightedTable(total = 16) {
            name("Master clue mega-rare")
            1 weight "obj.bucket_helm_gold" count 1
            1 weight "obj.ring_of_coins" count 1
            1 weight "obj.cert_cabbage" count 3
            1 weight "obj.cert_antivenom+4" count 15
            1 weight "obj.cert_torstol" count 50
            10 weight gilded
            1 weight thirdAge
        }

    /**
     * Weights are in 1/30ths of the 1/851 rarity of a single unique, so the Ankou, mummy and
     * ornament-kit rows (1/12,765 to 1/25,530) keep their wiki ratios.
     */
    private val uniques =
        rsPlayerWeightedTable(total = 1013) {
            name("Master clue uniques")
            30 weight "obj.occult_kit" count 1
            30 weight "obj.torture_kit" count 1
            30 weight "obj.anguish_kit" count 1
            30 weight "obj.tormented_kit" count 1
            30 weight "obj.dd_ornament_kit" count 1
            1 weight "obj.dragon_kiteshield_kit" count 1
            2 weight "obj.dragon_platebody_kit" count 1
            30 weight "obj.robe_darkness_head" count 1
            30 weight "obj.robe_darkness_top" count 1
            30 weight "obj.robe_darkness_hands" count 1
            30 weight "obj.robe_darkness_legs" count 1
            30 weight "obj.robe_darkness_feet" count 1
            30 weight "obj.samurai_hat" count 1
            30 weight "obj.samurai_top" count 1
            30 weight "obj.samurai_gloves" count 1
            30 weight "obj.samurai_legs" count 1
            30 weight "obj.samurai_boots" count 1
            30 weight "obj.zeah_hood_arceuus" count 1
            30 weight "obj.zeah_hood_hosidius" count 1
            30 weight "obj.zeah_hood_lovakengj" count 1
            30 weight "obj.zeah_hood_piscarilius" count 1
            30 weight "obj.zeah_hood_shayzien" count 1
            30 weight "obj.old_demon_mask" count 1
            30 weight "obj.lesser_demon_mask" count 1
            30 weight "obj.greater_demon_mask" count 1
            30 weight "obj.black_demon_mask" count 1
            30 weight "obj.jungle_demon_mask" count 1
            30 weight "obj.eye_patch_02" count 1
            30 weight "obj.the_bowl" count 1
            30 weight "obj.ale_gods" count 1
            30 weight "obj.tzhaar_cape_obsidian_r" count 1
            30 weight "obj.fancy_tiara" count 1
            30 weight "obj.half_moon_spectacles" count 1
            30 weight godswordKits
            2 weight "obj.ankou_head" count 1
            2 weight "obj.ankou_body" count 1
            2 weight "obj.ankou_hands" count 1
            2 weight "obj.ankou_legs" count 1
            2 weight "obj.ankou_feet" count 1
            2 weight "obj.mummy_head" count 1
            2 weight "obj.mummy_body" count 1
            2 weight "obj.mummy_hands" count 1
            2 weight "obj.mummy_legs" count 1
            2 weight "obj.mummy_feet" count 1
            30 weight mega
        }

    private val keyHalves =
        rsPlayerWeightedTable(total = 2) {
            name("Master clue key halves")
            1 weight "obj.keyhalf1" count 1
            1 weight "obj.keyhalf2" count 1
        }

    private val seeds =
        rsPlayerWeightedTable(total = 3) {
            name("Master clue seeds")
            1 weight "obj.palm_tree_seed" count 1..2
            1 weight "obj.yew_seed" count 1..2
            1 weight "obj.magic_tree_seed" count 1..2
        }

    private val shared =
        rsPlayerWeightedTable(total = 20) {
            name("Master clue shared rewards")
            7 weight "obj.coins" count 10000..15000
            7 weight "obj.trail_sweets" count 8..12
            6 weight CasketTables.blessings
        }

    private val firelighters =
        rsPlayerWeightedTable(total = 5) {
            name("Master clue firelighters")
            1 weight "obj.gnomish_firelighter_red" count 20..38
            1 weight "obj.gnomish_firelighter_green" count 20..38
            1 weight "obj.gnomish_firelighter_blue" count 20..38
            1 weight "obj.trail_gnomish_firelighter_purple" count 20..38
            1 weight "obj.trail_gnomish_firelighter_white" count 20..38
        }

    private val standard =
        rsPlayerWeightedTable(total = 29) {
            name("Master clue standard rewards")
            1 weight "obj.dragon_dagger" count 1
            1 weight "obj.dragon_mace" count 1
            1 weight "obj.dragon_longsword" count 1
            1 weight "obj.dragon_scimitar" count 1
            1 weight "obj.dragon_battleaxe" count 1
            1 weight "obj.dragon_halberd" count 1
            1 weight "obj.naturerune" count 100..200
            1 weight "obj.deathrune" count 100..200
            1 weight "obj.bloodrune" count 100..200
            1 weight "obj.soulrune" count 100..200
            1 weight "obj.xbows_crossbow_bolts_runite_tipped_onyx_enchanted" count 15..25
            1 weight "obj.cert_mantaray" count 15..25
            1 weight "obj.cert_wine_of_zamorak" count 35..50
            1 weight "obj.cert_limpwurt_root" count 40..60
            1 weight "obj.cert_unidentified_ranarr" count 5..10
            1 weight "obj.cert_unidentified_toadflax" count 25..35
            1 weight "obj.cert_unidentified_snapdragon" count 5..10
            1 weight "obj.cert_runite_ore" count 5..8
            1 weight "obj.cert_runite_bar" count 5..7
            1 weight "obj.cert_dragonhide_black" count 5..25
            1 weight "obj.coins" count 20000..35000
            1 weight "obj.trail_sweets" count 14..33
            1 weight keyHalves
            1 weight seeds
            1 weight shared
            1 weight firelighters
            2 weight CasketTables.scrollsOrBook
            1 weight CasketTables.prayerPages
        }

    private val masterTable =
        rsPlayerWeightedTable(total = 25530) {
            name("Reward casket (master)")
            24517 weight standard
            1013 weight uniques
        }
}
