package org.rsmod.content.other.treasuretrails

import dtx.rs.RSWeightedTable
import org.rsmod.api.droptable.DropRollItem
import org.rsmod.api.droptable.rsPlayerWeightedTable
import org.rsmod.game.entity.Player

/** One roll of each tier's reward casket; a casket rolls `reward_rolls_min..max` times. */
object CasketTables {
    fun forDifficulty(difficulty: Int): RSWeightedTable<Player, DropRollItem>? =
        when (difficulty) {
            0 -> beginner
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
}
