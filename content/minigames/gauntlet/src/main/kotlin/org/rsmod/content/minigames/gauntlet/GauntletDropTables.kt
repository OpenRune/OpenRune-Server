package org.rsmod.content.minigames.gauntlet

import dtx.rs.RSDropTable
import dtx.rs.locs
import org.rsmod.api.droptable.DropRollItem
import org.rsmod.api.droptable.RegisterDropTable
import org.rsmod.api.droptable.rsPlayerGuaranteedTable
import org.rsmod.api.droptable.rsPlayerTertiaryTable
import org.rsmod.api.droptable.rsPlayerWeightedTable
import org.rsmod.content.drops.clueScrollTransformObj
import org.rsmod.game.entity.Player

internal object GauntletRewardKeys {
    const val NORMAL = "loc.gauntlet_chest"
    const val CORRUPTED = "loc.gauntlet_chest_hm"
    const val INCOMPLETE = "gauntlet_reward_incomplete"
    const val JUNK = "gauntlet_reward_junk"
    const val NORMAL_ROLLS = 2
    const val CORRUPTED_ROLLS = 3
}

private const val SHARD = "obj.prif_crystal_shard"
private const val CAPE = "obj.gauntlet_crystalline_cape"
private const val ELITE_CLUE = "obj.trail_elite_emote_exp1"

@field:RegisterDropTable
@JvmField
val gauntletNormalRewardTable: RSDropTable<Player, DropRollItem> =
    RSDropTable(
        tableIdentifier = "Gauntlet Reward Chest",
        locs = locs(GauntletRewardKeys.NORMAL),
        guaranteed = rsPlayerGuaranteedTable { SHARD count 5..9 },
        mainTable =
            rsPlayerWeightedTable(total = 24) {
                name("Gauntlet Reward Chest")
                1 weight "obj.cert_battlestaff" count 4..8
                1 weight "obj.cert_rune_full_helm" count 2..4
                1 weight "obj.cert_rune_chainbody" count 1..2
                1 weight "obj.cert_rune_platebody" count 1..2
                1 weight "obj.cert_rune_platelegs" count 1..2
                1 weight "obj.cert_rune_plateskirt" count 1..2
                1 weight "obj.cert_rune_halberd" count 1..2
                1 weight "obj.cert_rune_pickaxe" count 1..2
                1 weight "obj.cert_dragon_halberd" count 1
                1 weight "obj.cosmicrune" count 160..240
                1 weight "obj.naturerune" count 100..140
                1 weight "obj.lawrune" count 80..140
                1 weight "obj.chaosrune" count 180..300
                1 weight "obj.deathrune" count 100..160
                1 weight "obj.bloodrune" count 80..140
                1 weight "obj.mithril_arrow" count 800..1200
                1 weight "obj.adamant_arrow" count 400..600
                1 weight "obj.rune_arrow" count 200..300
                1 weight "obj.dragon_arrow" count 30..85
                1 weight "obj.cert_uncut_sapphire" count 20..60
                1 weight "obj.cert_uncut_emerald" count 10..50
                1 weight "obj.cert_uncut_ruby" count 5..30
                1 weight "obj.cert_uncut_diamond" count 3..7
                1 weight "obj.coins" count 20_000..80_000
            },
        mainRolls = GauntletRewardKeys.NORMAL_ROLLS,
        tertiaries =
            rsPlayerTertiaryTable {
                1 outOf 25 weight ELITE_CLUE count 1 transformObj { player ->
                    player.clueScrollTransformObj(ELITE_CLUE)
                }
                1 outOf 120 weight "obj.crystal_seed_old" count 1
                1 outOf 120 weight "obj.prif_armour_seed" count 1
                boosted {
                    1 outOf 2000 weight "obj.prif_weapon_seed_enhanced" count 1
                }
                boosted {
                    1 outOf 2000 weight "obj.gauntletpet" count 1
                }
            },
    )

@field:RegisterDropTable
@JvmField
val gauntletCorruptedRewardTable: RSDropTable<Player, DropRollItem> =
    RSDropTable(
        tableIdentifier = "Corrupted Gauntlet Reward Chest",
        locs = locs(GauntletRewardKeys.CORRUPTED),
        guaranteed =
            rsPlayerGuaranteedTable {
                SHARD count 7..12
                CAPE count 1 condition { player -> !player.ownsGauntletCape() }
            },
        mainTable =
            rsPlayerWeightedTable(total = 48) {
                name("Corrupted Gauntlet Reward Chest")
                2 weight "obj.cert_battlestaff" count 8..12
                2 weight "obj.cert_rune_full_helm" count 3..5
                2 weight "obj.cert_rune_chainbody" count 2..3
                2 weight "obj.cert_rune_platebody" count 2
                2 weight "obj.cert_rune_platelegs" count 2..3
                2 weight "obj.cert_rune_plateskirt" count 2..3
                2 weight "obj.cert_rune_halberd" count 2..3
                2 weight "obj.cert_rune_pickaxe" count 2..3
                1 weight "obj.cert_dragon_halberd" count 1..2
                2 weight "obj.cosmicrune" count 175..250
                2 weight "obj.naturerune" count 125..150
                2 weight "obj.lawrune" count 100..150
                2 weight "obj.chaosrune" count 200..350
                2 weight "obj.deathrune" count 125..175
                2 weight "obj.bloodrune" count 100..150
                2 weight "obj.mithril_arrow" count 1000..1500
                2 weight "obj.adamant_arrow" count 500..750
                2 weight "obj.rune_arrow" count 250..450
                2 weight "obj.dragon_arrow" count 50..100
                2 weight "obj.cert_uncut_sapphire" count 25..65
                2 weight "obj.cert_uncut_emerald" count 15..60
                2 weight "obj.cert_uncut_ruby" count 10..40
                2 weight "obj.cert_uncut_diamond" count 5..15
                3 weight "obj.coins" count 75_000..150_000
            },
        mainRolls = GauntletRewardKeys.CORRUPTED_ROLLS,
        tertiaries =
            rsPlayerTertiaryTable {
                1 outOf 20 weight ELITE_CLUE count 1 transformObj { player ->
                    player.clueScrollTransformObj(ELITE_CLUE)
                }
                1 outOf 50 weight "obj.crystal_seed_old" count 1
                1 outOf 50 weight "obj.prif_armour_seed" count 1
                boosted {
                    1 outOf 400 weight "obj.prif_weapon_seed_enhanced" count 1
                }
                boosted {
                    1 outOf 800 weight "obj.gauntletpet_corrupt" count 1
                }
            },
    )

@field:RegisterDropTable
@JvmField
val gauntletIncompleteRewardTable: RSDropTable<Player, DropRollItem> =
    RSDropTable(
        tableIdentifier = "Gauntlet Incomplete Reward",
        locs = locs(GauntletRewardKeys.INCOMPLETE),
        mainTable =
            rsPlayerWeightedTable(total = 27) {
                name("Gauntlet Incomplete Reward")
                1 weight "obj.adamant_dagger" count 1
                1 weight "obj.adamant_full_helm" count 1
                1 weight "obj.cert_adamant_mace" count 2..3
                1 weight "obj.adamant_pickaxe" count 1
                1 weight "obj.adamant_platebody" count 1
                1 weight "obj.adamant_platelegs" count 1
                1 weight "obj.adamant_plateskirt" count 1
                1 weight "obj.adamant_scimitar" count 1
                1 weight "obj.cert_maple_longbow" count 7..13
                1 weight "obj.cert_maple_shortbow" count 8..11
                1 weight "obj.mithril_full_helm" count 1
                1 weight "obj.cert_mithril_mace" count 2..5
                1 weight "obj.mithril_platebody" count 1
                1 weight "obj.mithril_platelegs" count 1
                1 weight "obj.mithril_plateskirt" count 1
                1 weight "obj.airrune" count 200..300
                1 weight "obj.bodyrune" count 250..350
                1 weight "obj.earthrune" count 200..300
                1 weight "obj.firerune" count 200..300
                1 weight "obj.mindrune" count 300..400
                1 weight "obj.waterrune" count 200..300
                1 weight "obj.cert_cake" count 10..20
                1 weight "obj.cert_cod" count 75..125
                1 weight "obj.cert_trout" count 50..100
                1 weight "obj.cert_eye_of_newt" count 300..500
                1 weight "obj.cert_silver_bar" count 15..30
                1 weight "obj.cert_uncut_sapphire" count 1..3
            },
    )

@field:RegisterDropTable
@JvmField
val gauntletJunkRewardTable: RSDropTable<Player, DropRollItem> =
    RSDropTable(
        tableIdentifier = "Gauntlet Junk Reward",
        locs = locs(GauntletRewardKeys.JUNK),
        mainTable =
            rsPlayerWeightedTable(total = 3) {
                name("Gauntlet Junk Reward")
                1 weight "obj.flier_prif" count 1
                1 weight "obj.ogre_potion" count 1
                1 weight "obj.rotten_tomato" count 1
            },
    )

private fun Player.ownsGauntletCape(): Boolean =
    CAPE in inv || CAPE in worn || CAPE in invMap.getOrPut("inv.bank")
