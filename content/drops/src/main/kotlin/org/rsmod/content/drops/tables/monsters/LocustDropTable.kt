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
public val locustDropTable: RSDropTable<Player, DropRollItem> = RSDropTable(
    tableIdentifier = "Locust Drops",
    npcs = npcs("npc.ics_little_locust_vis", "npc.ullek_locust"),
    guaranteed = rsPlayerGuaranteedTable {
        "obj.locust_meat" count 1
    },
)
