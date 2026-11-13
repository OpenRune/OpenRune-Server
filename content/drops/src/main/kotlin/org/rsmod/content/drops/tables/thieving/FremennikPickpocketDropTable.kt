package org.rsmod.content.drops.tables.thieving

import dtx.rs.RSDropTable
import dtx.rs.pickpockets
import org.rsmod.api.droptable.DropRollItem
import org.rsmod.api.droptable.RegisterDropTable
import org.rsmod.api.droptable.rsPlayerGuaranteedTable
import org.rsmod.game.entity.Player

@field:RegisterDropTable
@JvmField
public val fremennikPickpocketDropTable: RSDropTable<Player, DropRollItem> = RSDropTable(
    tableIdentifier = "Fremennik Citizen (Pickpocket)",
    pickpockets = pickpockets("fremennik_citizen"),
    guaranteed = rsPlayerGuaranteedTable {
        "obj.pickpocket_coin_pouch_fremennik" count 1
    },
)
