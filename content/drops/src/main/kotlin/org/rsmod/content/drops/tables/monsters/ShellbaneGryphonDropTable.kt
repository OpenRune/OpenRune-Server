package org.rsmod.content.drops.tables.monsters

import dtx.rs.RSDropTable
import dtx.rs.npcs
import org.rsmod.api.droptable.DropRollItem
import org.rsmod.api.droptable.RegisterDropTable
import org.rsmod.api.droptable.nothing
import org.rsmod.api.droptable.rsPlayerGuaranteedTable
import org.rsmod.api.droptable.rsPlayerPrerollTable
import org.rsmod.api.droptable.rsPlayerTertiaryTable
import org.rsmod.api.droptable.rsPlayerWeightedTable
import org.rsmod.content.drops.brimstoneKeyRoll
import org.rsmod.content.drops.clueScrollTransformObj
import org.rsmod.content.drops.tables.shared.SharedDropTables
import org.rsmod.game.entity.Player

@field:RegisterDropTable
@JvmField
public val shellbaneGryphonDropTable: RSDropTable<Player, DropRollItem> =
    RSDropTable(
        tableIdentifier = "Shellbane gryphon Drops",
        npcs = npcs("npc.gryphon_boss"),
        guaranteed = rsPlayerGuaranteedTable { "obj.gryphon_feather" count 7..10 },
        preRoll = rsPlayerPrerollTable { 1 outOf 256 weight "obj.belles_folly_tarnished" count 1 },
        mainTable =
            rsPlayerWeightedTable(total = 128) {
                name("Shellbane gryphon Drops")
                10 weight "obj.rune_claws" count 1
                8 weight "obj.rune_scimitar" count 1
                8 weight "obj.adamant_cannonball" count 35..50
                6 weight "obj.rune_cannonball" count 20..30
                14 weight "obj.raw_swordfish" count 1
                10 weight "obj.sweetcorn" count 1
                10 weight "obj.watermelon" count 1
                4 weight "obj.raw_seaturtle" count 1
                1 weight "obj.coral_elkhorn_frag" count 1
                1 weight "obj.coral_pillar_frag" count 1
                11 weight "obj.gryphon_feather" count 35..50
                10 weight "obj.shark_lure" count 3..5
                3 weight "obj.basket_empty" count 1

                6 weight SharedDropTables.herb
                1 weight SharedDropTables.gem
                25 weight nothing()
                10 outOf 171 separate "obj.potato_seed" count 1..4
                10 outOf 341 separate "obj.onion_seed" count 1..3
                10 outOf 683 separate "obj.cabbage_seed" count 1..3
                5 outOf 186 separate "obj.tomato_seed" count 1..2
                2 outOf 149 separate "obj.sweetcorn_seed" count 1..2
                10 outOf 1489 separate "obj.strawberry_seed" count 1
                10 outOf 2979 separate "obj.watermelon_seed" count 1
                10 outOf 2979 separate "obj.snape_grass_seed" count 1
                10 outOf 853 separate "obj.unidentified_guam" count 1
                5 outOf 569 separate "obj.unidentified_marentill" count 1
                10 outOf 1517 separate "obj.unidentified_tarromin" count 1
                1 outOf 195 separate "obj.unidentified_harralander" count 1
                5 outOf 1241 separate "obj.unidentified_ranarr" count 1
                10 outOf 3413 separate "obj.unidentified_irit" count 1
                10 outOf 4551 separate "obj.unidentified_avantoe" count 1
                5 outOf 356 separate "obj.unidentified_kwuarm" count 1
                1 outOf 89 separate "obj.unidentified_cadantine" count 1
                10 outOf 1187 separate "obj.unidentified_lantadyme" count 1
                1 outOf 92 separate "obj.unidentified_dwarf_weed" count 1
            },
        tertiaries =
            rsPlayerTertiaryTable {
                1 outOf
                    1 weight
                    "obj.trail_elite_emote_exp1" count
                    1 condition
                    { player ->
                        // Drops Need Manual: Guaranteed elite clue scroll and reward casket drops
                        // only occur when completing an elite clue scroll asking you to kill the
                        // Shellbane gryphon.
                        true
                    }
                1 outOf 1 weight "obj.trail_reward_casket_elite" count 1
                onBuilder { brimstoneKeyRoll(konarTaskBonus = true) }
                1 outOf 400 weight "obj.dorgesh_construction_bone" count 1
                1 outOf 2000 weight "obj.jar_of_feathers" count 1
                1 outOf 3000 weight "obj.gryphonbosspet" count 1
                1 outOf 5013 weight "obj.dorgesh_construction_bone_curved" count 1
                1 outOf
                    190 weight
                    "obj.trail_elite_emote_exp1" count
                    1 transformObj
                    { player ->
                        player.clueScrollTransformObj("obj.trail_elite_emote_exp1")
                    }
            },
    )

// Unknown wiki drop rates (text rarity — need data collection):
