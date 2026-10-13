package org.rsmod.content.drops.tables.thieving

import dtx.rs.RSDropTable
import dtx.rs.pickpockets
import org.rsmod.api.droptable.DropRollItem
import org.rsmod.api.droptable.RegisterDropTable
import org.rsmod.api.droptable.rsPlayerWeightedTable
import org.rsmod.game.entity.Player

@field:RegisterDropTable
@JvmField
public val farmerPickpocketDropTable: RSDropTable<Player, DropRollItem> = RSDropTable(
    tableIdentifier = "Farmer (Pickpocket)",
    pickpockets = pickpockets("farmer"),
    mainTable = rsPlayerWeightedTable(total = 128) {
        name("Farmer (Pickpocket)")
        123 weight "obj.pickpocket_coin_pouch_farmer" count 1
        5 weight "obj.potato_seed" count 1
    },
)
