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
public val caveSlimeDropTable: RSDropTable<Player, DropRollItem> = RSDropTable(
    tableIdentifier = "Cave slime Drops",
    npcs = npcs("npc.swamp_cave_slime"),
    guaranteed = rsPlayerGuaranteedTable {
        "obj.swamp_tar" count 1..6
    },
    mainTable = rsPlayerWeightedTable(total = 128) {
        name("Cave slime Drops")
        3 weight "obj.bronze_axe" count 1
        7 weight "obj.iron_sword" count 1
        1 weight "obj.bronze_full_helm" count 1
        2 weight "obj.iron_kiteshield" count 1
        1 weight "obj.iron_armoured_boots" count 1
        5 weight "obj.waterrune" count 15
        3 weight "obj.earthrune" count 5
        7 weight "obj.coins" count 1
        30 weight "obj.coins" count 4
        39 weight "obj.coins" count 10
        10 weight "obj.coins" count 22
        2 weight "obj.coins" count 46
        11 weight "obj.torch_unlit" count 1
        1 weight "obj.oil_lantern_frame" count 1
        2 weight "obj.gold_bar" count 1

        4 weight SharedDropTables.gem
    },
    tertiaries = rsPlayerTertiaryTable {
        // Drops Need Manual (rate): The easy clue scroll drop rate increases to 1/121 after unlocking the easy Combat Achievements rewards tier.
        1 outOf 128 weight "obj.trail_clue_easy_simple001" count 1 transformObj { player ->
            // Drops Need Manual (item): Clue scrolls will drop as scroll boxes after the completion of X Marks the Spot.
             null
        }
    },
)
