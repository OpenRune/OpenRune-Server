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
public val possessedPriestDropTable: RSDropTable<Player, DropRollItem> = RSDropTable(
    tableIdentifier = "Possessed Priest Drops",
    npcs = npcs("npc.ics_little_possessedpriest"),
    guaranteed = rsPlayerGuaranteedTable {
        "obj.4dose1defense" count 1
        "obj.4dose1attack" count 1
        "obj.4dose1agility" count 1
        "obj.4dose1magic" count 1
    },
)

// Unknown wiki drop rates (text rarity — need data collection):
//   - Coins [main/Uncommon]
