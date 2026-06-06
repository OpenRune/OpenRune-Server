package org.rsmod.content.drops.tables.monsters

import dtx.rs.RSDropTable
import dtx.rs.npcs
import dtx.rs.areas
import org.rsmod.api.droptable.rsPlayerGuaranteedTable
import org.rsmod.api.droptable.rsPlayerTertiaryTable
import org.rsmod.api.droptable.rsPlayerWeightedTable
import org.rsmod.api.droptable.DropRollItem
import org.rsmod.api.droptable.ringNothing
import org.rsmod.api.droptable.RegisterDropTable
import org.rsmod.game.entity.Player

@field:RegisterDropTable
@JvmField
public val undeadChickenDropTable: RSDropTable<Player, DropRollItem> = RSDropTable(
    tableIdentifier = "Undead chicken Drops",
    npcs = npcs("npc.ahoy_undead_chicken"),
    guaranteed = rsPlayerGuaranteedTable {
        "obj.raw_chicken_undead" count 1
    },
    mainTable = rsPlayerWeightedTable(total = 128) {
        name("Undead chicken Drops")
        64 weight "obj.feather" count 5
        32 weight "obj.feather" count 15
        32 weight ringNothing()
    },
)
