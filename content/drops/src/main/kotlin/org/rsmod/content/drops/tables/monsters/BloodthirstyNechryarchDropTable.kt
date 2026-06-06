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
public val bloodthirstyNechryarchDropTable: RSDropTable<Player, DropRollItem> = RSDropTable(
    tableIdentifier = "Bloodthirsty Nechryarch Drops",
    npcs = npcs("npc.league_superior_nechryael"),
    guaranteed = rsPlayerGuaranteedTable {
        "obj.prif_crystal_shard" count 1 condition { player ->
            // Drops Need Manual: Crystal shards are only dropped by those found within the Iorwerth Dungeon.
             true
        }
    },
    mainTable = rsPlayerWeightedTable(total = 664) {
        name("Bloodthirsty Nechryarch Drops")
        5 weight "obj.cert_raw_lobster" count 60..80
        10 weight "obj.cert_snape_grass" count 60..150
        10 weight "obj.irit_seed" count 10..15
        5 weight "obj.cert_unicorn_horn" count 60..150
        10 weight "obj.cert_limpwurt_root" count 60..150
        15 weight "obj.cert_yew_logs" count 70..90
        15 weight "obj.cert_raw_monkfish" count 60..80
        20 weight "obj.cert_white_berries" count 60..150
        30 weight "obj.kwuarm_seed" count 8..15
        30 weight "obj.ranarr_seed" count 8..15
        25 weight "obj.cert_blue_dragon_scale" count 20..40
        20 weight "obj.cert_raw_shark" count 60..80
        25 weight "obj.cert_red_spiders_eggs" count 40..60
        35 weight "obj.cert_magic_logs" count 30..50
        30 weight "obj.cert_wine_of_zamorak" count 30..50
        30 weight "obj.dwarf_weed_seed" count 5..8
        5 weight "obj.cert_raw_lobster" count 60..80
        10 weight "obj.cert_snape_grass" count 60..150
        10 weight "obj.irit_seed" count 10..15
        5 weight "obj.cert_unicorn_horn" count 60..150
        10 weight "obj.cert_limpwurt_root" count 60..150
        15 weight "obj.cert_yew_logs" count 70..90
        15 weight "obj.cert_raw_monkfish" count 60..80
        20 weight "obj.cert_white_berries" count 60..150
        30 weight "obj.kwuarm_seed" count 8..15
        30 weight "obj.ranarr_seed" count 8..15
        25 weight "obj.cert_blue_dragon_scale" count 20..40
        20 weight "obj.cert_raw_shark" count 60..80
        25 weight "obj.cert_red_spiders_eggs" count 40..60
        35 weight "obj.cert_magic_logs" count 30..50
        30 weight "obj.cert_wine_of_zamorak" count 30..50
        30 weight "obj.dwarf_weed_seed" count 5..8
        4 outOf 116 separate rsPlayerWeightedTable {
            4 weight "obj.adamant_platelegs" count 1
            4 weight "obj.rune_2h_sword" count 1
        }
        3 outOf 116 separate "obj.rune_full_helm" count 1
        2 outOf 116 separate "obj.adamant_kiteshield" count 1
        1 outOf 116 separate "obj.rune_armoured_boots" count 1
        8 outOf 116 separate "obj.chaosrune" count 37
        6 outOf 116 separate rsPlayerWeightedTable {
            6 weight "obj.deathrune" count 5
            6 weight "obj.deathrune" count 10
        }
        5 outOf 116 separate "obj.lawrune" count 25..35
        4 outOf 116 separate "obj.bloodrune" count 15..20
        13 outOf 116 separate "obj.coins" count 1000..1499
        11 outOf 116 separate "obj.coins" count 1500..2000
        6 outOf 116 separate "obj.coins" count 2500..2999
        3 outOf 116 separate rsPlayerWeightedTable {
            3 weight "obj.coins" count 3000..3500
            3 weight "obj.coins" count 500..999
        }
        1 outOf 116 separate "obj.coins" count 5000
        4 outOf 116 separate "obj.cert_softclay" count 25
        3 outOf 116 separate "obj.tuna" count 1
        7 outOf 128 separate rsPlayerWeightedTable {
            7 weight "obj.adamant_kiteshield" count 1
            7 weight "obj.rune_axe" count 1
            7 weight "obj.rune_sq_shield" count 1
        }
        5 outOf 128 separate "obj.adamant_battleaxe" count 1
        4 outOf 128 separate "obj.rune_med_helm" count 1
        3 outOf 128 separate "obj.rune_full_helm" count 1
        2 outOf 128 separate "obj.mystic_air_staff" count 1
        1 outOf 128 separate rsPlayerWeightedTable {
            1 weight "obj.rune_armoured_boots" count 1
            1 weight "obj.rune_chainbody" count 1
        }
        12 outOf 128 separate "obj.deathrune" count 23
        10 outOf 128 separate rsPlayerWeightedTable {
            10 weight "obj.bloodrune" count 20
            10 weight "obj.chaosrune" count 50
        }
        6 outOf 128 separate "obj.airrune" count 150
        5 outOf 128 separate "obj.soulrune" count 25
        10 outOf 128 separate "obj.lobster" count 1
        8 outOf 128 separate "obj.coins" count 2000..2500
        7 outOf 128 separate "obj.cert_gold_bar" count 5
        6 outOf 128 separate "obj.tuna" count 2
        2 outOf 128 separate "obj.cert_wine_of_zamorak" count 3

        7 weight SharedDropTables.herb
        3 weight SharedDropTables.gem
        1 weight SharedDropTables.rareDrop
        18 weight SharedDropTables.rareSeed
        5 weight SharedDropTables.rareSeed
    },
    tertiaries = rsPlayerTertiaryTable {
        1 outOf 1 weight "obj.trail_clue_hard_map001" count 1
        onBuilder { brimstoneKeyRoll(konarTaskBonus = true) }
        onBuilder { brimstoneKeyRoll(konarTaskBonus = true) }
        1 outOf 1 weight "obj.trail_clue_hard_map001" count 1
    },
)
