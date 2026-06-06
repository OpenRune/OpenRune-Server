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
import org.rsmod.api.droptable.RegisterDropTable
import org.rsmod.game.entity.Player

@field:RegisterDropTable
@JvmField
public val borrokarDropTable: RSDropTable<Player, DropRollItem> = RSDropTable(
    tableIdentifier = "Borrokar Drops",
    npcs = npcs("npc.viking_man3"),
    mainTable = rsPlayerWeightedTable(total = 512) {
        name("Borrokar Drops")
        100 weight "obj.bronze_warhammer" count 1
        70 weight "obj.iron_warhammer" count 1
        1 weight "obj.viking_sword" count 1
        1 weight "obj.viking_shield" count 1
        1 weight "obj.viking_helmet" count 1
        10 weight "obj.copper_ore" count 5
        10 weight "obj.tin_ore" count 5
        10 weight "obj.iron_ore" count 5
        10 weight "obj.coal" count 1
        10 weight "obj.steel_bar" count 1
        20 weight "obj.coins" count 6
        15 weight "obj.coins" count 15
        10 weight "obj.coins" count 16
        10 weight "obj.coins" count 20
        5 weight "obj.coins" count 38
        50 weight "obj.tinderbox" count 1
        50 weight "obj.vial_empty" count 1
        50 weight "obj.vial_water" count 1
        17 weight "obj.bucket_empty" count 1
        30 weight "obj.viking_strung_lyre" count 1
        2 weight "obj.beer" count 1
        4 weight "obj.eye_of_newt" count 1
        2 weight "obj.snape_grass" count 1
        2 weight "obj.jangerberries" count 1
        1 weight "obj.blamish_oil" count 1

        10 weight SharedDropTables.herb
        1 weight SharedDropTables.gem
        10 weight nothing()
    },
    tertiaries = rsPlayerTertiaryTable {
        // Drops Need Manual (rate): The easy clue scroll drop rate increases to 1/121 after unlocking the easy Combat Achievements rewards tier.
        1 outOf 128 weight "obj.trail_clue_easy_simple001" count 1 transformObj { player ->
            // Drops Need Manual (item): Clue scrolls will drop as scroll boxes after the completion of X Marks the Spot.
             null
        }
    },
)
