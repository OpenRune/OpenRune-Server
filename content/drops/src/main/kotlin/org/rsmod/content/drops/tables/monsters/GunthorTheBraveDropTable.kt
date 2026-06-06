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
public val gunthorTheBraveDropTable: RSDropTable<Player, DropRollItem> = RSDropTable(
    tableIdentifier = "Gunthor the brave Drops",
    npcs = npcs("npc.gunthor_the_brave"),
    mainTable = rsPlayerWeightedTable(total = 128) {
        name("Gunthor the brave Drops")
        6 weight "obj.iron_axe" count 1
        4 weight "obj.bronze_battleaxe" count 1
        1 weight "obj.iron_mace" count 1
        4 weight "obj.bronze_arrow" count 10
        4 weight "obj.chaosrune" count 3
        3 weight "obj.iron_arrow" count 8
        3 weight "obj.earthrune" count 5
        2 weight "obj.mindrune" count 10
        2 weight "obj.firerune" count 8
        1 weight "obj.lawrune" count 2
        42 weight "obj.coins" count 8
        9 weight "obj.coins" count 12
        5 weight "obj.coins" count 25
        3 weight "obj.coins" count 32
        32 weight ringNothing()
        1 weight "obj.cooked_meat" count 1
        1 weight "obj.tin_ore" count 1
        1 weight "obj.amulet_mould" count 1
        1 weight "obj.beer" count 1
        1 weight "obj.fur" count 1
        1 weight "obj.flier" count 1

        1 weight SharedDropTables.gem
    },
    tertiaries = rsPlayerTertiaryTable {
        1 outOf 1 weight "obj.trail_elite_riddle_key32" count 1 condition { player ->
            // Drops Need Manual: Keys are only dropped when completing a medium clue scroll asking to kill a barbarian.
             true
        }
        1 outOf 30 weight "obj.trail_clue_beginner" count 1 transformObj { player ->
            // Drops Need Manual (item): Clue scrolls will drop as scroll boxes after the completion of X Marks the Spot.
             null
        }
    },
)
