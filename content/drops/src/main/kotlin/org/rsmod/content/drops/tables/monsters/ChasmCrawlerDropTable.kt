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
public val chasmCrawlerDropTable: RSDropTable<Player, DropRollItem> = RSDropTable(
    tableIdentifier = "Chasm Crawler Drops",
    npcs = npcs("npc.superior_cave_crawler", "npc.superior_cave_crawler_ice"),
    mainTable = rsPlayerWeightedTable(total = 128) {
        name("Chasm Crawler Drops")
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
        1 outOf 729 separate "obj.varlamore_key_half_1" count 1 condition { player ->
            // Drops Need Manual: Only dropped by those found in the Ruins of Tapoyauik.
             true
        }

        22 weight SharedDropTables.herb
        1 weight SharedDropTables.gem
        // Pool padding (F2P drops removed / subtable access missing from wiki parse)
        26 weight nothing()
    },
)
