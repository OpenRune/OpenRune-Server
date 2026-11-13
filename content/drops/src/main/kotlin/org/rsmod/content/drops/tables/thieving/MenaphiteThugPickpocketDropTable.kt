package org.rsmod.content.drops.tables.thieving

import dtx.rs.RSDropTable
import dtx.rs.pickpockets
import org.rsmod.api.droptable.DropRollItem
import org.rsmod.api.droptable.RegisterDropTable
import org.rsmod.api.droptable.rsPlayerGuaranteedTable
import org.rsmod.game.entity.Player

@field:RegisterDropTable
@JvmField
public val menaphiteThugPickpocketDropTable: RSDropTable<Player, DropRollItem> = RSDropTable(
    tableIdentifier = "Menaphite Thug (Pickpocket)",
    pickpockets = pickpockets("menaphite_thug"),
    guaranteed = rsPlayerGuaranteedTable {
        "obj.pickpocket_coin_pouch_menaphite" count 1
    },
)
