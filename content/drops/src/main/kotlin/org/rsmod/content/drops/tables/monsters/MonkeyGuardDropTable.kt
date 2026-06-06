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
public val monkeyGuardDropTable: RSDropTable<Player, DropRollItem> = RSDropTable(
    tableIdentifier = "Monkey Guard Drops",
    npcs = npcs("npc.mm_religious_guard", "npc.mm_religious_trapdoor_guard"),
    tertiaries = rsPlayerTertiaryTable {
        1 outOf 35 weight "obj.arceuus_corpse_monkey" count 1
    },
)
