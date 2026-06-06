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
public val evilChickenDropTable: RSDropTable<Player, DropRollItem> = RSDropTable(
    tableIdentifier = "Evil Chicken Drops",
    npcs = npcs("npc.evil_chicken"),
    guaranteed = rsPlayerGuaranteedTable {
        "obj.raw_chicken" count 1
        "obj.feather" count 3..242
    },
)
