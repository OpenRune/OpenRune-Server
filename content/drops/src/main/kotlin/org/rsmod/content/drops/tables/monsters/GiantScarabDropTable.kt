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
public val giantScarabDropTable: RSDropTable<Player, DropRollItem> = RSDropTable(
    tableIdentifier = "Giant Scarab Drops",
    npcs = npcs("npc.contact_dummy_boss", "npc.contact_scarab_boss"),
    guaranteed = rsPlayerGuaranteedTable {
        "obj.contact_keris" count 1
    },
)
