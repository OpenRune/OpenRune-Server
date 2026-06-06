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
public val drunkenManDropTable: RSDropTable<Player, DropRollItem> = RSDropTable(
    tableIdentifier = "Drunken man Drops",
    npcs = npcs("npc.falador_man1"),
    mainTable = rsPlayerWeightedTable(total = 128) {
        name("Drunken man Drops")
        2 weight "obj.bronze_med_helm" count 1
        1 weight "obj.iron_dagger" count 1
        22 weight "obj.bolt" count 2..12
        3 weight "obj.bronze_arrow" count 7
        2 weight "obj.earthrune" count 4
        2 weight "obj.firerune" count 6
        2 weight "obj.mindrune" count 9
        1 weight "obj.chaosrune" count 2
        38 weight "obj.coins" count 3
        9 weight "obj.coins" count 5
        4 weight "obj.coins" count 15
        1 weight "obj.coins" count 25
        8 weight ringNothing()
        5 weight "obj.fishing_bait" count 1
        2 weight "obj.copper_ore" count 1
        2 weight "obj.earth_talisman" count 1
        1 weight "obj.cabbage" count 1

        23 weight SharedDropTables.herb
    },
    tertiaries = rsPlayerTertiaryTable {
        1 outOf 1 weight "obj.trail_elite_riddle_key32" count 1 condition { player ->
            // Drops Need Manual: The key is only dropped when completing a medium clue scroll asking the player to kill a Man.
             true
        }
        1 outOf 90 weight "obj.trail_clue_beginner" count 1 transformObj { player ->
            // Drops Need Manual (item): Clue scrolls will drop as scroll boxes after the completion of X Marks the Spot.
             null
        }
    },
)
