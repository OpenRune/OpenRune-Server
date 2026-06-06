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
public val drinkTrollQueenDropTable: RSDropTable<Player, DropRollItem> = RSDropTable(
    tableIdentifier = "Drink troll queen Drops",
    npcs = npcs("npc.sailing_charting_drink_crate_monkfish_stout_effect"),
    guaranteed = rsPlayerGuaranteedTable {
        "obj.beer" count 1
    },
)
