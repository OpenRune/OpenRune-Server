package org.rsmod.content.drops.tables.thieving

import dtx.rs.RSDropTable
import dtx.rs.pickpockets
import org.rsmod.api.droptable.DropRollItem
import org.rsmod.api.droptable.RegisterDropTable
import org.rsmod.api.droptable.rsPlayerGuaranteedTable
import org.rsmod.game.entity.Player

@field:RegisterDropTable
@JvmField
public val watchmanPickpocketDropTable: RSDropTable<Player, DropRollItem> = RSDropTable(
    tableIdentifier = "Watchman (Pickpocket)",
    pickpockets = pickpockets("watchman"),
    guaranteed = rsPlayerGuaranteedTable {
        "obj.pickpocket_coin_pouch_watchman" count 1
        "obj.bread" count 1
    },
)
