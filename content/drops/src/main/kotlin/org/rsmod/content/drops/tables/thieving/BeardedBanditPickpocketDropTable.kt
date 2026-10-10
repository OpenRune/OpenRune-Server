package org.rsmod.content.drops.tables.thieving

import dtx.rs.RSDropTable
import dtx.rs.pickpockets
import org.rsmod.api.droptable.DropRollItem
import org.rsmod.api.droptable.RegisterDropTable
import org.rsmod.api.droptable.rsPlayerGuaranteedTable
import org.rsmod.game.entity.Player

@field:RegisterDropTable
@JvmField
public val beardedBanditPickpocketDropTable: RSDropTable<Player, DropRollItem> = RSDropTable(
    tableIdentifier = "Bearded Pollnivnian Bandit (Pickpocket)",
    pickpockets = pickpockets("bearded_bandit"),
    guaranteed = rsPlayerGuaranteedTable {
        "obj.pickpocket_coin_pouch_bandit2" count 1
    },
)
