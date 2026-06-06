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
import org.rsmod.api.droptable.ringNothing
import org.rsmod.api.droptable.RegisterDropTable
import org.rsmod.game.entity.Player

@field:RegisterDropTable
@JvmField
public val bloodthirstyCrawlerDropTable: RSDropTable<Player, DropRollItem> = RSDropTable(
    tableIdentifier = "Bloodthirsty Crawler Drops",
    npcs = npcs("npc.league_superior_cave_crawler"),
    mainTable = rsPlayerWeightedTable(total = 128) {
        name("Bloodthirsty Crawler Drops")
        1 weight "obj.bronze_armoured_boots" count 1
        6 weight "obj.naturerune" count 3..4
        5 weight "obj.firerune" count 12
        2 weight "obj.earthrune" count 9
        13 weight "obj.vial_water" count 1
        5 weight "obj.white_berries" count 1
        2 weight "obj.unicorn_horn_dust" count 1
        1 weight "obj.eye_of_newt" count 1
        1 weight "obj.red_spiders_eggs" count 1
        1 weight "obj.limpwurt_root" count 1
        1 weight "obj.snape_grass" count 1
        5 weight "obj.coins" count 3
        3 weight "obj.coins" count 8
        3 weight "obj.coins" count 29
        1 weight "obj.coins" count 10
        29 weight ringNothing()
        5 outOf 40 separate rsPlayerWeightedTable {
            5 weight "obj.cert_raw_lobster" count 60..80
            5 weight "obj.cert_unicorn_horn" count 60..150
        }
        10 outOf 40 separate rsPlayerWeightedTable {
            10 weight "obj.cert_snape_grass" count 60..150
            10 weight "obj.irit_seed" count 10..15
            10 weight "obj.cert_limpwurt_root" count 60..150
        }

        22 weight SharedDropTables.herb
        1 weight SharedDropTables.gem
        // Pool padding (F2P drops removed / subtable access missing from wiki parse)
        26 weight nothing()
    },
)
