package org.rsmod.content.drops.tables.thieving

import dtx.rs.RSDropTable
import dtx.rs.pickpockets
import org.rsmod.api.droptable.DropRollItem
import org.rsmod.api.droptable.RegisterDropTable
import org.rsmod.api.droptable.rsPlayerWeightedTable
import org.rsmod.game.entity.Player

@field:RegisterDropTable
@JvmField
public val tzhaarHurPickpocketDropTable: RSDropTable<Player, DropRollItem> = RSDropTable(
    tableIdentifier = "TzHaar-Hur (Pickpocket)",
    pickpockets = pickpockets("tzhaar_hur"),
    mainTable = rsPlayerWeightedTable(total = 195) {
        name("TzHaar-Hur (Pickpocket)")
        182 weight "obj.tzhaar_token" count 3..7
        5 weight "obj.uncut_sapphire" count 1
        4 weight "obj.uncut_emerald" count 1
        3 weight "obj.uncut_ruby" count 1
        1 weight "obj.uncut_diamond" count 1
    },
)
