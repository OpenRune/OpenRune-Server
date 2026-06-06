package org.rsmod.content.drops.tables.monsters

import dtx.rs.RSDropTable
import dtx.rs.npcs
import dtx.rs.areas
import org.rsmod.api.droptable.rsPlayerGuaranteedTable
import org.rsmod.api.droptable.rsPlayerTertiaryTable
import org.rsmod.api.droptable.rsPlayerWeightedTable
import org.rsmod.content.drops.tables.shared.SharedDropTables
import org.rsmod.api.droptable.DropRollItem
import org.rsmod.content.drops.clueScrollTransformObj
import org.rsmod.api.droptable.RegisterDropTable
import org.rsmod.game.entity.Player

@field:RegisterDropTable
@JvmField
public val swampCrabDropTable: RSDropTable<Player, DropRollItem> = RSDropTable(
    tableIdentifier = "Swamp Crab Drops",
    npcs = npcs("npc.swampcrab", "npc.swampcrab_bloom", "npc.swampcrab_inactive"),
    guaranteed = rsPlayerGuaranteedTable {
        "obj.mortmyremushroom" count 3 condition { player ->
            // Drops Need Manual: Only if the Bloom spell is used while being attacked by a Swamp Crab.
             true
        }
    },
    mainTable = rsPlayerWeightedTable(total = 128) {
        name("Swamp Crab Drops")
        12 weight "obj.bronze_pickaxe" count 1
        6 weight "obj.bronze_axe" count 1
        5 weight "obj.iron_axe" count 1
        4 weight "obj.logs" count 8
        2 weight "obj.oak_logs" count 4
        2 weight "obj.copper_ore" count 4
        4 weight "obj.tin_ore" count 4
        2 weight "obj.iron_ore" count 2
        2 weight "obj.coal" count 2
        1 weight "obj.willow_logs" count 2
        2 weight "obj.mortmyremushroom" count 1
        1 weight "obj.yew_logs" count 1
        19 weight "obj.coins" count 4..10
        8 weight "obj.coins" count 42
        6 weight "obj.coins" count 120
        4 weight "obj.seaweed" count 2..8
        29 weight "obj.knife" count 4
        3 weight "obj.chisel" count 1
        2 weight "obj.fishing_bait" count 12
        2 weight "obj.edible_seaweed" count 3
        9 weight "obj.hollow_bark" count 1..2
        1 weight "obj.spinach_roll" count 1
        1 weight "obj.casket" count 1

        1 weight SharedDropTables.gem
    },
    tertiaries = rsPlayerTertiaryTable {
        1 outOf 91 weight "obj.trail_clue_easy_simple001" count 1 transformObj { player ->
             player.clueScrollTransformObj("obj.trail_clue_easy_simple001")
        }
    },
)
