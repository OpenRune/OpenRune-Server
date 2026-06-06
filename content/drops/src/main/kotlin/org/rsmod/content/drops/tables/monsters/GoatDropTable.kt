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
public val goatDropTable: RSDropTable<Player, DropRollItem> = RSDropTable(
    tableIdentifier = "Goat Drops",
    npcs = npcs("npc.desert_goat_black1", "npc.desert_goat_black2", "npc.desert_goat_white1", "npc.desert_goat_white2"),
    guaranteed = rsPlayerGuaranteedTable {
        "obj.desert_goat_horn" count 1
    },
)
