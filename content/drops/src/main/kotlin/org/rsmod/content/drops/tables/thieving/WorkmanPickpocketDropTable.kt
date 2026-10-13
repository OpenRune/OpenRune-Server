package org.rsmod.content.drops.tables.thieving

import dtx.rs.RSDropTable
import dtx.rs.pickpockets
import org.rsmod.api.droptable.DropRollItem
import org.rsmod.api.droptable.RegisterDropTable
import org.rsmod.api.droptable.rsPlayerWeightedTable
import org.rsmod.game.entity.Player

@field:RegisterDropTable
@JvmField
public val workmanPickpocketDropTable: RSDropTable<Player, DropRollItem> = RSDropTable(
    tableIdentifier = "Workman (Pickpocket)",
    pickpockets = pickpockets("workman"),
    mainTable = rsPlayerWeightedTable(total = 11) {
        name("Workman (Pickpocket)")
        3 weight "obj.specimen_brush" count 1
        3 weight "obj.rock_sample1" count 1
        1 weight "obj.coins" count 10
        1 weight "obj.rope" count 1
        1 weight "obj.bucket_empty" count 1
        1 weight "obj.leather_gloves" count 1
        1 weight "obj.spade" count 1
    },
)
