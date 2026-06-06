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
public val frostCrabDropTable: RSDropTable<Player, DropRollItem> = RSDropTable(
    tableIdentifier = "Frost Crab Drops",
    npcs = npcs("npc.frostcrab", "npc.frostcrab_inactive", "npc.frostcrab_small", "npc.frostcrab_small_inactive"),
    mainTable = rsPlayerWeightedTable(total = 182) {
        name("Frost Crab Drops")
        12 weight "obj.bronze_pickaxe" count 1
        10 weight "obj.iron_pickaxe" count 1
        16 weight "obj.coins" count 2
        39 weight "obj.coins" count 4
        8 weight "obj.coins" count 8
        12 weight "obj.coins" count 24
        11 weight "obj.coins" count 36
        7 weight "obj.waterrune" count 1
        7 weight "obj.waterrune" count 2
        4 weight "obj.waterrune" count 5
        4 weight "obj.waterrune" count 10
        4 weight "obj.waterrune" count 15
        8 weight "obj.tin_ore" count 3
        4 weight "obj.copper_ore" count 3
        4 weight "obj.iron_ore" count 1
        2 weight "obj.iron_ore" count 2
        2 weight "obj.coal" count 1
        4 weight "obj.coal" count 2
        2 weight "obj.coal" count 3
        6 weight "obj.chisel" count 1
        6 weight "obj.bucket_water" count 1
        2 weight "obj.spinach_roll" count 1
        2 weight "obj.casket" count 1
        4 weight "obj.softclay" count 1

        2 weight SharedDropTables.gem
    },
    tertiaries = rsPlayerTertiaryTable {
        1 outOf 90 weight "obj.frozen_tear" count 5
        1 outOf 1354 weight "obj.varlamore_key_half_1" count 1
        // Drops Need Manual (rate): The easy clue scroll drop rate increases to 1/121 after unlocking the easy Combat Achievements rewards tier.
        1 outOf 128 weight "obj.trail_clue_easy_simple001" count 1 transformObj { player ->
            // Drops Need Manual (item): Clue scrolls will drop as scroll boxes after the completion of X Marks the Spot.
             null
        }
    },
)
