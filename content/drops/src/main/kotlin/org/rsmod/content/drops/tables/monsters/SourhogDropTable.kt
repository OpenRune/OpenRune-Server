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
public val sourhogDropTable: RSDropTable<Player, DropRollItem> = RSDropTable(
    tableIdentifier = "Sourhog Drops",
    npcs = npcs("npc.sourhog"),
    mainTable = rsPlayerWeightedTable(total = 110) {
        name("Sourhog Drops")
        2 weight "obj.adamant_scimitar" count 1
        1 weight "obj.adamant_kiteshield" count 1
        6 weight "obj.mindrune" count 3..10
        6 weight "obj.airrune" count 5..15
        6 weight "obj.earthrune" count 5..15
        4 weight "obj.naturerune" count 1..5
        5 weight "obj.chaosrune" count 1..5
        5 weight "obj.steel_arrow" count 3..7
        15 weight "obj.coins" count 5..30
        5 weight "obj.limpwurt_root" count 1
        5 weight "obj.cabbage" count 1
        5 weight "obj.potato" count 1
        5 weight "obj.cooking_apple" count 1

        10 weight SharedDropTables.herb
        1 weight SharedDropTables.rareDrop
        5 weight SharedDropTables.gem
        24 weight nothing()
    },
    tertiaries = rsPlayerTertiaryTable {
        1 outOf 400 weight "obj.dorgesh_construction_bone" count 1
        1 outOf 5013 weight "obj.dorgesh_construction_bone_curved" count 1
        // Drops Need Manual (rate): The easy clue scroll drop rate increases to 1/121 after unlocking the easy Combat Achievements rewards tier.
        1 outOf 128 weight "obj.trail_clue_easy_simple001" count 1 transformObj { player ->
            // Drops Need Manual (item): Clue scrolls will drop as scroll boxes after the completion of X Marks the Spot.
             null
        }
    },
)
