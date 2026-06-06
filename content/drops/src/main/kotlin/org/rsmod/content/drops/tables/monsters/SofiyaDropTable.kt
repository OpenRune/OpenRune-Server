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
public val sofiyaDropTable: RSDropTable<Player, DropRollItem> = RSDropTable(
    tableIdentifier = "Sofiya Drops",
    npcs = npcs("npc.canafis_woman6"),
    mainTable = rsPlayerWeightedTable(total = 512) {
        name("Sofiya Drops")
        10 weight "obj.steel_axe" count 1
        10 weight "obj.steel_arrow" count 50
        10 weight "obj.steel_arrow_p" count 5
        2 weight "obj.eye_of_newt" count 1
        2 weight "obj.snape_grass" count 1
        2 weight "obj.jangerberries" count 1
        2 weight "obj.white_berries" count 1
        2 weight "obj.limpwurt_root" count 1
        2 weight "obj.wine_of_zamorak" count 1
        10 weight "obj.iron_ore" count 5
        10 weight "obj.coal" count 2
        10 weight "obj.steel_bar" count 2
        10 weight "obj.raw_chicken" count 2
        10 weight "obj.raw_beef" count 2
        10 weight "obj.raw_bear_meat" count 2
        50 weight "obj.vial_water" count 2
        20 weight "obj.coins" count 6
        5 weight "obj.coins" count 15
        5 weight "obj.coins" count 16
        10 weight "obj.coins" count 20
        5 weight "obj.coins" count 38
        5 weight "obj.coins" count 50
        5 weight "obj.coins" count 96
        5 weight "obj.coins" count 120
        25 weight "obj.bucket_empty" count 2
        20 weight "obj.tinderbox" count 2
        50 weight "obj.vial_empty" count 2
        100 weight "obj.werewolve_fur" count 1
        100 weight "obj.grey_wolf_fur" count 1
        1 weight "obj.blamish_oil" count 1

        2 weight SharedDropTables.herb
        1 weight SharedDropTables.gem
        1 weight nothing()
    },
    tertiaries = rsPlayerTertiaryTable {
        // Drops Need Manual (rate): The easy clue scroll drop rate increases to 1/121 after unlocking the easy Combat Achievements rewards tier.
        1 outOf 128 weight "obj.trail_clue_easy_simple001" count 1 transformObj { player ->
            // Drops Need Manual (item): Clue scrolls will drop as scroll boxes after the completion of X Marks the Spot.
             null
        }
    },
)
