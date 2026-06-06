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
public val molaniskDropTable: RSDropTable<Player, DropRollItem> = RSDropTable(
    tableIdentifier = "Molanisk Drops",
    npcs = npcs("npc.molanisk"),
    mainTable = rsPlayerWeightedTable(total = 128) {
        name("Molanisk Drops")
        10 weight "obj.waterrune" count 1..14
        10 weight "obj.earthrune" count 1..20
        3 weight "obj.naturerune" count 1..5
        5 weight "obj.cosmicrune" count 1..7
        2 weight "obj.mudrune" count 1..15
        37 weight "obj.dorgesh_swamp_weed" count 1..4
        10 weight "obj.dorgesh_swamp_weed" count 5..8
        10 weight "obj.coins" count 1..75
        1 weight "obj.mole_claw" count 1

        32 weight SharedDropTables.herb
        7 weight SharedDropTables.gem
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
