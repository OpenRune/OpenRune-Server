package org.rsmod.content.drops.tables.monsters

import dtx.rs.RSDropTable
import dtx.rs.npcs
import dtx.rs.areas
import org.rsmod.api.droptable.rsPlayerGuaranteedTable
import org.rsmod.api.droptable.rsPlayerTertiaryTable
import org.rsmod.api.droptable.rsPlayerWeightedTable
import org.rsmod.content.drops.tables.shared.SharedDropTables
import org.rsmod.api.droptable.DropRollItem
import org.rsmod.api.droptable.RegisterDropTable
import org.rsmod.game.entity.Player

@field:RegisterDropTable
@JvmField
public val bloodthirstyBansheeDropTable: RSDropTable<Player, DropRollItem> = RSDropTable(
    tableIdentifier = "Bloodthirsty banshee Drops",
    npcs = npcs("npc.league_superior_banshee"),
    mainTable = rsPlayerWeightedTable(total = 128) {
        name("Bloodthirsty banshee Drops")
        2 weight "obj.iron_mace" count 1
        2 weight "obj.iron_dagger" count 1
        1 weight "obj.iron_kiteshield" count 1
        3 weight "obj.airrune" count 3
        3 weight "obj.cosmicrune" count 2
        2 weight "obj.chaosrune" count 3
        1 weight "obj.firerune" count 7
        1 weight "obj.chaosrune" count 7
        10 weight "obj.coins" count 13
        8 weight "obj.coins" count 26
        8 weight "obj.coins" count 35
        22 weight "obj.cert_blankrune_high" count 13
        22 weight "obj.fishing_bait" count 15
        5 weight "obj.fishing_bait" count 7
        1 weight "obj.eye_of_newt" count 1
        1 weight "obj.iron_ore" count 1
        5 outOf 40 separate rsPlayerWeightedTable {
            5 weight "obj.cert_raw_lobster" count 60..80
            5 weight "obj.cert_unicorn_horn" count 60..150
        }
        10 outOf 40 separate rsPlayerWeightedTable {
            10 weight "obj.cert_snape_grass" count 60..150
            10 weight "obj.irit_seed" count 10..15
            10 weight "obj.cert_limpwurt_root" count 60..150
        }
        1 outOf 512 separate "obj.mystic_gloves_dark" count 1

        34 weight SharedDropTables.herb
        2 weight SharedDropTables.gem
    },
    tertiaries = rsPlayerTertiaryTable {
        1 outOf 1 weight "obj.trail_clue_easy_simple001" count 1
    },
)
