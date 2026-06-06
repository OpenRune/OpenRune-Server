package org.rsmod.content.drops.tables.monsters

import dtx.rs.RSDropTable
import dtx.rs.npcs
import dtx.rs.areas
import org.rsmod.api.droptable.rsPlayerGuaranteedTable
import org.rsmod.api.droptable.rsPlayerTertiaryTable
import org.rsmod.api.droptable.rsPlayerWeightedTable
import org.rsmod.content.drops.tables.shared.SharedDropTables
import org.rsmod.api.droptable.DropRollItem
import org.rsmod.api.droptable.ringNothing
import org.rsmod.api.droptable.RegisterDropTable
import org.rsmod.game.entity.Player

@field:RegisterDropTable
@JvmField
public val muggerDropTable: RSDropTable<Player, DropRollItem> = RSDropTable(
    tableIdentifier = "Mugger Drops",
    npcs = npcs("npc.jail_mugger", "npc.mugger", "npc.varlamore_mugger", "npc.zeah_mugger"),
    mainTable = rsPlayerWeightedTable(total = 128) {
        name("Mugger Drops")
        27 weight "obj.bolt" count 2..12
        3 weight "obj.mindrune" count 9
        2 weight "obj.waterrune" count 6
        2 weight "obj.earthrune" count 5
        12 weight "obj.coins" count 5
        3 weight "obj.coins" count 15
        1 weight "obj.coins" count 25
        40 weight "obj.rope" count 1
        13 weight ringNothing()
        1 weight "obj.knife" count 1
        6 weight "obj.fishing_bait" count 1
        2 weight "obj.copper_ore" count 1
        2 weight "obj.bronze_med_helm" count 1
        1 weight "obj.cabbage" count 1

        13 weight SharedDropTables.herb
    },
    tertiaries = rsPlayerTertiaryTable {
        1 outOf 80 weight "obj.trail_clue_beginner" count 1 transformObj { player ->
            // Drops Need Manual (item): Clue scrolls will drop as scroll boxes after the completion of X Marks the Spot.
             null
        }
    },
)
