package org.rsmod.content.drops.tables.monsters

import dtx.rs.RSDropTable
import dtx.rs.npcs
import dtx.rs.areas
import org.rsmod.api.droptable.rsPlayerGuaranteedTable
import org.rsmod.api.droptable.rsPlayerTertiaryTable
import org.rsmod.api.droptable.rsPlayerWeightedTable
import org.rsmod.api.droptable.DropRollItem
import org.rsmod.api.droptable.RegisterDropTable
import org.rsmod.game.entity.Player

@field:RegisterDropTable
@JvmField
public val jonnyTheBeardDropTable: RSDropTable<Player, DropRollItem> = RSDropTable(
    tableIdentifier = "Jonny the Beard Drops",
    npcs = npcs("npc.jonny_the_beard", "npc.jonny_the_beard_1op", "npc.jonny_the_beard_2op"),
    tertiaries = rsPlayerTertiaryTable {
        1 outOf 1 weight "obj.intelligence_report" count 1
    },
)
