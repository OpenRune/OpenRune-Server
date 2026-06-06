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
public val bloodthirstyJellyDropTable: RSDropTable<Player, DropRollItem> = RSDropTable(
    tableIdentifier = "Bloodthirsty Jelly Drops",
    npcs = npcs("npc.league_superior_jelly"),
    mainTable = rsPlayerWeightedTable(total = 128) {
        name("Bloodthirsty Jelly Drops")
        11 weight "obj.steel_battleaxe" count 1
        7 weight "obj.steel_2h_sword" count 1
        3 weight "obj.steel_axe" count 1
        2 weight "obj.mithril_kiteshield" count 1
        1 weight "obj.mithril_armoured_boots" count 1
        1 weight "obj.rune_full_helm" count 1
        39 weight "obj.coins" count 102
        30 weight "obj.coins" count 44
        10 weight "obj.coins" count 220
        7 weight "obj.coins" count 11
        2 weight "obj.coins" count 460
        5 weight "obj.chaosrune" count 15
        3 weight "obj.deathrune" count 5
        2 weight "obj.gold_bar" count 1
        1 weight "obj.thread" count 10
        5 outOf 76 separate rsPlayerWeightedTable {
            5 weight "obj.cert_raw_lobster" count 60..80
            5 weight "obj.cert_unicorn_horn" count 60..150
        }
        10 outOf 76 separate rsPlayerWeightedTable {
            10 weight "obj.cert_snape_grass" count 60..150
            10 weight "obj.irit_seed" count 10..15
            10 weight "obj.cert_limpwurt_root" count 60..150
        }
        15 outOf 76 separate rsPlayerWeightedTable {
            15 weight "obj.cert_yew_logs" count 70..90
            15 weight "obj.cert_raw_monkfish" count 60..80
        }
        6 outOf 76 separate "obj.cert_white_berries" count 60..150

        4 weight SharedDropTables.gem
    },
    tertiaries = rsPlayerTertiaryTable {
        1 outOf 1 weight "obj.trail_clue_hard_map001" count 1
        onBuilder { brimstoneKeyRoll(konarTaskBonus = true) }
    },
)
