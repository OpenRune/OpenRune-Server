package org.rsmod.content.drops.tables.monsters

import dtx.rs.RSDropTable
import dtx.rs.npcs
import dtx.rs.areas
import org.rsmod.api.droptable.rsPlayerGuaranteedTable
import org.rsmod.api.droptable.rsPlayerTertiaryTable
import org.rsmod.api.droptable.rsPlayerWeightedTable
import org.rsmod.api.droptable.DropRollItem
import org.rsmod.api.droptable.nothing
import org.rsmod.api.droptable.ringNothing
import org.rsmod.api.droptable.RegisterDropTable
import org.rsmod.game.entity.Player

@field:RegisterDropTable
@JvmField
public val tzHaarXilDropTable: RSDropTable<Player, DropRollItem> = RSDropTable(
    tableIdentifier = "TzHaar-Xil Drops",
    npcs = npcs("npc.tzhaar_xil1", "npc.tzhaar_xil2", "npc.tzhaar_xil3", "npc.tzhaar_xil4", "npc.tzhaar_xil5", "npc.tzhaar_xil6"),
    mainTable = rsPlayerWeightedTable(total = 512) {
        name("TzHaar-Xil Drops")
        16 weight "obj.uncut_sapphire" count 1
        8 weight "obj.uncut_emerald" count 1
        4 weight "obj.uncut_ruby" count 1
        1 weight "obj.uncut_diamond" count 1
        195 weight ringNothing()
        4 outOf 2048 separate "obj.tzhaar_throwingring" count 10..29
        3 outOf 2048 separate rsPlayerWeightedTable {
            3 weight "obj.tzhaar_splitsword" count 1
            3 weight "obj.tzhaar_knife" count 1
        }
        6 outOf 2048 separate "obj.tzhaar_cape_obsidian" count 1
        72 outOf 128 separate "obj.tzhaar_token" count 1..32
        // Pool padding (F2P drops removed / subtable access missing from wiki parse)
        288 weight nothing()
    },
)
