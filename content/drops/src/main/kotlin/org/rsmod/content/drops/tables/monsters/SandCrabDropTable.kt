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
public val sandCrabDropTable: RSDropTable<Player, DropRollItem> = RSDropTable(
    tableIdentifier = "Sand Crab Drops",
    npcs = npcs("npc.zeah_sandcrab", "npc.zeah_sandcrab_inactive", "npc.zeah_sandcrab_small", "npc.zeah_sandcrab_small_inactive"),
    mainTable = rsPlayerWeightedTable(total = 128) {
        name("Sand Crab Drops")
        6 weight "obj.bronze_pickaxe" count 1
        5 weight "obj.iron_pickaxe" count 1
        29 weight "obj.coins" count 4
        6 weight "obj.coins" count 8
        8 weight "obj.coins" count 36
        4 weight "obj.seaweed" count 1
        4 weight "obj.seaweed" count 2
        2 weight "obj.seaweed" count 5
        2 weight "obj.edible_seaweed" count 2
        4 weight "obj.tin_ore" count 3
        2 weight "obj.iron_ore" count 1
        2 weight "obj.coal" count 2
        2 weight "obj.copper_ore" count 3
        9 weight "obj.oystershell" count 1
        12 weight "obj.oystershell" count 2
        3 weight "obj.oysterempty" count 1
        1 weight "obj.oysterempty" count 3
        1 weight "obj.smalloysterpearls" count 1
        13 weight ringNothing()
        6 weight "obj.bucket_sand" count 1
        2 weight "obj.opal_bolttips" count 5
        2 weight "obj.fishing_bait" count 10
        1 weight "obj.spinach_roll" count 1
        1 weight "obj.casket" count 1

        1 weight SharedDropTables.gem
    },
    tertiaries = rsPlayerTertiaryTable {
        // Drops Need Manual (rate): The easy clue scroll drop rate increases to 1/121 after unlocking the easy Combat Achievements rewards tier.
        1 outOf 128 weight "obj.trail_clue_easy_simple001" count 1 transformObj { player ->
            // Drops Need Manual (item): Clue scrolls will drop as scroll boxes after the completion of X Marks the Spot.
             null
        }
    },
)
