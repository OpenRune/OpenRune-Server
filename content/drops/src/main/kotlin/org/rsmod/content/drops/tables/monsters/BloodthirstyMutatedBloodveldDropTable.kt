package org.rsmod.content.drops.tables.monsters

import dtx.rs.RSDropTable
import dtx.rs.npcs
import dtx.rs.areas
import org.rsmod.api.droptable.rsPlayerGuaranteedTable
import org.rsmod.api.droptable.rsPlayerTertiaryTable
import org.rsmod.api.droptable.rsPlayerWeightedTable
import org.rsmod.content.drops.tables.shared.SharedDropTables
import org.rsmod.api.droptable.DropRollItem
import org.rsmod.content.drops.brimstoneKeyRoll
import org.rsmod.api.droptable.RegisterDropTable
import org.rsmod.game.entity.Player

@field:RegisterDropTable
@JvmField
public val bloodthirstyMutatedBloodveldDropTable: RSDropTable<Player, DropRollItem> = RSDropTable(
    tableIdentifier = "Bloodthirsty mutated Bloodveld Drops",
    npcs = npcs("npc.league_superior_kourend_bloodveld"),
    guaranteed = rsPlayerGuaranteedTable {
        "obj.arceuus_corpse_bloodveld" count 1
        "obj.prif_crystal_shard" count 1 condition { player ->
            // Drops Need Manual: Crystal shards are only dropped by those found within the Iorwerth Dungeon.
             true
        }
    },
    mainTable = rsPlayerWeightedTable(total = 128) {
        name("Bloodthirsty mutated Bloodveld Drops")
        5 weight "obj.black_med_helm" count 1
        5 weight "obj.mithril_axe" count 1
        6 weight "obj.mithril_battleaxe" count 1
        2 weight "obj.mithril_platebody" count 1
        3 weight "obj.adamant_knife" count 2
        3 weight "obj.adamant_chainbody" count 1
        8 weight "obj.mithril_full_helm" count 1
        1 weight "obj.adamant_longsword" count 1
        3 weight "obj.adamant_scimitar" count 1
        2 weight "obj.rune_med_helm" count 1
        1 weight "obj.black_armoured_boots" count 1
        1 weight "obj.rune_dagger" count 1
        1 weight "obj.rune_battleaxe" count 1
        7 weight "obj.airrune" count 105
        9 weight "obj.firerune" count 75
        13 weight "obj.bloodrune" count 30
        10 weight "obj.bloodrune" count 7
        7 weight "obj.soulrune" count 4
        10 weight "obj.coins" count 350
        3 weight "obj.coins" count 11
        4 weight "obj.bow_string" count 1
        7 weight "obj.gold_ore" count 1
        5 weight "obj.meat_pie" count 1
        5 weight "obj.mithril_bar" count 1
        2 weight "obj.strung_ruby_amulet" count 1
        5 outOf 166 separate rsPlayerWeightedTable {
            5 weight "obj.cert_raw_lobster" count 60..80
            5 weight "obj.cert_unicorn_horn" count 60..150
        }
        10 outOf 166 separate rsPlayerWeightedTable {
            10 weight "obj.cert_snape_grass" count 60..150
            10 weight "obj.irit_seed" count 10..15
            10 weight "obj.cert_limpwurt_root" count 60..150
        }
        15 outOf 166 separate rsPlayerWeightedTable {
            15 weight "obj.cert_yew_logs" count 70..90
            15 weight "obj.cert_raw_monkfish" count 60..80
        }
        20 outOf 166 separate "obj.cert_white_berries" count 60..150
        30 outOf 166 separate rsPlayerWeightedTable {
            30 weight "obj.kwuarm_seed" count 8..15
            30 weight "obj.ranarr_seed" count 8..15
        }
        16 outOf 166 separate "obj.cert_blue_dragon_scale" count 20..40

        3 weight SharedDropTables.rareDrop
        2 weight SharedDropTables.gem
    },
    tertiaries = rsPlayerTertiaryTable {
        1 outOf 1 weight "obj.trail_clue_hard_map001" count 1
        onBuilder { brimstoneKeyRoll(konarTaskBonus = true) }
    },
)
