package org.rsmod.content.drops.tables.thieving

import dtx.rs.RSDropTable
import dtx.rs.pickpockets
import org.rsmod.api.droptable.DropRollItem
import org.rsmod.api.droptable.RegisterDropTable
import org.rsmod.api.droptable.rsPlayerWeightedTable
import org.rsmod.game.entity.Player

@field:RegisterDropTable
@JvmField
public val roguePickpocketDropTable: RSDropTable<Player, DropRollItem> = RSDropTable(
    tableIdentifier = "Rogue (Pickpocket)",
    pickpockets = pickpockets("rogue"),
    mainTable = rsPlayerWeightedTable(total = 144) {
        name("Rogue (Pickpocket)")
        123 weight "obj.pickpocket_coin_pouch_rogue" count 1
        9 weight "obj.airrune" count 8
        6 weight "obj.jug_wine" count 1
        5 weight "obj.lockpick" count 1
        1 weight "obj.iron_dagger_p" count 1
    },
)
