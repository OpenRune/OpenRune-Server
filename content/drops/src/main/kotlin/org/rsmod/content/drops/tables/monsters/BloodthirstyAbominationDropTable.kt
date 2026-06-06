package org.rsmod.content.drops.tables.monsters

import dtx.rs.RSDropTable
import dtx.rs.npcs
import dtx.rs.areas
import org.rsmod.api.droptable.rsPlayerGuaranteedTable
import org.rsmod.api.droptable.rsPlayerTertiaryTable
import org.rsmod.api.droptable.rsPlayerWeightedTable
import org.rsmod.content.drops.tables.shared.SharedDropTables
import org.rsmod.api.droptable.DropRollItem
import org.rsmod.api.droptable.nothing
import org.rsmod.api.droptable.RegisterDropTable
import org.rsmod.game.entity.Player

@field:RegisterDropTable
@JvmField
public val bloodthirstyAbominationDropTable: RSDropTable<Player, DropRollItem> = RSDropTable(
    tableIdentifier = "Bloodthirsty abomination Drops",
    npcs = npcs("npc.league_superior_cave_horror"),
    guaranteed = rsPlayerGuaranteedTable {
        "obj.arceuus_corpse_horror" count 1
    },
    mainTable = rsPlayerWeightedTable(total = 128) {
        name("Bloodthirsty abomination Drops")
        3 weight "obj.mithril_axe" count 1
        1 weight "obj.rune_dagger" count 1
        1 weight "obj.adamant_full_helm" count 1
        1 weight "obj.mithril_kiteshield" count 1
        6 weight "obj.naturerune" count 6
        5 weight "obj.naturerune" count 4
        1 weight "obj.naturerune" count 3
        28 weight "obj.coins" count 44
        12 weight "obj.coins" count 132
        1 weight "obj.coins" count 440
        7 weight "obj.limpwurt_root" count 1
        7 weight "obj.cert_teak_logs" count 4
        3 weight "obj.mahogany_logs" count 2
        5 outOf 136 separate rsPlayerWeightedTable {
            5 weight "obj.cert_raw_lobster" count 60..80
            5 weight "obj.cert_unicorn_horn" count 60..150
        }
        10 outOf 136 separate rsPlayerWeightedTable {
            10 weight "obj.cert_snape_grass" count 60..150
            10 weight "obj.irit_seed" count 10..15
            10 weight "obj.cert_limpwurt_root" count 60..150
        }
        15 outOf 136 separate rsPlayerWeightedTable {
            15 weight "obj.cert_yew_logs" count 70..90
            15 weight "obj.cert_raw_monkfish" count 60..80
        }
        20 outOf 136 separate "obj.cert_white_berries" count 60..150
        30 outOf 136 separate "obj.kwuarm_seed" count 8..15
        16 outOf 136 separate "obj.ranarr_seed" count 8..15
        1 outOf 512 separate "obj.harmless_black_mask_10" count 1

        13 weight SharedDropTables.herb
        5 weight SharedDropTables.gem
        18 weight SharedDropTables.rareSeed
        16 weight nothing()
    },
    tertiaries = rsPlayerTertiaryTable {
        1 outOf 1 weight "obj.trail_clue_hard_map001" count 1
        1 outOf 400 weight "obj.dorgesh_construction_bone" count 1
        1 outOf 5013 weight "obj.dorgesh_construction_bone_curved" count 1
    },
)
