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
public val bloodthirstyKuraskDropTable: RSDropTable<Player, DropRollItem> = RSDropTable(
    tableIdentifier = "Bloodthirsty kurask Drops",
    npcs = npcs("npc.league_superior_kurask"),
    guaranteed = rsPlayerGuaranteedTable {
        "obj.prif_crystal_shard" count 1 condition { player ->
            // Drops Need Manual: Crystal shards are only dropped by those found within the Iorwerth Dungeon.
             true
        }
    },
    mainTable = rsPlayerWeightedTable(total = 294) {
        name("Bloodthirsty kurask Drops")
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
        3 outOf 124 separate rsPlayerWeightedTable {
            3 weight "obj.mithril_kiteshield" count 1
            3 weight "obj.rune_longsword" count 1
            3 weight "obj.adamant_platebody" count 1
            3 weight "obj.rune_axe" count 1
        }
        10 outOf 124 separate "obj.naturerune" count 10
        7 outOf 124 separate "obj.naturerune" count 15
        4 outOf 124 separate "obj.naturerune" count 30
        16 outOf 124 separate "obj.coins" count 2000..3000
        6 outOf 124 separate rsPlayerWeightedTable {
            6 weight "obj.cert_flax" count 100
            6 weight "obj.cert_white_berries" count 12
        }
        5 outOf 124 separate rsPlayerWeightedTable {
            5 weight "obj.cert_big_bones" count 20
            5 weight "obj.coins" count 10000
        }
        4 outOf 124 separate rsPlayerWeightedTable {
            4 weight "obj.cert_papaya" count 10
            4 weight "obj.cert_coconut" count 10
        }
        1 outOf 384 separate "obj.leafbladed_sword" count 1
        1 outOf 512 separate "obj.mystic_robe_top_light" count 1
        1 outOf 1026 separate "obj.leafbladed_battleaxe" count 1

        18 weight SharedDropTables.herb
        6 weight SharedDropTables.gem
        15 weight SharedDropTables.rareSeed
    },
    tertiaries = rsPlayerTertiaryTable {
        1 outOf 1 weight "obj.trail_clue_hard_map001" count 1
        onBuilder { brimstoneKeyRoll(konarTaskBonus = true) }
        1 outOf 3000 weight "obj.poh_trophydrop_kurask" count 1
    },
)
