package org.rsmod.content.drops.tables.thieving

import dtx.rs.RSDropTable
import dtx.rs.locs
import org.rsmod.api.droptable.DropRollItem
import org.rsmod.api.droptable.RegisterDropTable
import org.rsmod.api.droptable.rsPlayerWeightedTable
import org.rsmod.game.entity.Player

@field:RegisterDropTable
@JvmField
public val fruitStallDropTable: RSDropTable<Player, DropRollItem> = RSDropTable(
    tableIdentifier = "Fruit Stall (Thieving)",
    locs = locs("loc.hos_fruit_stall_02"),
    mainTable = rsPlayerWeightedTable(total = 100) {
        name("Fruit Stall (Thieving)")
        40 weight "obj.cooking_apple" count 1
        20 weight "obj.banana" count 1
        7 weight "obj.strawberry" count 1
        5 weight "obj.jangerberries" count 1
        5 weight "obj.lemon" count 1
        5 weight "obj.redberries" count 1
        5 weight "obj.pineapple" count 1
        5 weight "obj.lime" count 1
        5 weight "obj.macro_triffidfruit" count 1
        2 weight "obj.golovanova_top" count 1
        1 weight "obj.papaya" count 1
    },
)
