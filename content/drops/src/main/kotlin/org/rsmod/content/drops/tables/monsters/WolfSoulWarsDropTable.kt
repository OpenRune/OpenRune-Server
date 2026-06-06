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
public val wolfSoulWarsDropTable: RSDropTable<Player, DropRollItem> = RSDropTable(
    tableIdentifier = "Wolf (Soul Wars) Drops",
    npcs = npcs("npc.soul_wars_tut_wolf", "npc.soul_wars_wolf"),
    guaranteed = rsPlayerGuaranteedTable {
        "obj.soul_wars_bones" count 1
    },
)
