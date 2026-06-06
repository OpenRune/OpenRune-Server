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
public val monkeyArcherDropTable: RSDropTable<Player, DropRollItem> = RSDropTable(
    tableIdentifier = "Monkey Archer Drops",
    npcs = npcs("npc.mm2_monkey_archer", "npc.mm_monkey_archer", "npc.mm_posted_archer", "npc.mm_ravine_archer"),
    tertiaries = rsPlayerTertiaryTable {
        1 outOf 35 weight "obj.arceuus_corpse_monkey" count 1
    },
)
