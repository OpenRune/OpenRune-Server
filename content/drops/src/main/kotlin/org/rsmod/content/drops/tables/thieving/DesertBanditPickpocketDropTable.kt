package org.rsmod.content.drops.tables.thieving

import dtx.rs.RSDropTable
import dtx.rs.pickpockets
import org.rsmod.api.droptable.DropRollItem
import org.rsmod.api.droptable.RegisterDropTable
import org.rsmod.api.droptable.rsPlayerWeightedTable
import org.rsmod.game.entity.Player

@field:RegisterDropTable
@JvmField
public val desertBanditPickpocketDropTable: RSDropTable<Player, DropRollItem> = RSDropTable(
    tableIdentifier = "Desert Bandit (Pickpocket)",
    pickpockets = pickpockets("desert_bandit"),
    mainTable = rsPlayerWeightedTable(total = 7) {
        name("Desert Bandit (Pickpocket)")
        5 weight "obj.pickpocket_coin_pouch_desertbandit" count 1
        1 weight "obj.1doseantipoison" count 1
        1 weight "obj.lockpick" count 1
    },
)
